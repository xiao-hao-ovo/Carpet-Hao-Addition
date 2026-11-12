package carpet_hao_addition.waterlogged;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerMode;
import fi.dy.masa.malilib.util.LayerRange;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * 「投影里该有水/岩浆/火,而实际还没有」那一格的判定与射线拾取(客户端)。
 * <p>
 * 水、岩浆、火都没有对应的可放置物品,原版和 Litematica 都不会把它们放下去,所以只能由本模组
 * 自己认格、发包、让服务端来放。
 * <p>
 * 认格分两层:
 * <ol>
 *   <li>{@link RayTraceUtils#getGenericTrace}:同时打「现实」和「投影」两条射线取更近的那条,
 *       所以现实里挡在中间的方块仍然会挡住它 —— 不会隔墙放水;</li>
 *   <li>命中格本身不是流体时,再看<b>命中面正对进去的那一格</b>(原版放水桶的语义:点面、水落到
 *       相邻格),这样点坑壁、坑底、活板门边沿也能把水放到凹槽里的那一格。</li>
 * </ol>
 * 只有在现实中这条视线上一格方块都没有(开阔地/水下)时,才补一条「只看投影」的射线,
 * 把被现实地形挡住的投影流体找回来;那种情况下本来也没有遮挡可穿,不存在隔墙放水。
 */
public final class FluidSourceTarget {
	/** 投影射线的最远距离(格),略大于玩家方块交互距离,便于命中投影里的流体。 */
	public static final double TRACE_RANGE = 6.0;

	private FluidSourceTarget() {
	}

	/**
	 * 玩家此刻正对着的、投影里该有而实际没有的流体格;没有则返回 {@code null}。
	 * <p>
	 * <b>只返回真正的流体目标</b>:调用方拿到非 null 就会走「放流体」分支,返回别的格会把
	 * 含水补水那条逻辑整个挤掉。
	 */
	public static BlockPos lookedAt(MinecraftClient minecraft, PlayerEntity player, WorldSchematic schematicWorld) {
		RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(minecraft.world, player, TRACE_RANGE);
		BlockPos fromGeneric = pick(player, schematicWorld, wrapper);
		if (fromGeneric != null) {
			return fromGeneric;
		}
		// 主射线被现实方块挡掉时,只有在现实中这条视线本来就一路无遮挡(打不到任何方块)的情况下,
		// 才允许用「只看投影」的射线补一次 —— 否则就成了隔着墙往那边放水。
		HitResult vanilla = RayTraceUtils.getRayTraceFromEntity(minecraft.world, player, true, TRACE_RANGE);
		if (vanilla != null && vanilla.getType() != HitResult.Type.MISS) {
			return null;
		}
		BlockHitResult schematicHit = RayTraceUtils.traceToSchematicWorld(player, TRACE_RANGE, true, true);
		if (schematicHit == null || schematicHit.getType() == HitResult.Type.MISS) {
			return null;
		}
		return pick(player, schematicWorld, schematicHit);
	}

	/** 上一格被认领的 x/z 水柱 → 认领时的客户端 tick。 */
	private static final java.util.Map<Long, Long> HAO_RECENT_COLUMNS = new java.util.HashMap<>();
	/** 同一 x/z 水柱的认领保护时长(客户端 tick)。 */
	private static final long HAO_COLUMN_TTL = 4L;
	/** 上一次真正发包所在的客户端 tick。 */
	private static long hao$lastSentTick = Long.MIN_VALUE;

	/**
	 * 认领一次「这一格该放流体」的上报权。
	 * <p>
	 * 现在有两条入口会认格:{@code MinecraftClient.doItemUse}(玩家右键)和 Litematica 的
	 * {@code WorldUtils.doEasyPlaceAction}(投影打印机自己每 tick 调一次)。两条入口各自都不知道对方
	 * 已经报过,拿到的又常常是同一条水柱的<b>上下两格</b>(灵魂沙/岩浆块上方的水在投影里是一整根
	 * 气泡柱,每一格都「该有水而实际没有」),于是点一下右键、上下两格同时被放出来。
	 * <p>
	 * 这里按 x/z 水柱加一个很短(4 tick)的互斥:同一条柱子在同一个手势里只上报一格,另一条入口
	 * 这次就放弃 —— 该补的格下一 tick 会重新判定,不会永久漏掉。
	 *
	 * @return {@code true} 表示这一格可以上报
	 */
	public static boolean claim(PlayerEntity player, BlockPos pos) {
		long now = player.getWorld().getTime();
		if (hao$lastSentTick == now) {
			return false;
		}
		long column = ((long) pos.getX() << 32) ^ (pos.getZ() & 0xFFFFFFFFL);
		Long last = HAO_RECENT_COLUMNS.get(column);
		if (last != null && now - last <= HAO_COLUMN_TTL) {
			return false;
		}
		hao$lastSentTick = now;
		HAO_RECENT_COLUMNS.put(column, now);
		if (HAO_RECENT_COLUMNS.size() > 256) {
			HAO_RECENT_COLUMNS.entrySet().removeIf(e -> now - e.getValue() > HAO_COLUMN_TTL);
		}
		return true;
	}
	/** 从一次射线命中里挑流体格:只认投影方块/现实方块命中,选择框、实体等一律不算。 */
	private static BlockPos pick(PlayerEntity player, WorldSchematic schematicWorld, RayTraceWrapper wrapper) {
		if (wrapper == null) {
			return null;
		}
		RayTraceWrapper.HitType type = wrapper.getHitType();
		if (type != RayTraceWrapper.HitType.SCHEMATIC_BLOCK && type != RayTraceWrapper.HitType.VANILLA_BLOCK) {
			return null;
		}
		BlockHitResult hit = wrapper.getBlockHitResult();
		return hit == null ? null : pick(player, schematicWorld, hit);
	}

	/** 从一次方块命中里挑流体格:先看命中格,再看命中面正对进去的相邻格。 */
	private static BlockPos pick(PlayerEntity player, WorldSchematic schematicWorld, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (missingKind(player, schematicWorld, pos) != WaterloggedFillPayload.KIND_NONE) {
			return pos;
		}
		if (hit.getSide() != null) {
			BlockPos adjacent = pos.offset(hit.getSide());
			if (missingKind(player, schematicWorld, adjacent) != WaterloggedFillPayload.KIND_NONE) {
				return adjacent;
			}
		}
		return null;
	}

	/**
	 * 该格是否落在 Litematica 当前的「渲染层」范围内。
	 * <p>
	 * 玩家把投影切成单层渲染时,只允许补那一层:否则灵魂沙上的气泡柱会顺着往上把
	 * 好几格"看不见"的水一起补掉 —— 连点几次就多出几格水,与画面上看到的层数对不上。
	 * 层范围读不到(没装/未初始化)时不拦截:宁可多放,也不要让右键忽然没反应。
	 */
	public static boolean withinRenderLayers(BlockPos pos) {
		try {
			LayerRange range = DataManager.getRenderLayerRange();
			if (range == null || range.getLayerMode() == LayerMode.ALL) {
				return true;
			}
			int value = switch (range.getAxis()) {
				case X -> pos.getX();
				case Y -> pos.getY();
				case Z -> pos.getZ();
			};
			return value >= range.getLayerMin() && value <= range.getLayerMax();
		} catch (Throwable ignored) {
			return true;
		}
	}

	/** 目标格投影里该放什么、而实际还没有 —— 返回请求类型;不需要处理时返回 {@code KIND_NONE}。 */
	public static int missingKind(PlayerEntity player, WorldSchematic schematicWorld, BlockPos pos) {
		// 只认玩家当前渲染层范围内的格:渲染了一格就只补那一层。
		if (!withinRenderLayers(pos)) {
			return WaterloggedFillPayload.KIND_NONE;
		}
		BlockState desired = schematicWorld.getBlockState(pos);
		FluidState fluid = player.getWorld().getFluidState(pos);
		// 流动的水/岩浆不算"已经有了":源格被流动流体覆盖时仍要放一格源把它补正。
		boolean fluidMissing = fluid.isEmpty() || !fluid.isStill();

		// 灵魂沙/岩浆块上方的水在投影里存成 bubble_column(气泡柱)方块,它不是 WATER,但同样该补。
		if (desired.isOf(Blocks.LAVA_CAULDRON)) {
			return player.getWorld().getBlockState(pos).isOf(Blocks.LAVA_CAULDRON)
					? WaterloggedFillPayload.KIND_NONE
					: WaterloggedFillPayload.KIND_LAVA_CAULDRON;
		}
		if ((desired.isOf(Blocks.WATER) && desired.getFluidState().isStill()) || desired.isOf(Blocks.BUBBLE_COLUMN)) {
			return fluidMissing ? WaterloggedFillPayload.KIND_WATER : WaterloggedFillPayload.KIND_NONE;
		}
		if (desired.isOf(Blocks.LAVA) && desired.getFluidState().isStill()) {
			return fluidMissing ? WaterloggedFillPayload.KIND_LAVA : WaterloggedFillPayload.KIND_NONE;
		}
		// 投影里是火 / 灵魂火(常见于灵魂沙、岩浆块、下界岩上方),或需要点出的下界传送门 → 点火。
		if (desired.isOf(Blocks.FIRE) || desired.isOf(Blocks.SOUL_FIRE) || desired.isOf(Blocks.NETHER_PORTAL)) {
			BlockState current = player.getWorld().getBlockState(pos);
			boolean lit = current.isOf(Blocks.FIRE) || current.isOf(Blocks.SOUL_FIRE)
					|| current.isOf(Blocks.NETHER_PORTAL);
			return lit ? WaterloggedFillPayload.KIND_NONE : WaterloggedFillPayload.KIND_IGNITE;
		}
		return WaterloggedFillPayload.KIND_NONE;
	}

	/** 请求类型需要消耗的材料是否充足:水→冰,岩浆→岩浆块,装岩浆的炼药锅→炼药锅+岩浆块。 */
	public static boolean hasMaterial(PlayerEntity player, int kind) {
		return switch (kind) {
			case WaterloggedFillPayload.KIND_WATER -> hasItem(player, Items.ICE);
			case WaterloggedFillPayload.KIND_LAVA -> hasItem(player, Items.MAGMA_BLOCK);
			// 炼药锅本身由正常放置流程消耗(见 WaterloggedFillHandler),这里只要求岩浆块。
			case WaterloggedFillPayload.KIND_LAVA_CAULDRON -> hasItem(player, Items.MAGMA_BLOCK);
			// 点火:火焰弹(消耗 1 个)或打火石(消耗 1 点耐久)任一即可。
			case WaterloggedFillPayload.KIND_IGNITE ->
					hasItem(player, Items.FIRE_CHARGE) || hasItem(player, Items.FLINT_AND_STEEL);
			default -> false;
		};
	}

	/** 材料对应的名字,用于提示玩家缺什么。 */
	public static String materialName(int kind) {
		return switch (kind) {
			case WaterloggedFillPayload.KIND_WATER -> "冰块";
			case WaterloggedFillPayload.KIND_LAVA, WaterloggedFillPayload.KIND_LAVA_CAULDRON -> "岩浆块";
			case WaterloggedFillPayload.KIND_IGNITE -> "火焰弹或打火石";
			default -> "材料";
		};
	}

	private static boolean hasItem(PlayerEntity player, Item item) {
		return ClientWaterlogNotifier.hasItemInInventory(player, item);
	}
}

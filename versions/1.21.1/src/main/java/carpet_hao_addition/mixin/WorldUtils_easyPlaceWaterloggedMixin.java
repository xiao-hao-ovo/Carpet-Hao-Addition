package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.PlaceWaterloggedHandler;
import carpet_hao_addition.PlaceWaterloggedPayload;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * easyPlaceWaterlogged(客户端):告诉服务端「正在轻松放置」以及「哪一格该补什么」。
 * <p>
 * 三类东西没有对应的可放置物品,原版与 Litematica 都不会放下,所以由客户端把它们报给服务端:
 * 水源(含气泡柱)消耗冰、源岩浆消耗岩浆块、装岩浆的炼药锅按正常消耗炼药锅物品;材料不足就什么都不做。
 * 另外把「投影里含水、而实际还没水」的格(中心 ±1 范围内整批)报给服务端,由服务端在其放上方块后补含水。
 * <p>
 * 钩子选 {@code WorldUtils.doEasyPlaceAction}(轻松放置总入口,每次操作都会调用)。
 * <b>不能挂在 {@code easyPlaceBlockChecksCancel} 上</b> —— 投影里是水(必需建造物品为空)或
 * 投影与现状相同时,Litematica 会提前 return,那个判定根本不会被调用,灵魂沙上方的水就永远补不上。
 */
@Mixin(WorldUtils.class)
public abstract class WorldUtils_easyPlaceWaterloggedMixin {
	/** 上次发包所在的客户端 tick,用于每 tick 限一次。 */
	@Unique
	private static long hao$lastSentTick = Long.MIN_VALUE;
	/**
	 * 最近请求过的位置 → 请求时的 tick。
	 * <p>
	 * 用来避免"同一格被重复请求":流体的更新要下一 tick 才生效,而相邻源之间可能被挡水方块隔开
	 * (流体不会流过去),于是相邻的另一个源格仍然"看起来没补上",会被再请求一次。
	 */
	@Unique
	private static final java.util.Map<BlockPos, Long> hao$recentWaterRequests = new java.util.HashMap<>();
	/** 位置请求后的保护时长(客户端 tick)。 */
	@Unique
	private static final long HAO_REQUEST_TTL = 40L;
	/** 投影射线的最远距离(格),略大于玩家方块交互距离,便于命中投影里的流体。 */
	@Unique
	private static final double HAO_TRACE_RANGE = 6.0;

	@Inject(method = "doEasyPlaceAction", at = @At("HEAD"), cancellable = true)
	private static void hao$waterlogOnEasyPlace(MinecraftClient minecraft, CallbackInfoReturnable<ActionResult> cir) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		PlayerEntity player = minecraft.player;
		if (player == null) {
			return;
		}
		long tick = player.getWorld().getTime();
		if (hao$lastSentTick == tick) {
			return;
		}
		WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
		if (schematicWorld == null) {
			return;
		}

		// 1) 投影里是水/岩浆/装岩浆的炼药锅:它们都没有可放置物品,Litematica 不会放下它们。
		BlockPos target = hao$fluidTarget(minecraft, player);
		if (target != null) {
			int kind = hao$missingKind(player, schematicWorld, target);
			if (kind != PlaceWaterloggedPayload.KIND_NONE && hao$hasMaterial(player, kind)
					&& hao$markRequested(player, target)) {
				hao$lastSentTick = tick;
				PlaceWaterloggedHandler.registerPayloadType();
				ClientPlayNetworking.send(new PlaceWaterloggedPayload(true, kind, List.of(target)));
				if (kind != PlaceWaterloggedPayload.KIND_LAVA_CAULDRON) {
					// 投影里这一格是水/岩浆,它们没有对应的可放置物品:Litematica 会走内部
					// "必需建造物品为空"那条分支,直接用手里的东西右键一次。拦下这次操作,改由服务端放。
					cir.setReturnValue(ActionResult.SUCCESS);
				}
				// 炼药锅不拦:让 Litematica 正常用一个炼药锅物品把 cauldron 放下,服务端随后灌岩浆。
				return;
			}
		}

		// 2) 含水方块补水:在中心附近小范围里,把"投影里含水、实际还没水"的格**整批**报给服务端。
		//    不去赌某一格"完全对上" —— 服务端会在这些格真的放上方块后补水,避免偶发漏补。
		BlockPos waterlogCenter = target != null ? target : hao$litematicaTarget(minecraft, player);
		List<BlockPos> waterlogTargets = hao$collectWaterlogTargets(player, schematicWorld, waterlogCenter);
		// 每次操作都发一包:没有要补的格时也报个空包,服务端据此刷新"正在施工"状态并清理记录。
		hao$lastSentTick = tick;
		PlaceWaterloggedHandler.registerPayloadType();
		ClientPlayNetworking.send(new PlaceWaterloggedPayload(true,
				waterlogTargets.isEmpty() ? PlaceWaterloggedPayload.KIND_NONE : PlaceWaterloggedPayload.KIND_WATERLOG,
				waterlogTargets));
	}

	/** 目标格投影里该放什么、而实际还没有 —— 返回请求类型;不需要处理时返回 {@code KIND_NONE}。 */
	@Unique
	private static int hao$missingKind(PlayerEntity player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		FluidState fluid = player.getWorld().getFluidState(pos);
		// 流动的水/岩浆不算"已经有了":源格被流动流体覆盖时仍要放一格源把它补正。
		boolean fluidMissing = fluid.isEmpty() || !fluid.isStill();

		// 灵魂沙/岩浆块上方的水在投影里存成 bubble_column(气泡柱)方块,它不是 WATER,但同样该补。
		if (desired.isOf(Blocks.LAVA_CAULDRON)) {
			return player.getWorld().getBlockState(pos).isOf(Blocks.LAVA_CAULDRON)
					? PlaceWaterloggedPayload.KIND_NONE
					: PlaceWaterloggedPayload.KIND_LAVA_CAULDRON;
		}
		if ((desired.isOf(Blocks.WATER) && desired.getFluidState().isStill()) || desired.isOf(Blocks.BUBBLE_COLUMN)) {
			return fluidMissing ? PlaceWaterloggedPayload.KIND_WATER : PlaceWaterloggedPayload.KIND_NONE;
		}
		if (desired.isOf(Blocks.LAVA) && desired.getFluidState().isStill()) {
			return fluidMissing ? PlaceWaterloggedPayload.KIND_LAVA : PlaceWaterloggedPayload.KIND_NONE;
		}
		return PlaceWaterloggedPayload.KIND_NONE;
	}

	/** 请求类型需要消耗的材料是否充足:水→冰,岩浆→岩浆块,装岩浆的炼药锅→炼药锅+岩浆块。 */
	@Unique
	private static boolean hao$hasMaterial(PlayerEntity player, int kind) {
		return switch (kind) {
			case PlaceWaterloggedPayload.KIND_WATER -> hao$hasItem(player, Items.ICE);
			case PlaceWaterloggedPayload.KIND_LAVA -> hao$hasItem(player, Items.MAGMA_BLOCK);
			// 炼药锅本身由正常放置流程消耗(见 PlaceWaterloggedHandler),这里只要求岩浆块。
			case PlaceWaterloggedPayload.KIND_LAVA_CAULDRON -> hao$hasItem(player, Items.MAGMA_BLOCK);
			default -> false;
		};
	}

	/** 同一格短时间内只请求一次(流体的更新要下一 tick 才生效,否则会被重复放)。 */
	@Unique
	private static boolean hao$markRequested(PlayerEntity player, BlockPos pos) {
		long now = player.getWorld().getTime();
		Long last = hao$recentWaterRequests.get(pos);
		if (last != null && now - last <= HAO_REQUEST_TTL) {
			return false;
		}
		hao$recentWaterRequests.put(pos, now);
		hao$pruneWaterRequests(now);
		return true;
	}

	/**
	 * 用<b>含流体</b>的最近投影命中取目标格({@code getGenericTrace} 的 3 参重载,内部即
	 * {@code respectRenderRange=true, targetFluids=true, false})。
	 * <p>
	 * Litematica 本部在关掉"目标流体"时的射线看不见流体,所以这条路径必须绕开它的设置,
	 * 否则灵魂沙上方的水/岩浆永远放不上。不是投影方块时返回 null。
	 */
	@Unique
	private static BlockPos hao$fluidTarget(MinecraftClient minecraft, PlayerEntity player) {
		RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(minecraft.world, player, HAO_TRACE_RANGE);
		if (wrapper == null || wrapper.getHitType() != RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
			return null;
		}
		BlockHitResult hit = wrapper.getBlockHitResult();
		return hit == null ? null : hit.getBlockPos();
	}

	/**
	 * 复刻 Litematica 本部 {@code doEasyPlaceAction} 的目标格选择({@code Configs.Generic.EASY_PLACE_FIRST}
	 * 决定用哪条射线,含它的"目标流体"设置)。
	 * <p>
	 * 只在"含流体射线没命中投影方块"时,作为补水扫描的中心兜底使用 —— <b>不能</b>拿它判定"要放什么",
	 * 因为它跟着 Litematica 的"目标流体"开关走,那个开关关掉时它看不见水/气泡柱。
	 */
	@Unique
	private static BlockPos hao$litematicaTarget(MinecraftClient minecraft, PlayerEntity player) {
		RayTraceWrapper wrapper;
		if (Configs.Generic.EASY_PLACE_FIRST.getBooleanValue()) {
			wrapper = RayTraceUtils.getGenericTrace(minecraft.world, player, HAO_TRACE_RANGE, true,
					Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue(), false);
		} else {
			wrapper = RayTraceUtils.getFurthestSchematicWorldTraceBeforeVanilla(
					minecraft.world, player, HAO_TRACE_RANGE);
		}
		if (wrapper == null || wrapper.getHitType() != RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
			return null;
		}
		BlockHitResult hit = wrapper.getBlockHitResult();
		return hit == null ? null : hit.getBlockPos();
	}

	/** 该格在投影里是否「含水、而实际还没水」—— 是则会被上报给服务端,在其放上方块后补含水并扣冰。 */
	@Unique
	private static boolean hao$needsWaterlog(PlayerEntity player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		if (!desired.contains(Properties.WATERLOGGED) || !Boolean.TRUE.equals(desired.get(Properties.WATERLOGGED))) {
			return false;
		}
		BlockState current = player.getWorld().getBlockState(pos);
		return !current.contains(Properties.WATERLOGGED) || !Boolean.TRUE.equals(current.get(Properties.WATERLOGGED));
	}

	/**
	 * 在中心附近 ±1 格内收集"投影里含水、而实际还没水"的格(需要有冰)。
	 * <p>
	 * 不要求玩家准星格算得完全准:Litematica 目标格与实际放置位置可能差一格,整批上报后由服务端在
	 * 这些格真的放上方块时补水,偶发的位置偏差就不会漏。
	 */
	@Unique
	private static List<BlockPos> hao$collectWaterlogTargets(PlayerEntity player, WorldSchematic schematicWorld,
			BlockPos center) {
		List<BlockPos> result = new ArrayList<>();
		if (center == null || !hao$hasItem(player, Items.ICE)) {
			return result;
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos pos = center.add(dx, dy, dz);
					if (hao$needsWaterlog(player, schematicWorld, pos)) {
						result.add(pos);
					}
				}
			}
		}
		return result;
	}

	/** 清理过期的请求记录。 */
	@Unique
	private static void hao$pruneWaterRequests(long now) {
		hao$recentWaterRequests.entrySet().removeIf(e -> now - e.getValue() > HAO_REQUEST_TTL);
	}

	@Unique
	private static boolean hao$hasItem(PlayerEntity player, Item item) {
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (!stack.isEmpty() && stack.isOf(item)) {
				return true;
			}
		}
		return false;
	}
}

package carpet_hao_addition.waterlogged;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.HaoDebug;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * haoProjectionWaterlogged 的补水/补料上报(客户端)。
 * <p>
 * 补水原来只挂在 {@code WorldUtils.doEasyPlaceAction} 上,但那个方法是 Litematica 的内部私有实现,
 * 各移植版会换掉它的调用链(0.23.7 的移植版把 {@code EasyPlaceUtils.shouldDoEasyPlaceActions()}
 * 写成了恒 {@code false},{@code doEasyPlaceAction} 在某些操作里根本不会被调用)。
 * 于是「先用手放一个干的铁活板门、再蹲下右键补水」这种操作一个包都发不出去,表现就是「怎么点都不出水」。
 * <p>
 * 所以这里另挂一条 {@code MinecraftClient.doItemUse}(玩家每次右键都必然经过它),但**前提条件**是
 * 投影(Litematica)的轻松放置必须开着:关掉轻松放置就完全不认格、不发包,右键也不会冒出流体 ——
 * 这个规则的名字就是「投影轻松放置补料」,不能脱离轻松放置自己干活。
 */
public final class ClientWaterlogNotifier {
	/** 上次发包所在的客户端 tick:每 tick 只上报一次,避免连点刷包。 */
	private static long hao$lastSentTick = Long.MIN_VALUE;
	/** 上次提示「缺冰」所在的 tick:提示不要每次点击都刷。 */
	private static long hao$lastHintTick = Long.MIN_VALUE;
	/** 缺料提示的最小间隔(客户端 tick)。 */
	private static final long HAO_HINT_INTERVAL = 20L;

	private ClientWaterlogNotifier() {
	}

	/**
	 * 前提条件:投影(Litematica)的轻松放置必须开着。
	 * <p>
	 * 关掉时本规则完全不工作:不认格、不发包、不拦右键。读不到配置(没装 Litematica / 版本对不上)
	 * 也按「没开」处理。
	 */
	public static boolean easyPlaceEnabled() {
		try {
			return Configs.Generic.EASY_PLACE_MODE.getBooleanValue();
		} catch (Throwable ignored) {
			return false;
		}
	}

	/**
	 * {@code MinecraftClient.doItemUse} 的 HEAD 钩子调用:按需上报待补水格。
	 *
	 * @return {@code true} 表示这次右键只该补水,调用方要拦下整次使用
	 *         (否则手上的方块会被一起放出去)
	 */
	public static boolean onUseItem(MinecraftClient minecraft) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return false;
		}
		if (!easyPlaceEnabled()) {
			return false; // 没开轻松放置:这条规则不参与任何右键
		}
		PlayerEntity player = minecraft.player;
		if (player == null) {
			return false;
		}
		// 触发条件:站立触发时要求未潜行,蹲下触发时要求正在潜行。
		if (!EasyPlaceWaterloggedSettings.triggerAllowed(player.isSneaking())) {
			return false;
		}
		WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
		if (schematicWorld == null) {
			return false;
		}
		// 0) 投影里是水/岩浆/火、实际还没有:这些东西没有对应的可放置物品,原版和投影都不会
		//    把它们放下去,只能由这里认格并发包。必须先判 —— 那一刻准星打在空气上,
		//    crosshairTarget 是 MISS,下面的 BlockHitResult 判定会直接把它漏掉。
		BlockPos sourceCell = FluidSourceTarget.lookedAt(minecraft, player, schematicWorld);
		if (sourceCell != null) {
			int kind = FluidSourceTarget.missingKind(player, schematicWorld, sourceCell);
			if (kind != WaterloggedFillPayload.KIND_NONE) {
				hao$requestFluidSource(player, schematicWorld, sourceCell, kind);
				// 这里**必须无条件拦下这次右键**,不能因为"这次没发包"(同 tick 已被另一条入口发过/
				// 材料不够/服务端没装)就放行:投影里这一格该是流体,原版放下一个方块之后就再也补不上水了
				// —— 服务端到点一看这格已经是固体方块,直接放弃。
				// 另一条入口(Litematica 的 doEasyPlaceAction)也不会替我们拦 doItemUse,于是就成了
				// "水没出来,方块倒是放出来了"。
				// 装岩浆的炼药锅是唯一例外:它本身就得先当作方块放下去,服务端只负责往里灌岩浆。
				return kind != WaterloggedFillPayload.KIND_LAVA_CAULDRON;
			}
		}
		HitResult hitResult = minecraft.crosshairTarget;
		if (!(hitResult instanceof BlockHitResult hit) || hitResult.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		List<BlockPos> targets = hao$collectTargets(player, schematicWorld, hit.getBlockPos());
		if (targets.isEmpty()) {
			return false;
		}
		long tick = player.getWorld().getTime();
		if (hao$lastSentTick == tick) {
			return false;
		}
		if (!hao$hasItem(player, Items.ICE)) {
			// 有该补的格却没有冰:一定要让玩家看见,否则就是「点了没反应」。
			if (tick - hao$lastHintTick >= HAO_HINT_INTERVAL) {
				hao$lastHintTick = tick;
				player.sendMessage(Text.literal("§e补水需要背包或潜影盒里有冰块"), true);
			}
			return false;
		}
		WaterloggedFillHandler.registerPayloadType();
		if (!ClientPlayNetworking.canSend(WaterloggedFillPayload.ID)) {
			// 服务端没装本模组(或版本对不上):发出去也没人收,静默放弃 —— 也不拦这次右键。
			return false;
		}
		hao$lastSentTick = tick;
		ClientPlayNetworking.send(new WaterloggedFillPayload(true,
				WaterloggedFillPayload.KIND_WATERLOG, targets));
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-waterlog] 右键上报补水 " + targets);
		}
		// 这一格已经放着投影里那个方块、只差含水 → 这次右键的唯一目的就是补水,
		// 拦下整次使用,别把手上的方块顺带放出去(用户说的「补水时多放一个方块」)。
		return hao$isPureRefill(player, schematicWorld, targets.get(0));
	}

	/**
	 * 上报一次「投影里这一格该有流体」的请求。
	 *
	 * @return {@code null} 表示这次没上报(同 tick 已发过 / 材料不够 / 服务端没装本模组),
	 *         调用方应继续走原来的放置流程;{@code true} 表示已上报且要拦下整次右键;
	 *         {@code false} 表示已上报但不要拦(装岩浆的炼药锅还得靠正常放置把锅放下去)
	 */
	private static Boolean hao$requestFluidSource(PlayerEntity player, WorldSchematic schematicWorld,
			BlockPos pos, int kind) {
		long tick = player.getWorld().getTime();
		if (hao$lastSentTick == tick) {
			return null;
		}
		if (!FluidSourceTarget.hasMaterial(player, kind)) {
			if (tick - hao$lastHintTick >= HAO_HINT_INTERVAL) {
				hao$lastHintTick = tick;
				player.sendMessage(Text.literal("§e需要背包或潜影盒里有" + FluidSourceTarget.materialName(kind)), true);
			}
			return null;
		}
		WaterloggedFillHandler.registerPayloadType();
		if (!ClientPlayNetworking.canSend(WaterloggedFillPayload.ID)) {
			return null;
		}
		// doItemUse 与 Litematica 的 doEasyPlaceAction 两条入口都会认格,同一条水柱会被认成上下两格:
		// 抢不到这一次上报权就放弃,别让一次右键冒出两格。
		if (!FluidSourceTarget.claim(player, pos)) {
			return null;
		}
		hao$lastSentTick = tick;
		ClientPlayNetworking.send(new WaterloggedFillPayload(true, kind, List.of(pos)));
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-fluid] 右键直接放 kind=" + kind + " " + pos);
		}
		// 炼药锅本身要先当作方块放下去(服务端只负责往里灌岩浆),拦了就没锅可灌。
		return kind != WaterloggedFillPayload.KIND_LAVA_CAULDRON;
	}

	/**
	 * 该格是否「已经放着投影里那个方块、只是没含水」。
	 * <p>
	 * 只有这种情况才对得上「纯补水」:方块还没放(空气/别的方块)时不能拦 —— 那一下仍然要靠
	 * 放置流程把方块放下,服务端随后再补水,拦了就会变成「右键毫无反应」。
	 */
	private static boolean hao$isPureRefill(PlayerEntity player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		BlockState current = player.getWorld().getBlockState(pos);
		return current.getBlock() == desired.getBlock() && hao$needsWaterlog(player, schematicWorld, pos);
	}

	/**
	 * 判断某个格是否为「纯补水」目标(方块已放好、只差含水、且确实有冰)。
	 * <p>
	 * 供更靠后的放置路径(如 {@code ClientPlayerInteractionManager.interactBlock})使用:
	 * 打印机(litematica-printer)在自己的 tick 里直接调 {@code interactBlock} 放置方块,不经过
	 * {@code MinecraftClient.doItemUse},只在 doItemUse 上 {@code ci.cancel()} 拦不住它,
	 * 于是用同一判定把「打印机这次交互」也一并拦下。
	 */
	public static boolean isPureRefillTarget(MinecraftClient minecraft, BlockPos pos) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return false;
		}
		if (!easyPlaceEnabled()) {
			return false; // 没开轻松放置:别去动玩家的手动放置
		}
		PlayerEntity player = minecraft.player;
		if (player == null) {
			return false;
		}
		if (!EasyPlaceWaterloggedSettings.triggerAllowed(player.isSneaking())) {
			return false;
		}
		WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
		if (schematicWorld == null) {
			return false;
		}
		if (!hao$hasItem(player, Items.ICE)) {
			return false;
		}
		return hao$isPureRefill(player, schematicWorld, pos);
	}

	/**
	 * 只检查准星正对的<b>那一格</b>:投影里含水、实际还没水才上报。
	 * <p>
	 * 之前用 ±1 容差找"最近的待补水格",导致明明没对准也会把旁边一格补了;
	 * 按用户要求改为严格按投影所对准的位置补水,不再带容差。
	 */
	private static List<BlockPos> hao$collectTargets(PlayerEntity player, WorldSchematic schematicWorld,
			BlockPos center) {
		List<BlockPos> result = new ArrayList<>();
		if (center != null && hao$needsWaterlog(player, schematicWorld, center)) {
			result.add(center);
		}
		return result;
	}

	/** 该格在投影里是否「含水、而实际还没水」,且落在玩家当前渲染的层里。 */
	private static boolean hao$needsWaterlog(PlayerEntity player, WorldSchematic schematicWorld, BlockPos pos) {
		if (!FluidSourceTarget.withinRenderLayers(pos)) {
			return false; // 不在玩家当前渲染的层里:不补
		}
		BlockState desired = schematicWorld.getBlockState(pos);
		if (!desired.contains(Properties.WATERLOGGED) || !Boolean.TRUE.equals(desired.get(Properties.WATERLOGGED))) {
			return false;
		}
		BlockState current = player.getWorld().getBlockState(pos);
		return !current.contains(Properties.WATERLOGGED) || !Boolean.TRUE.equals(current.get(Properties.WATERLOGGED));
	}

	/** 背包(含快捷栏/副手)里是否有该物品;背包里没有时,按规则取值再看背包里的潜影盒。 */
	public static boolean hasItemInInventory(PlayerEntity player, Item item) {
		return hao$hasItem(player, item);
	}

	private static boolean hao$hasItem(PlayerEntity player, Item item) {
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (!stack.isEmpty() && stack.isOf(item)) {
				return true;
			}
		}
		if (EasyPlaceWaterloggedSettings.shulkerMode() == EasyPlaceWaterloggedSettings.ShulkerMode.FALSE) {
			return false;
		}
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack boxStack = player.getInventory().getStack(slot);
			if (boxStack.isEmpty() || !(boxStack.getItem() instanceof BlockItem blockItem)
					|| !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
				continue;
			}
			ContainerComponent container = boxStack.get(DataComponentTypes.CONTAINER);
			if (container == null) {
				continue;
			}
			for (ItemStack inner : container.iterateNonEmpty()) {
				if (inner.isOf(item)) {
					return true;
				}
			}
		}
		return false;
	}
}

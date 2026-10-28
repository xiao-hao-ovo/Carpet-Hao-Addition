package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.HaoDebug;
import carpet_hao_addition.waterlogged.WaterloggedFillHandler;
import carpet_hao_addition.waterlogged.WaterloggedFillPayload;
import carpet_hao_addition.waterlogged.FluidSourceTarget;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * haoProjectionWaterlogged(客户端):告诉服务端「正在轻松放置」以及「哪一格该补什么」。
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
public abstract class WorldUtils_waterloggedFillMixin {
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
		// 触发条件:站立触发时要求未潜行,蹲下触发时要求正在潜行。
		if (!EasyPlaceWaterloggedSettings.triggerAllowed(player.isSneaking())) {
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
		BlockPos target = hao$fluidTarget(minecraft, player, schematicWorld);
		if (target != null) {
			int kind = hao$missingKind(player, schematicWorld, target);
			if (HaoDebug.ENABLED) {
				HaoDebug.log("[hao-fluid] branch1 target=" + target + " kind=" + kind
						+ " hasMaterial=" + hao$hasMaterial(player, kind));
			}
			if (kind != WaterloggedFillPayload.KIND_NONE && hao$hasMaterial(player, kind)
					&& hao$markRequested(player, target)) {
				hao$lastSentTick = tick;
				WaterloggedFillHandler.registerPayloadType();
				// 服务端不认这个通道（没装本模组/版本对不上）时，别拦原版放置 ——
				// 否则客户端已经返回 SUCCESS，玩家右键毫无反应、也不扣料，而且没有任何提示。
				if (!ClientPlayNetworking.canSend(WaterloggedFillPayload.ID)) {
					return;
				}
				// 与 doItemUse 那条入口互斥:同一条水柱这一次只放一格(详见 FluidSourceTarget#claim)。
				if (!FluidSourceTarget.claim(player, target)) {
					return;
				}
				ClientPlayNetworking.send(new WaterloggedFillPayload(true, kind, List.of(target)));
				if (kind != WaterloggedFillPayload.KIND_LAVA_CAULDRON) {
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
		BlockPos waterlogCenter = target;
		// 流体目标没命中时,直接看准星正对着的那一格(与 ClientWaterlogNotifier 同一判定),
		// 不依赖 hao$litematicaTarget:它跟着 Litematica 的"目标流体/紧贴显示"设置走,
		// 而"没水 vs 有水"在射线里往往不算"不同方块",会命中不到,导致补水中心落空。
		if (waterlogCenter == null && minecraft.crosshairTarget instanceof BlockHitResult chit) {
			waterlogCenter = chit.getBlockPos();
		}
		List<BlockPos> waterlogTargets = hao$collectWaterlogTargets(player, schematicWorld, waterlogCenter);
		// 没有要补的格就不发包(服务端清理只靠 active=false 与规则关闭兜底,空包本就是 no-op,纯浪费带宽)。
		if (waterlogTargets.isEmpty()) {
			return;
		}
		hao$lastSentTick = tick;
		WaterloggedFillHandler.registerPayloadType();
		if (!ClientPlayNetworking.canSend(WaterloggedFillPayload.ID)) {
			return; // 服务端不认这个通道:这一包没意义,直接放弃
		}
		ClientPlayNetworking.send(new WaterloggedFillPayload(true,
				WaterloggedFillPayload.KIND_WATERLOG, waterlogTargets));
		// 方块已经放好、只差含水 → 这次操作只为补水,拦下它,Litematica/原版别把手上的方块再放一遍。
		// 方块还没放下的格不拦:那一下要靠放置流程把方块放下来,服务端随后再补水。
		if (waterlogTargets.size() == 1 && hao$isPureRefill(player, schematicWorld, waterlogTargets.get(0))) {
			cir.setReturnValue(ActionResult.SUCCESS);
		}
	}

	/** 目标格投影里该放什么、而实际还没有 —— 返回请求类型;不需要处理时返回 {@code KIND_NONE}。 */
	@Unique
	private static int hao$missingKind(PlayerEntity player, WorldSchematic schematicWorld, BlockPos pos) {
		return FluidSourceTarget.missingKind(player, schematicWorld, pos);
	}

	/** 请求类型需要消耗的材料是否充足:水→冰,岩浆→岩浆块,装岩浆的炼药锅→炼药锅+岩浆块。 */
	@Unique
	private static boolean hao$hasMaterial(PlayerEntity player, int kind) {
		return FluidSourceTarget.hasMaterial(player, kind);
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
	 * 取「这一下右键该补水/岩浆/点火的那一格」。
	 * <p>
	 * 判定与射线拾取都放在 {@link FluidSourceTarget},与 {@code ClientWaterlogNotifier.onUseItem}
	 * 共用同一份逻辑:水源格在现实里是空气,准星打不到任何方块,{@code doItemUse} 那条钩子才是
	 * 唯一能拦住它的入口,两条路径必须给出同一个答案,否则一边能放、一边不能。
	 * 两条入口都先查「投影轻松放置是否开着」,关掉时本规则不参与任何操作。
	 */
	@Unique
	private static BlockPos hao$fluidTarget(MinecraftClient minecraft, PlayerEntity player,
			WorldSchematic schematicWorld) {
		BlockPos target = FluidSourceTarget.lookedAt(minecraft, player, schematicWorld);
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-fluid] fluidTarget=" + target);
		}
		return target;
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

	/**
	 * 只检查投影命中的<i>那一格</i>:投影里含水、实际还没水才上报。
	 * <p>
	 * 之前用 ±1 容差找"最近的待补水格",导致没对准也会把旁边一格补掉;
	 * 按用户要求改为严格按投影所对准的位置补水,不再带容差。
	 */
	@Unique
	private static List<BlockPos> hao$collectWaterlogTargets(PlayerEntity player, WorldSchematic schematicWorld,
			BlockPos center) {
		List<BlockPos> result = new ArrayList<>();
		if (center != null && hao$hasItem(player, Items.ICE)
				&& hao$needsWaterlog(player, schematicWorld, center)) {
			result.add(center);
		}
		return result;
	}

	/**
	 * 该格是否「已经放着投影里那个方块、只是没含水」。
	 * <p>
	 * 只有这种情况才算「纯补水」:方块还没放(空气/别的方块)时,这一次操作仍然要靠放置流程
	 * 把方块放下,服务端随后补含水 —— 拦掉就会变成「右键毫无反应」。
	 */
	@Unique
	private static boolean hao$isPureRefill(PlayerEntity player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		BlockState current = player.getWorld().getBlockState(pos);
		return current.getBlock() == desired.getBlock() && hao$needsWaterlog(player, schematicWorld, pos);
	}

	/** 清理过期的请求记录。 */
	@Unique
	private static void hao$pruneWaterRequests(long now) {
		hao$recentWaterRequests.entrySet().removeIf(e -> now - e.getValue() > HAO_REQUEST_TTL);
	}

	/**
	 * 背包(含快捷栏/副手)里是否有该物品;背包里没有时,再看背包里的潜影盒。
	 * <p>
	 * 潜影盒取货不依赖任何客户端潜影盒 mod:这里直接读潜影盒的 container 组件,
	 * 服务端扣材料时用同样的方式写回组件。
	 */
	@Unique
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

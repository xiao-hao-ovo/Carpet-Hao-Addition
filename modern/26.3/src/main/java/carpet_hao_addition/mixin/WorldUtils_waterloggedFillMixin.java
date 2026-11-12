package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.HaoDebug;
import carpet_hao_addition.WaterloggedFillHandler;
import carpet_hao_addition.WaterloggedFillPayload;
import carpet_hao_addition.FluidSourceTarget;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

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
 * 结构与 1.21.8 的 {@code WorldUtils_waterloggedFillMixin} 一致:钩子统一挂在
 * {@code WorldUtils.doEasyPlaceAction}(轻松放置总入口),而不是 26.x 早期的
 * {@code PlacementHandler.applyPlacementProtocolToPlacementState} —— 后者在 0.27.14 的
 * easy place 主路径里不一定被调用,开了规则也不会触发。
 * <p>
 * <b>priority = 800</b>:litematica-printer 在 {@code doEasyPlaceAction} 的 HEAD 以 900
 * 整体取消方法,本模组必须排在它前面,否则补料请求一个都发不出去。
 */
@Mixin(value = WorldUtils.class, priority = 800)
public abstract class WorldUtils_waterloggedFillMixin {
	/** 上次发包所在的客户端 tick,用于每 tick 限一次。 */
	@Unique
	private static long hao$lastSentTick = Long.MIN_VALUE;
	@Unique
	private static final java.util.Map<BlockPos, Long> hao$recentWaterRequests = new java.util.HashMap<>();
	@Unique
	private static final long HAO_REQUEST_TTL = 40L;
	@Unique
	private static final double HAO_TRACE_RANGE = 6.0;

	@Inject(method = "doEasyPlaceAction", at = @At("HEAD"), cancellable = true)
	private static void hao$waterlogOnEasyPlace(Minecraft minecraft, CallbackInfoReturnable<InteractionResult> cir) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		Player player = minecraft.player;
		if (player == null) {
			return;
		}
		if (!EasyPlaceWaterloggedSettings.triggerAllowed(player.isShiftKeyDown())) {
			return;
		}
		long tick = player.level().getGameTime();
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
				if (!ClientPlayNetworking.canSend(WaterloggedFillPayload.ID)) {
					return;
				}
				if (!FluidSourceTarget.claim(player, target)) {
					return;
				}
				ClientPlayNetworking.send(new WaterloggedFillPayload(true, kind, List.of(target)));
				if (kind != WaterloggedFillPayload.KIND_LAVA_CAULDRON) {
					cir.setReturnValue(InteractionResult.SUCCESS);
				}
				return;
			}
		}

		// 2) 含水方块补水:把"投影里含水、实际还没水"的那一格报给服务端。
		BlockPos waterlogCenter = target;
		if (waterlogCenter == null && minecraft.hitResult instanceof BlockHitResult chit) {
			waterlogCenter = chit.getBlockPos();
		}
		List<BlockPos> waterlogTargets = hao$collectWaterlogTargets(player, schematicWorld, waterlogCenter);
		if (waterlogTargets.isEmpty()) {
			return;
		}
		hao$lastSentTick = tick;
		WaterloggedFillHandler.registerPayloadType();
		if (!ClientPlayNetworking.canSend(WaterloggedFillPayload.ID)) {
			return;
		}
		ClientPlayNetworking.send(new WaterloggedFillPayload(true,
				WaterloggedFillPayload.KIND_WATERLOG, waterlogTargets));
		if (waterlogTargets.size() == 1 && hao$isPureRefill(player, schematicWorld, waterlogTargets.get(0))) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}

	@Unique
	private static int hao$missingKind(Player player, WorldSchematic schematicWorld, BlockPos pos) {
		return FluidSourceTarget.missingKind(player, schematicWorld, pos);
	}

	@Unique
	private static boolean hao$hasMaterial(Player player, int kind) {
		return FluidSourceTarget.hasMaterial(player, kind);
	}

	@Unique
	private static boolean hao$markRequested(Player player, BlockPos pos) {
		long now = player.level().getGameTime();
		Long last = hao$recentWaterRequests.get(pos);
		if (last != null && now - last <= HAO_REQUEST_TTL) {
			return false;
		}
		hao$recentWaterRequests.put(pos, now);
		hao$pruneWaterRequests(now);
		return true;
	}

	@Unique
	private static BlockPos hao$fluidTarget(Minecraft minecraft, Player player, WorldSchematic schematicWorld) {
		BlockPos target = FluidSourceTarget.lookedAt(minecraft, player, schematicWorld);
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-fluid] fluidTarget=" + target);
		}
		return target;
	}

	@Unique
	private static boolean hao$needsWaterlog(Player player, WorldSchematic schematicWorld, BlockPos pos) {
		if (!FluidSourceTarget.withinRenderLayers(pos)) {
			return false;
		}
		BlockState desired = schematicWorld.getBlockState(pos);
		if (!desired.hasProperty(BlockStateProperties.WATERLOGGED)
				|| !Boolean.TRUE.equals(desired.getValue(BlockStateProperties.WATERLOGGED))) {
			return false;
		}
		BlockState current = player.level().getBlockState(pos);
		return !current.hasProperty(BlockStateProperties.WATERLOGGED)
				|| !Boolean.TRUE.equals(current.getValue(BlockStateProperties.WATERLOGGED));
	}

	@Unique
	private static List<BlockPos> hao$collectWaterlogTargets(Player player, WorldSchematic schematicWorld,
			BlockPos center) {
		List<BlockPos> result = new ArrayList<>();
		if (center != null && hao$hasItem(player, Items.ICE)
				&& hao$needsWaterlog(player, schematicWorld, center)) {
			result.add(center);
		}
		return result;
	}

	@Unique
	private static boolean hao$isPureRefill(Player player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		BlockState current = player.level().getBlockState(pos);
		return current.getBlock() == desired.getBlock() && hao$needsWaterlog(player, schematicWorld, pos);
	}

	@Unique
	private static void hao$pruneWaterRequests(long now) {
		hao$recentWaterRequests.entrySet().removeIf(e -> now - e.getValue() > HAO_REQUEST_TTL);
	}

	@Unique
	private static boolean hao$hasItem(Player player, Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && stack.is(item)) {
				return true;
			}
		}
		if (EasyPlaceWaterloggedSettings.shulkerMode() == EasyPlaceWaterloggedSettings.ShulkerMode.FALSE) {
			return false;
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack boxStack = player.getInventory().getItem(slot);
			if (boxStack.isEmpty() || !(boxStack.getItem() instanceof BlockItem blockItem)
					|| !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
				continue;
			}
			ItemContainerContents container = boxStack.get(DataComponents.CONTAINER);
			if (container == null) {
				continue;
			}
			for (ItemStackTemplate inner : container.nonEmptyItems()) {
				if (inner.create().is(item)) {
					return true;
				}
			}
		}
		return false;
	}
}
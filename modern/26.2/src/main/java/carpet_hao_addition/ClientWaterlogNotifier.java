package carpet_hao_addition;

import carpet_hao_addition.HaoDebug;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * haoProjectionWaterlogged 的补水/补料上报(客户端)。
 * <p>移植自 1.21.8 的 {@code waterlogged.ClientWaterlogNotifier}，换成 26.x 的 Mojang 映射。</p>
 */
public final class ClientWaterlogNotifier {
	private static long hao$lastSentTick = Long.MIN_VALUE;
	private static long hao$lastHintTick = Long.MIN_VALUE;
	private static final long HAO_HINT_INTERVAL = 20L;

	private ClientWaterlogNotifier() {
	}

	public static boolean easyPlaceEnabled() {
		try {
			return Configs.Generic.EASY_PLACE_MODE.getBooleanValue();
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static boolean onUseItem(Minecraft minecraft) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return false;
		}
		if (!easyPlaceEnabled()) {
			return false;
		}
		Player player = minecraft.player;
		if (player == null) {
			return false;
		}
		if (!EasyPlaceWaterloggedSettings.triggerAllowed(player.isShiftKeyDown())) {
			return false;
		}
		WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
		if (schematicWorld == null) {
			return false;
		}
		BlockPos sourceCell = FluidSourceTarget.lookedAt(minecraft, player, schematicWorld);
		if (sourceCell != null) {
			int kind = FluidSourceTarget.missingKind(player, schematicWorld, sourceCell);
			if (kind != WaterloggedFillPayload.KIND_NONE) {
				hao$requestFluidSource(player, schematicWorld, sourceCell, kind);
				return kind != WaterloggedFillPayload.KIND_LAVA_CAULDRON;
			}
		}
		HitResult hitResult = minecraft.hitResult;
		if (!(hitResult instanceof BlockHitResult hit) || hitResult.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		List<BlockPos> targets = hao$collectTargets(player, schematicWorld, hit.getBlockPos());
		if (targets.isEmpty()) {
			return false;
		}
		long tick = player.level().getGameTime();
		if (hao$lastSentTick == tick) {
			return false;
		}
		if (!hao$hasItem(player, Items.ICE)) {
			if (tick - hao$lastHintTick >= HAO_HINT_INTERVAL) {
				hao$lastHintTick = tick;
				player.sendSystemMessage(Component.literal("§e补水需要背包或潜影盒里有冰块"));
			}
			return false;
		}
		WaterloggedFillHandler.registerPayloadType();
		if (!ClientPlayNetworking.canSend(WaterloggedFillPayload.ID)) {
			return false;
		}
		hao$lastSentTick = tick;
		ClientPlayNetworking.send(new WaterloggedFillPayload(true,
				WaterloggedFillPayload.KIND_WATERLOG, targets));
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-waterlog] 右键上报补水 " + targets);
		}
		return hao$isPureRefill(player, schematicWorld, targets.get(0));
	}

	private static Boolean hao$requestFluidSource(Player player, WorldSchematic schematicWorld,
			BlockPos pos, int kind) {
		long tick = player.level().getGameTime();
		if (hao$lastSentTick == tick) {
			return null;
		}
		if (!FluidSourceTarget.hasMaterial(player, kind)) {
			if (tick - hao$lastHintTick >= HAO_HINT_INTERVAL) {
				hao$lastHintTick = tick;
				player.sendSystemMessage(Component.literal("§e需要背包或潜影盒里有"
						+ FluidSourceTarget.materialName(kind)));
			}
			return null;
		}
		WaterloggedFillHandler.registerPayloadType();
		if (!ClientPlayNetworking.canSend(WaterloggedFillPayload.ID)) {
			return null;
		}
		if (!FluidSourceTarget.claim(player, pos)) {
			return null;
		}
		hao$lastSentTick = tick;
		ClientPlayNetworking.send(new WaterloggedFillPayload(true, kind, List.of(pos)));
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-fluid] 右键直接放 kind=" + kind + " " + pos);
		}
		return kind != WaterloggedFillPayload.KIND_LAVA_CAULDRON;
	}

	private static boolean hao$isPureRefill(Player player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		BlockState current = player.level().getBlockState(pos);
		return current.getBlock() == desired.getBlock() && hao$needsWaterlog(player, schematicWorld, pos);
	}

	public static boolean isPureRefillTarget(Minecraft minecraft, BlockPos pos) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return false;
		}
		if (!easyPlaceEnabled()) {
			return false;
		}
		Player player = minecraft.player;
		if (player == null) {
			return false;
		}
		if (!EasyPlaceWaterloggedSettings.triggerAllowed(player.isShiftKeyDown())) {
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

	private static List<BlockPos> hao$collectTargets(Player player, WorldSchematic schematicWorld,
			BlockPos center) {
		List<BlockPos> result = new ArrayList<>();
		if (center != null && hao$needsWaterlog(player, schematicWorld, center)) {
			result.add(center);
		}
		return result;
	}

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

	public static boolean hasItemInInventory(Player player, Item item) {
		return hao$hasItem(player, item);
	}

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
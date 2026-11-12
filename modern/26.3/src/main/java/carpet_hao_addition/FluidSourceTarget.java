package carpet_hao_addition;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerMode;
import fi.dy.masa.malilib.util.position.LayerRange;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 「投影里该有水/岩浆/火,而实际还没有」那一格的判定与射线拾取(客户端)。
 * <p>移植自 1.21.8 的 {@code waterlogged.FluidSourceTarget}，换成 26.x 的 Mojang 映射。</p>
 */
public final class FluidSourceTarget {
	public static final double TRACE_RANGE = 6.0;

	private FluidSourceTarget() {
	}

	public static BlockPos lookedAt(Minecraft minecraft, Player player, WorldSchematic schematicWorld) {
		RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(minecraft.level, player, TRACE_RANGE);
		BlockPos fromGeneric = pick(player, schematicWorld, wrapper);
		if (fromGeneric != null) {
			return fromGeneric;
		}
		HitResult vanilla = RayTraceUtils.getRayTraceFromEntity(minecraft.level, player, true, TRACE_RANGE);
		if (vanilla != null && vanilla.getType() != HitResult.Type.MISS) {
			return null;
		}
		BlockHitResult schematicHit = RayTraceUtils.traceToSchematicWorld(player, TRACE_RANGE, true, true);
		if (schematicHit == null || schematicHit.getType() == HitResult.Type.MISS) {
			return null;
		}
		return pick(player, schematicWorld, schematicHit);
	}

	private static final java.util.Map<Long, Long> HAO_RECENT_COLUMNS = new java.util.HashMap<>();
	private static final long HAO_COLUMN_TTL = 4L;
	private static long hao$lastSentTick = Long.MIN_VALUE;

	public static boolean claim(Player player, BlockPos pos) {
		long now = player.level().getGameTime();
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

	private static BlockPos pick(Player player, WorldSchematic schematicWorld, RayTraceWrapper wrapper) {
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

	private static BlockPos pick(Player player, WorldSchematic schematicWorld, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (missingKind(player, schematicWorld, pos) != WaterloggedFillPayload.KIND_NONE) {
			return pos;
		}
		if (hit.getDirection() != null) {
			BlockPos adjacent = pos.relative(hit.getDirection());
			if (missingKind(player, schematicWorld, adjacent) != WaterloggedFillPayload.KIND_NONE) {
				return adjacent;
			}
		}
		return null;
	}

	public static boolean withinRenderLayers(BlockPos pos) {
		try {
			LayerRange range = DataManager.getRenderLayerRange();
			if (range == null || range.getLayerMode() == LayerMode.ALL) {
				return true;
			}
			return range.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ());
		} catch (Throwable ignored) {
			return true;
		}
	}

	public static int missingKind(Player player, WorldSchematic schematicWorld, BlockPos pos) {
		if (!withinRenderLayers(pos)) {
			return WaterloggedFillPayload.KIND_NONE;
		}
		BlockState desired = schematicWorld.getBlockState(pos);
		FluidState fluid = player.level().getFluidState(pos);
		boolean fluidMissing = fluid.isEmpty() || !fluid.isSource();

		if (desired.is(Blocks.LAVA_CAULDRON)) {
			return player.level().getBlockState(pos).is(Blocks.LAVA_CAULDRON)
					? WaterloggedFillPayload.KIND_NONE
					: WaterloggedFillPayload.KIND_LAVA_CAULDRON;
		}
		if ((desired.is(Blocks.WATER) && desired.getFluidState().isSource()) || desired.is(Blocks.BUBBLE_COLUMN)) {
			return fluidMissing ? WaterloggedFillPayload.KIND_WATER : WaterloggedFillPayload.KIND_NONE;
		}
		if (desired.is(Blocks.LAVA) && desired.getFluidState().isSource()) {
			return fluidMissing ? WaterloggedFillPayload.KIND_LAVA : WaterloggedFillPayload.KIND_NONE;
		}
		if (desired.is(Blocks.FIRE) || desired.is(Blocks.SOUL_FIRE) || desired.is(Blocks.NETHER_PORTAL)) {
			BlockState current = player.level().getBlockState(pos);
			boolean lit = current.is(Blocks.FIRE) || current.is(Blocks.SOUL_FIRE)
					|| current.is(Blocks.NETHER_PORTAL);
			return lit ? WaterloggedFillPayload.KIND_NONE : WaterloggedFillPayload.KIND_IGNITE;
		}
		return WaterloggedFillPayload.KIND_NONE;
	}

	public static boolean hasMaterial(Player player, int kind) {
		return switch (kind) {
			case WaterloggedFillPayload.KIND_WATER -> hasItem(player, Items.ICE);
			case WaterloggedFillPayload.KIND_LAVA -> hasItem(player, Items.MAGMA_BLOCK);
			case WaterloggedFillPayload.KIND_LAVA_CAULDRON -> hasItem(player, Items.MAGMA_BLOCK);
			case WaterloggedFillPayload.KIND_IGNITE ->
					hasItem(player, Items.FIRE_CHARGE) || hasItem(player, Items.FLINT_AND_STEEL);
			default -> false;
		};
	}

	public static String materialName(int kind) {
		return switch (kind) {
			case WaterloggedFillPayload.KIND_WATER -> "冰块";
			case WaterloggedFillPayload.KIND_LAVA, WaterloggedFillPayload.KIND_LAVA_CAULDRON -> "岩浆块";
			case WaterloggedFillPayload.KIND_IGNITE -> "火焰弹或打火石";
			default -> "材料";
		};
	}

	private static boolean hasItem(Player player, Item item) {
		return ClientWaterlogNotifier.hasItemInInventory(player, item);
	}
}
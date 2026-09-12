package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.PlaceWaterloggedHandler;
import carpet_hao_addition.PlaceWaterloggedPayload;

import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * easyPlaceWaterlogged(客户端,26.x):告诉服务端「正在轻松放置」以及「哪些格该补什么」。
 * <p>
 * 钩子挂在 {@link PlacementHandler#applyPlacementProtocolToPlacementState}:它是 Litematica 轻松放置
 * 给每个待放方块计算最终状态时都会调用的方法,{@code UseContext.pos()} 就是本次目标位置。
 * <b>只发请求,不修改返回值</b> —— 改写放置状态会让状态与实际位置不符,服务端校验失败,方块放不下去。
 * <p>
 * 三类东西没有对应的可放置物品,原版与 Litematica 都不会放下,所以由客户端报给服务端:
 * 水源(含气泡柱)消耗冰、源岩浆消耗岩浆块、装岩浆的炼药锅按正常消耗炼药锅物品(岩浆另扣岩浆块);
 * 另外把「投影里含水、而实际还没水」的格(目标格 ±1 内整批)报给服务端,由服务端在其放上方块后补含水。
 */
@Mixin(PlacementHandler.class)
public abstract class WorldUtils_easyPlaceWaterloggedMixin {
	/** 上次发包所在的客户端 tick,用于每 tick 限一次。 */
	@Unique
	private static long hao$lastSentTick = Long.MIN_VALUE;
	/** 最近请求过的位置 → 请求时的 tick,避免同一格被重复请求。 */
	@Unique
	private static final java.util.Map<BlockPos, Long> hao$recentRequests = new java.util.HashMap<>();
	/** 位置请求后的保护时长(客户端 tick)。 */
	@Unique
	private static final long HAO_REQUEST_TTL = 40L;

	@Inject(method = "applyPlacementProtocolToPlacementState", at = @At("RETURN"))
	private static void hao$requestWater(BlockState state, PlacementHandler.UseContext context,
			CallbackInfoReturnable<BlockState> cir) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		Player player = context.entity() instanceof Player p ? p : null;
		if (player == null || !player.level().isClientSide()) {
			return;
		}
		WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
		if (schematicWorld == null) {
			return;
		}
		long tick = player.level().getGameTime();
		if (hao$lastSentTick == tick) {
			return;
		}
		BlockPos center = context.pos();
		if (center == null) {
			return;
		}

		// 1) 投影里是水/岩浆/装岩浆的炼药锅:都没有可放置物品,Litematica 不会放下它们。
		int kind = hao$missingKind(player, schematicWorld, center);
		if (kind != PlaceWaterloggedPayload.KIND_NONE && hao$hasMaterial(player, kind)
				&& hao$markRequested(player, center)) {
			hao$lastSentTick = tick;
			PlaceWaterloggedHandler.registerPayloadType();
			ClientPlayNetworking.send(new PlaceWaterloggedPayload(true, kind, List.of(center)));
			return;
		}

		// 2) 含水方块补水:中心 ±1 内"投影里含水、实际还没水"的格整批上报,由服务端在方块落下后补。
		List<BlockPos> waterlogTargets = hao$collectWaterlogTargets(player, schematicWorld, center);
		hao$lastSentTick = tick;
		PlaceWaterloggedHandler.registerPayloadType();
		ClientPlayNetworking.send(new PlaceWaterloggedPayload(true,
				waterlogTargets.isEmpty() ? PlaceWaterloggedPayload.KIND_NONE : PlaceWaterloggedPayload.KIND_WATERLOG,
				waterlogTargets));
	}

	/** 目标格投影里该放什么、而实际还没有 —— 返回请求类型;不需要处理时返回 {@code KIND_NONE}。 */
	@Unique
	private static int hao$missingKind(Player player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		FluidState fluid = player.level().getFluidState(pos);
		// 流动的水/岩浆不算"已经有了":源格被流动流体覆盖时仍要放一格源把它补正。
		boolean fluidMissing = fluid.isEmpty() || !fluid.isSource();

		if (desired.is(Blocks.LAVA_CAULDRON)) {
			return player.level().getBlockState(pos).is(Blocks.LAVA_CAULDRON)
					? PlaceWaterloggedPayload.KIND_NONE
					: PlaceWaterloggedPayload.KIND_LAVA_CAULDRON;
		}
		// 灵魂沙/岩浆块上方的水在投影里存成 bubble_column(气泡柱)方块,它不是 WATER,但同样该补。
		if ((desired.is(Blocks.WATER) && desired.getFluidState().isSource()) || desired.is(Blocks.BUBBLE_COLUMN)) {
			return fluidMissing ? PlaceWaterloggedPayload.KIND_WATER : PlaceWaterloggedPayload.KIND_NONE;
		}
		if (desired.is(Blocks.LAVA) && desired.getFluidState().isSource()) {
			return fluidMissing ? PlaceWaterloggedPayload.KIND_LAVA : PlaceWaterloggedPayload.KIND_NONE;
		}
		return PlaceWaterloggedPayload.KIND_NONE;
	}

	/** 请求类型需要消耗的材料是否充足:水→冰,岩浆→岩浆块,装岩浆的炼药锅→岩浆块(炼药锅由正常放置扣)。 */
	@Unique
	private static boolean hao$hasMaterial(Player player, int kind) {
		return switch (kind) {
			case PlaceWaterloggedPayload.KIND_WATER -> hao$hasItem(player, Items.ICE);
			case PlaceWaterloggedPayload.KIND_LAVA -> hao$hasItem(player, Items.MAGMA_BLOCK);
			case PlaceWaterloggedPayload.KIND_LAVA_CAULDRON -> hao$hasItem(player, Items.MAGMA_BLOCK);
			default -> false;
		};
	}

	/** 同一格短时间内只请求一次(流体的更新要下一 tick 才生效,否则会被重复放)。 */
	@Unique
	private static boolean hao$markRequested(Player player, BlockPos pos) {
		long now = player.level().getGameTime();
		Long last = hao$recentRequests.get(pos);
		if (last != null && now - last <= HAO_REQUEST_TTL) {
			return false;
		}
		hao$recentRequests.put(pos, now);
		hao$recentRequests.entrySet().removeIf(entry -> now - entry.getValue() > HAO_REQUEST_TTL);
		return true;
	}

	/** 该格在投影里是否「含水、而实际还没水」—— 是则会被上报给服务端,在其放上方块后补含水并扣冰。 */
	@Unique
	private static boolean hao$needsWaterlog(Player player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		if (!desired.hasProperty(BlockStateProperties.WATERLOGGED)
				|| !Boolean.TRUE.equals(desired.getValue(BlockStateProperties.WATERLOGGED))) {
			return false;
		}
		BlockState current = player.level().getBlockState(pos);
		return !current.hasProperty(BlockStateProperties.WATERLOGGED)
				|| !Boolean.TRUE.equals(current.getValue(BlockStateProperties.WATERLOGGED));
	}

	/**
	 * 在中心附近 ±1 格内收集"投影里含水、而实际还没水"的格(需要有冰)。
	 * <p>
	 * 不要求目标格算得完全准:Litematica 目标格与实际放置位置可能差一格,整批上报后由服务端在
	 * 这些格真的放上方块时补水,偶发的位置偏差就不会漏。
	 */
	@Unique
	private static List<BlockPos> hao$collectWaterlogTargets(Player player, WorldSchematic schematicWorld,
			BlockPos center) {
		List<BlockPos> result = new ArrayList<>();
		if (center == null || !hao$hasItem(player, Items.ICE)) {
			return result;
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos pos = center.offset(dx, dy, dz);
					if (hao$needsWaterlog(player, schematicWorld, pos)) {
						result.add(pos);
					}
				}
			}
		}
		return result;
	}

	@Unique
	private static boolean hao$hasItem(Player player, Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && stack.is(item)) {
				return true;
			}
		}
		return false;
	}
}

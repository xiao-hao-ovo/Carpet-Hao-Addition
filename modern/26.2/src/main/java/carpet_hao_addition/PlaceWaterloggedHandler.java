package carpet_hao_addition;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * easyPlaceWaterlogged 的注册与结算。
 * <p>
 * 三类东西在投影里出现,但都没有"直接可放"的物品形式,原版与 Litematica 都不会正确落下:
 * <ul>
 *   <li><b>水源 / 气泡柱</b> —— 服务端直接放一格水源,扣 1 个冰;</li>
 *   <li><b>源岩浆</b> —— 服务端直接放一格源岩浆,扣 1 个岩浆块;</li>
 *   <li><b>装岩浆的炼药锅</b> —— 炼药锅走正常放置流程,服务端只在方块落下后把那一格换成装岩浆的
 *       炼药锅,再扣 1 个岩浆块。</li>
 * </ul>
 * <b>含水方块补水用"每 tick 轮询"而不是监听方块放置事件</b>:方块放置事件靠注入
 * {@code BlockItem#place},该方法容易被其它扩展做取消型注入,一旦被取消我们的注入就整段跳过,
 * 表现为"偶尔不补水"。改成:客户端把待补水格报过来,服务端每 tick 检查这些格 —— 方块一落上去
 * (可含水且还没含水)就补,与"谁在什么时候放、走的哪条路径"无关。
 */
public final class PlaceWaterloggedHandler {
	/** 待补水格:玩家 UUID → (格子 → 请求时刻)。 */
	private static final Map<UUID, Map<BlockPos, Long>> PENDING_WATERLOG = new HashMap<>();
	/** 待灌岩浆的炼药锅格。 */
	private static final Map<UUID, Map<BlockPos, Long>> PENDING_LAVA_CAULDRON = new HashMap<>();
	/** 待处理记录的存活时长(服务端刻)。 */
	private static final long PENDING_TTL = 40L;

	private static boolean payloadRegistered;
	private static boolean receiverRegistered;
	private static boolean tickRegistered;

	private PlaceWaterloggedHandler() {
	}

	/** 注册 C2S payload 类型。客户端与服务端都要调用(幂等)。 */
	public static void registerPayloadType() {
		if (payloadRegistered) {
			return;
		}
		payloadRegistered = true;
		PayloadTypeRegistry.serverboundPlay().register(PlaceWaterloggedPayload.ID, PlaceWaterloggedPayload.CODEC);
	}

	/** 注册服务端接收端与每 tick 轮询(仅服务端调用,幂等)。 */
	public static void registerServerReceiver() {
		registerPayloadType();
		registerTickHandler();
		if (receiverRegistered) {
			return;
		}
		receiverRegistered = true;
		ServerPlayNetworking.registerGlobalReceiver(PlaceWaterloggedPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				if (!EasyPlaceWaterloggedSettings.isEnabled() || !payload.active()) {
					PENDING_WATERLOG.remove(player.getUUID());
					PENDING_LAVA_CAULDRON.remove(player.getUUID());
					return;
				}
				ServerLevel world = player.level();
				long now = world.getGameTime();
				switch (payload.kind()) {
					case PlaceWaterloggedPayload.KIND_WATERLOG -> {
						markPending(PENDING_WATERLOG, player, payload.positions(), now);
						prunePending(PENDING_WATERLOG, now);
					}
					case PlaceWaterloggedPayload.KIND_LAVA_CAULDRON -> {
						markPending(PENDING_LAVA_CAULDRON, player, payload.positions(), now);
						prunePending(PENDING_LAVA_CAULDRON, now);
					}
					default -> {
						for (BlockPos pos : payload.positions()) {
							placeRequested(player, world, pos, payload.kind());
						}
					}
				}
			});
		});
	}

	/** 注册每 tick 检查(幂等)。 */
	public static void registerTickHandler() {
		if (tickRegistered) {
			return;
		}
		tickRegistered = true;
		ServerTickEvents.END_SERVER_TICK.register(PlaceWaterloggedHandler::onServerTick);
	}

	/** 每 tick 检查待处理格:炼药锅灌岩浆、含水方块补水。 */
	private static void onServerTick(MinecraftServer server) {
		if (PENDING_WATERLOG.isEmpty() && PENDING_LAVA_CAULDRON.isEmpty()) {
			return;
		}
		processPending(server, PENDING_LAVA_CAULDRON, PlaceWaterloggedHandler::tryFillCauldron);
		processPending(server, PENDING_WATERLOG, PlaceWaterloggedHandler::tryWaterlog);
	}

	private interface PendingAction {
		/** @return true 表示这一格已处理完,可以从名单里移除。 */
		boolean apply(ServerPlayer player, ServerLevel world, BlockPos pos);
	}

	private static void processPending(MinecraftServer server, Map<UUID, Map<BlockPos, Long>> table,
			PendingAction action) {
		if (table.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Map<BlockPos, Long>>> it = table.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Map<BlockPos, Long>> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			ServerLevel world = player.level();
			long now = world.getGameTime();
			Iterator<Map.Entry<BlockPos, Long>> pendingIterator = entry.getValue().entrySet().iterator();
			while (pendingIterator.hasNext()) {
				Map.Entry<BlockPos, Long> pending = pendingIterator.next();
				if (now - pending.getValue() > PENDING_TTL) {
					pendingIterator.remove();
					continue;
				}
				if (action.apply(player, world, pending.getKey())) {
					pendingIterator.remove();
				}
			}
			if (entry.getValue().isEmpty()) {
				it.remove();
			}
		}
	}

	/** 该格已经是(空)炼药锅 → 换成装岩浆的炼药锅,扣 1 个岩浆块。 */
	private static boolean tryFillCauldron(ServerPlayer player, ServerLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.is(Blocks.LAVA_CAULDRON)) {
			return true;
		}
		if (state.is(Blocks.CAULDRON)) {
			if (!consumeOne(player, Items.MAGMA_BLOCK)) {
				return false; // 暂时没有岩浆块:留着,等有材料或过期
			}
			world.setBlock(pos, Blocks.LAVA_CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
			return true;
		}
		// 落的是别的方块(不是炼药锅)→ 这次目标作废;还是空气则继续等它落下。
		return !state.isAir();
	}

	/** 该格已放上"可含水却没含水"的方块 → 补上含水,扣 1 个冰。 */
	private static boolean tryWaterlog(ServerPlayer player, ServerLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) {
			return false; // 还没放上方块:继续等
		}
		if (!state.hasProperty(BlockStateProperties.WATERLOGGED)
				|| Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED))) {
			return true; // 不用补水 / 已经含水 → 这次目标结束
		}
		if (!consumeOne(player, Items.ICE)) {
			return false; // 暂时没有冰:留着,等有材料或过期
		}
		world.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_ALL);
		world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		return true;
	}

	/** 按客户端请求的类型直接放置:水源 / 源岩浆。 */
	private static void placeRequested(ServerPlayer player, ServerLevel world, BlockPos pos, int kind) {
		switch (kind) {
			case PlaceWaterloggedPayload.KIND_WATER ->
					placeFluid(player, world, pos, Fluids.WATER, Blocks.WATER, Items.ICE);
			case PlaceWaterloggedPayload.KIND_LAVA ->
					placeFluid(player, world, pos, Fluids.LAVA, Blocks.LAVA, Items.MAGMA_BLOCK);
			default -> {
			}
		}
	}

	/** 放一格流体源:逐格校验(距离、位置可替换、不能已是同种源流体),并扣掉 1 个材料。 */
	private static void placeFluid(ServerPlayer player, ServerLevel world, BlockPos pos,
			Fluid fluid, Block block, Item cost) {
		if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
			return;
		}
		FluidState current = world.getFluidState(pos);
		if (!current.isEmpty() && current.isSource()) {
			return; // 已经是源流体
		}
		BlockState state = world.getBlockState(pos);
		// 流动流体格要允许被源覆盖:否则流体一旦流到投影里的源格,那格就永远补不上源。
		boolean flowingFluid = !current.isEmpty();
		if (!state.isAir() && !state.canBeReplaced() && !flowingFluid) {
			return;
		}
		if (!consumeOne(player, cost)) {
			return;
		}
		world.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
		world.scheduleTick(pos, fluid, fluid.getTickDelay(world));
	}

	private static void markPending(Map<UUID, Map<BlockPos, Long>> table, ServerPlayer player,
			List<BlockPos> positions, long now) {
		Map<BlockPos, Long> pending = table.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
		for (BlockPos pos : positions) {
			pending.put(pos, now);
		}
	}

	/** 清理过期的待处理记录。 */
	private static void prunePending(Map<UUID, Map<BlockPos, Long>> table, long now) {
		for (Map<BlockPos, Long> pending : table.values()) {
			pending.entrySet().removeIf(entry -> now - entry.getValue() > PENDING_TTL);
		}
	}

	/** 从背包(含快捷栏/副手)里扣掉 1 个指定物品;没有则返回 false。 */
	private static boolean consumeOne(ServerPlayer player, Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty() || !stack.is(item)) {
				continue;
			}
			stack.shrink(1);
			if (stack.isEmpty()) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
			player.getInventory().setChanged();
			player.containerMenu.broadcastChanges();
			return true;
		}
		return false;
	}
}

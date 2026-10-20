package carpet_hao_addition.easyplace;

import carpet_hao_addition.HaoDebug;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 完整 NBT 通道:客户端把投影里该位置的方块实体 NBT 发给服务端,放置时写入。
 * 覆盖协议值装不下的内容(标牌文字、命令方块指令等)。
 */
public final class BlockDataChannel {
	private static boolean payloadRegistered = false;
	private static boolean receiverRegistered = false;

	/** 服务端缓存的一条数据:该位置的完整方块实体 NBT(可 null)。 */
	public record PendingData(NbtCompound nbt) {
	}

	/** 服务端:玩家 UUID -> (位置 -> 数据)。 */
	private static final Map<UUID, Map<BlockPos, PendingData>> HAO_PENDING = new HashMap<>();

	/**
	 * 单个玩家最多能屯多少条待用 NBT。
	 * <p>
	 * 这张表只在 {@link #take} 消费与断线时清理，**没有 TTL**，而每条的 NBT 上限是 2MB
	 * ——客户端一直发不同坐标就能把它撑成无界。正常一次放置只会留 0~1 条。
	 */
	private static final int MAX_PENDING_PER_PLAYER = 256;

	private BlockDataChannel() {
	}

	/** 注册 payload 类型(幂等)。 */
	public static void registerPayloadType() {
		if (payloadRegistered) {
			return;
		}
		payloadRegistered = true;
		PayloadTypeRegistry.playC2S().register(BlockDataPayload.ID, BlockDataPayload.CODEC);
	}

	/** 注册服务端接收端(幂等)。 */
	public static void registerServerReceiver() {
		registerPayloadType();
		if (receiverRegistered) {
			return;
		}
		receiverRegistered = true;
		ServerPlayNetworking.registerGlobalReceiver(BlockDataPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			context.server().execute(() -> {
				if (HaoDebug.ENABLED) {
					HaoDebug.log("[hao-easyplace] [服务端] 收到包: 位置=" + payload.pos()
							+ " id=" + (payload.nbt() == null ? "null" : payload.nbt().getString("id")));
				}
				java.util.Map<BlockPos, PendingData> pending =
						HAO_PENDING.computeIfAbsent(player.getUuid(), ignored -> new HashMap<>());
				// 这个表只在 take() 消费和断线时清理,没有 TTL —— 客户端一直发**不同坐标**
				// 就能把它撑成无界（每个条目的 NBT 最大 2MB）。正常一次放置只会有 0~1 条，
				// 所以超限时直接丢新数据:代价是那一格不还原方块实体，比把服务端吃光好。
				if (pending.size() >= MAX_PENDING_PER_PLAYER && !pending.containsKey(payload.pos())) {
					if (HaoDebug.ENABLED) {
						HaoDebug.log("[hao-easyplace] [服务端] 待用 NBT 已达上限 "
								+ MAX_PENDING_PER_PLAYER + "，丢弃位置=" + payload.pos());
					}
					return;
				}
				pending.put(payload.pos(), new PendingData(payload.nbt()));
			});
		});
		// 断线时清掉这个玩家还没被消费的条目。
		// 否则玩家放了方块却没真正放置（取消、切物品、断线）时，这些完整 NBT 会一直
		// 堆在静态表里；堆得越多，后面的放置越容易在宽容命中那一步被别的条目干扰。
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> {
					if (handler.getPlayer() != null) {
						java.util.Map<BlockPos, PendingData> dropped =
								HAO_PENDING.remove(handler.getPlayer().getUuid());
						if (dropped != null && !dropped.isEmpty()) {
							if (HaoDebug.ENABLED) {
								HaoDebug.log("[hao-easyplace] [断线清理] 丢掉 " + dropped.size() + " 条待用 NBT");
							}
						}
					}
				});
	}

	/** 客户端:把该位置的完整 NBT 发给服务端(同内容不重复发)。 */
	@Environment(EnvType.CLIENT)
	public static void dispatchPacket(BlockPos pos, NbtCompound nbt) {
		registerPayloadType();
		// 不做节流:该函数只在真正放置时调用,节流会吞包导致文字错位。
		boolean canSend = net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(BlockDataPayload.ID);
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-easyplace] [客户端] 发包: 位置=" + pos + " canSend=" + canSend
					+ " id=" + (nbt == null ? "null" : nbt.getString("id")));
		}
		if (canSend) {
			net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
					new BlockDataPayload(pos, nbt));
		}
	}

	/**
	 * 服务端:取走该位置缓存的数据(取走即删)。
	 *
	 * <p>优先精确命中。落点可能因为点击面不同和投影坐标差一格，所以留了一条宽容路径 ——
	 * 但密集放置时缓存里会同时存着相邻好几个位置的数据，简单地"取最近的"很容易把
	 * **隔壁那一个**的方块实体数据写到这一格上（合成器锁定槽位、标牌文字都会串）。
	 * 因此宽容命中必须同时满足三条：</p>
	 *
	 * <ol>
	 *   <li><b>距离 ≤ 1</b> —— 再远就不是"落点偏移"能解释的了；</li>
	 *   <li><b>id 与这一格要放的方块相容</b> —— 相邻的另一种方块实体不会被误取；</li>
	 *   <li><b>候选唯一</b> —— 有多个同距离候选说明存在歧义，宁可不还原也不要串号。</li>
	 * </ol>
	 *
	 * @param state 这一格即将放置的方块状态，用于校验缓存条目的方块实体类型
	 */
	public static PendingData take(PlayerEntity player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return null;
		}
		Map<BlockPos, PendingData> byPos = HAO_PENDING.get(serverPlayer.getUuid());
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-easyplace] [take] 查找 pos=" + pos
					+ " 缓存=" + (byPos == null ? "null" : byPos.keySet()));
		}
		if (byPos == null) {
			return null;
		}
		PendingData nbt = byPos.remove(pos);
		if (nbt == null) {
			// 宽容命中：落点可能因点击面不同和投影坐标差一格。
			// 但**只在缓存里恰好剩一条**时才敢猜 —— 剩多条说明还有别的位置在等
			// 自己的数据，这时"取最近的一个"很可能把隔壁那条拿过来，于是合成器的
			// 锁定槽位、标牌的文字就串了。宁可这一处不还原，也不要串号。
			if (byPos.size() == 1) {
				Map.Entry<BlockPos, PendingData> only = byPos.entrySet().iterator().next();
				BlockPos key = only.getKey();
				int d = Math.abs(key.getX() - pos.getX())
						+ Math.abs(key.getY() - pos.getY())
						+ Math.abs(key.getZ() - pos.getZ());
				if (d <= 1 && hao$idMatches(only.getValue(), state)) {
					nbt = byPos.remove(key);
					if (HaoDebug.ENABLED) {
						HaoDebug.log("[hao-easyplace] [take] 宽容命中: 查找=" + pos
								+ " 实际=" + key + " 距离=" + d);
					}
				} else {
					if (HaoDebug.ENABLED) {
						HaoDebug.log("[hao-easyplace] [take] 宽容未命中: 查找=" + pos
								+ " 唯一缓存=" + key + " 距离=" + d + " id相容=" + hao$idMatches(only.getValue(), state));
					}
				}
			} else if (!byPos.isEmpty()) {
				if (HaoDebug.ENABLED) {
					HaoDebug.log("[hao-easyplace] [take] 宽容放弃: 查找=" + pos
							+ " 缓存里还有 " + byPos.size() + " 条,不猜,宁可不还原也不串号");
				}
			}
		}
		if (byPos.isEmpty()) {
			HAO_PENDING.remove(serverPlayer.getUuid());
		}
		return nbt;
	}

	/** 缓存条目的方块实体 id 是否与即将放置的方块相容。 */
	private static boolean hao$idMatches(PendingData data, BlockState state) {
		if (data == null || data.nbt() == null || state == null) {
			return true; // 没有 id 可校验时不做额外限制
		}
		String nbtId = data.nbt().getString("id").orElse("");
		if (nbtId.isEmpty()) {
			return true;
		}
		Identifier id = Identifier.tryParse(nbtId);
		if (id == null) {
			return false;
		}
		net.minecraft.block.entity.BlockEntityType<?> type =
				net.minecraft.registry.Registries.BLOCK_ENTITY_TYPE.get(id);
		return type != null && type.supports(state);
	}
}

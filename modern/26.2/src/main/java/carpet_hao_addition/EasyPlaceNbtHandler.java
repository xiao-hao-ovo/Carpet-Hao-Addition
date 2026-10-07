package carpet_hao_addition;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 完整 NBT 通道:客户端把投影里该位置的方块实体 NBT 发给服务端,放置时写入。
 * 覆盖协议值装不下的内容(标牌文字、命令方块指令等)。
 */
public final class EasyPlaceNbtHandler {
	private static boolean payloadRegistered = false;
	private static boolean receiverRegistered = false;

	/** 服务端缓存的一条数据:投影方块状态(仅珊瑚类会填) + 完整方块实体 NBT(可 null)。 */
	public record PendingData(CompoundTag stateNbt, CompoundTag nbt) {
	}

	/** 服务端:玩家 UUID -> (位置 -> 数据)。 */
	private static final Map<UUID, Map<BlockPos, PendingData>> PENDING = new HashMap<>();

	private EasyPlaceNbtHandler() {
	}

	/** 注册 payload 类型(幂等)。 */
	public static void registerPayloadType() {
		if (payloadRegistered) {
			return;
		}
		payloadRegistered = true;
		PayloadTypeRegistry.serverboundPlay().register(EasyPlaceNbtPayload.ID, EasyPlaceNbtPayload.CODEC);
	}

	/** 注册服务端接收端(幂等)。 */
	public static void registerServerReceiver() {
		registerPayloadType();
		if (receiverRegistered) {
			return;
		}
		receiverRegistered = true;
		ServerPlayNetworking.registerGlobalReceiver(EasyPlaceNbtPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				System.out.println("[hao-easyplace] [服务端] 收到包: 位置=" + payload.pos()
						+ " id=" + (payload.nbt() == null ? "null" : payload.nbt().getString("id").orElse("?"))
						+ " 带状态=" + (payload.stateNbt() != null));
				PENDING.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
						.put(payload.pos(), new PendingData(payload.stateNbt(), payload.nbt()));
			});
		});
	}

	/** 客户端:把该位置的完整 NBT 发给服务端(同内容不重复发)。 */
	@Environment(EnvType.CLIENT)
	public static void send(BlockPos pos, CompoundTag stateNbt, CompoundTag nbt) {
		registerPayloadType();
		// 不做节流:该函数只在真正放置时调用,节流会吞包导致文字错位。
		boolean canSend = net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(EasyPlaceNbtPayload.ID);
		System.out.println("[hao-easyplace] [客户端] 发包: 位置=" + pos + " canSend=" + canSend
				+ " 带状态=" + (stateNbt != null)
				+ " id=" + (nbt == null ? "null" : nbt.getString("id").orElse("?")));
		if (canSend) {
			net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
					new EasyPlaceNbtPayload(pos, stateNbt, nbt));
		}
	}

	/** 服务端:取走该位置缓存的数据(取走即删)。 */
	public static PendingData take(Player player, BlockPos pos) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return null;
		}
		Map<BlockPos, PendingData> byPos = PENDING.get(serverPlayer.getUUID());
		System.out.println("[hao-easyplace] [take] 查找 pos=" + pos
				+ " 缓存=" + (byPos == null ? "null" : byPos.keySet()));
		if (byPos == null) {
			return null;
		}
		PendingData nbt = byPos.remove(pos);
		if (nbt == null) {
			// 服务端落点可能比投影位置差 1 格,方向随点击面变,所以取曼哈顿距离 <= 2 的最近项。
			BlockPos bestKey = null;
			int bestDist = 3;
			for (BlockPos key : byPos.keySet()) {
				int d = Math.abs(key.getX() - pos.getX())
						+ Math.abs(key.getY() - pos.getY())
						+ Math.abs(key.getZ() - pos.getZ());
				if (d < bestDist) {
					bestDist = d;
					bestKey = key;
				}
			}
			if (bestKey != null) {
				nbt = byPos.remove(bestKey);
				System.out.println("[hao-easyplace] [take] 宽容命中: 查找=" + pos
						+ " 实际=" + bestKey + " 距离=" + bestDist);
			} else {
				System.out.println("[hao-easyplace] [take] 宽容未命中: 查找=" + pos
						+ " 缓存=" + byPos.keySet());
			}
		}
		if (byPos.isEmpty()) {
			PENDING.remove(serverPlayer.getUUID());
		}
		return nbt;
	}
}

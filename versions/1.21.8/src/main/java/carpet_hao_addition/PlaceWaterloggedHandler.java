package carpet_hao_addition;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * easyPlaceWaterlogged 的注册与结算。
 * <p>
 * 分工:客户端只表明「此刻正在轻松放置照投影施工」(见 WorldUtils_easyPlaceWaterloggedMixin),
 * <b>补水的位置与时机由服务端在方块真正落下时决定</b>(见 BlockItem_easyPlaceWaterloggedMixin)。
 * <p>
 * 这样就不需要任何"预测位置":客户端不必推算目标格(easy place 的目标位置在 Litematica 内部,
 * 射线命中位置在快速放置时还会过期),服务端直接拿放置事件给的位置。
 */
public final class PlaceWaterloggedHandler {
	/** 玩家 UUID → 最近一次收到"正在轻松放置"标记的游戏刻。 */
	private static final Map<UUID, Long> ACTIVE = new HashMap<>();
	/** 标记的有效时长(游戏刻)。轻松放置每次操作都会刷新。 */
	private static final long ACTIVE_TTL = 10L;

	private static boolean payloadRegistered;
	private static boolean receiverRegistered;

	private PlaceWaterloggedHandler() {
	}

	/** 注册 C2S payload 类型。客户端与服务端都要调用(幂等)。 */
	public static void registerPayloadType() {
		if (payloadRegistered) {
			return;
		}
		payloadRegistered = true;
		PayloadTypeRegistry.playC2S().register(PlaceWaterloggedPayload.ID, PlaceWaterloggedPayload.CODEC);
	}

	/** 注册服务端接收端(仅服务端调用,幂等)。 */
	public static void registerServerReceiver() {
		registerPayloadType();
		if (receiverRegistered) {
			return;
		}
		receiverRegistered = true;
		ServerPlayNetworking.registerGlobalReceiver(PlaceWaterloggedPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			context.server().execute(() -> {
				if (!EasyPlaceWaterloggedSettings.isEnabled() || !payload.active()) {
					ACTIVE.remove(player.getUuid());
					return;
				}
				ServerWorld world = player.getWorld() instanceof ServerWorld sw ? sw : null;
				if (world == null) {
					return;
				}
				ACTIVE.put(player.getUuid(), world.getTime());
				// 放水源:逐格校验后放水并扣冰。
				for (BlockPos waterPos : payload.waterPositions()) {
					hao$placeSourceWater(player, world, waterPos);
				}
			});
		});
	}

	/**
	 * 方块落下后调用:若该玩家正在轻松放置、方块可含水却还没含水、背包里有冰,
	 * 就给方块补上含水并扣掉 1 个冰。
	 */
	public static void onBlockPlaced(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState placed) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		if (!isActive(player, world.getTime())) {
			return;
		}
		if (placed.isAir() || !placed.contains(Properties.WATERLOGGED)
				|| Boolean.TRUE.equals(placed.get(Properties.WATERLOGGED))) {
			return;
		}
		// 位置本来就有水的话,Litematica 放下时方块自然就含水了,不会走到这里。
		if (!consumeOneIce(player)) {
			return;
		}
		world.setBlockState(pos, placed.with(Properties.WATERLOGGED, true), Block.NOTIFY_ALL);
	}

	/** 在指定位置放一格水源(逐格校验:距离、位置可替换、不能已有水),并扣 1 个冰。 */
	private static void hao$placeSourceWater(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		// 距离校验,避免被用来远程放水。
		if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
			return;
		}
		if (!world.getFluidState(pos).isEmpty()) {
			// 已经有水(或流动水)了,不必再放。
			return;
		}
		BlockState current = world.getBlockState(pos);
		if (!current.isAir() && !current.isReplaceable()) {
			return;
		}
		if (!consumeOneIce(player)) {
			return;
		}
		world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
	}

	private static boolean isActive(ServerPlayerEntity player, long now) {
		Long last = ACTIVE.get(player.getUuid());
		if (last == null) {
			return false;
		}
		if (now - last > ACTIVE_TTL) {
			ACTIVE.remove(player.getUuid());
			return false;
		}
		return true;
	}

	/** 从背包(含快捷栏/副手)里扣掉 1 个冰;没有冰返回 false。 */
	public static boolean consumeOneIce(ServerPlayerEntity player) {
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (stack.isEmpty() || !stack.isOf(Items.ICE)) {
				continue;
			}
			stack.decrement(1);
			if (stack.isEmpty()) {
				player.getInventory().setStack(slot, ItemStack.EMPTY);
			}
			player.getInventory().markDirty();
			player.currentScreenHandler.syncState();
			return true;
		}
		return false;
	}
}

package carpet_hao_addition;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * easyPlaceWaterlogged 的注册与结算。
 * <p>
 * 分工:客户端只表明「此刻正在轻松放置照投影施工」并附上需要放水源的位置,
 * <b>补水的位置与时机由服务端在方块真正落下时决定</b>(见 BlockItem_easyPlaceWaterloggedMixin)。
 */
public final class PlaceWaterloggedHandler {
	/** 玩家 UUID → 最近一次收到"正在轻松放置"标记的游戏刻。 */
	private static final Map<UUID, Long> ACTIVE = new HashMap<>();
	/** 标记的有效时长(游戏刻)。 */
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
		PayloadTypeRegistry.serverboundPlay().register(PlaceWaterloggedPayload.ID, PlaceWaterloggedPayload.CODEC);
	}

	/** 注册服务端接收端(仅服务端调用,幂等)。 */
	public static void registerServerReceiver() {
		registerPayloadType();
		if (receiverRegistered) {
			return;
		}
		receiverRegistered = true;
		ServerPlayNetworking.registerGlobalReceiver(PlaceWaterloggedPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				if (!EasyPlaceWaterloggedSettings.isEnabled() || !payload.active()) {
					ACTIVE.remove(player.getUUID());
					return;
				}
				ServerLevel world = player.level();
				ACTIVE.put(player.getUUID(), world.getGameTime());
				for (BlockPos waterPos : payload.waterPositions()) {
					placeSourceWater(player, world, waterPos);
				}
			});
		});
	}

	/** 在指定位置放一格水源,并扣 1 个冰。 */
	private static void placeSourceWater(ServerPlayer player, ServerLevel world, BlockPos pos) {
		if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
			return;
		}
		if (!world.getFluidState(pos).isEmpty()) {
			return;
		}
		BlockState current = world.getBlockState(pos);
		if (!current.isAir() && !current.canBeReplaced()) {
			return;
		}
		if (!consumeOneIce(player)) {
			return;
		}
		world.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
	}

	/**
	 * 方块落下后调用:若该玩家正在轻松放置、方块可含水却还没含水、背包里有冰,
	 * 就给方块补上含水并扣掉 1 个冰。
	 */
	public static void onBlockPlaced(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState placed) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		Long last = ACTIVE.get(player.getUUID());
		if (last == null || world.getGameTime() - last > ACTIVE_TTL) {
			ACTIVE.remove(player.getUUID());
			return;
		}
		if (placed.isAir() || !placed.hasProperty(BlockStateProperties.WATERLOGGED)
				|| Boolean.TRUE.equals(placed.getValue(BlockStateProperties.WATERLOGGED))) {
			return;
		}
		if (!consumeOneIce(player)) {
			return;
		}
		world.setBlock(pos, placed.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_ALL);
	}

	/** 从背包(含快捷栏/副手)里扣掉 1 个冰;没有冰返回 false。 */
	public static boolean consumeOneIce(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty() || !stack.is(Items.ICE)) {
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

package carpet_hao_addition;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * easyPlaceWaterlogged 的自定义包。两种用途合并在一个包里:
 * <ul>
 *   <li>{@code active} —— 客户端表明「此刻正在轻松放置照投影施工」,服务端据此决定
 *       是否给刚放下的含水方块补水(位置由服务端从放置事件直接获得);</li>
 *   <li>{@code waterPositions} —— 投影里是<b>水源</b>的位置。水不能作为物品放置,原版与
 *       Litematica 都不会处理这些格子,所以由客户端把需要放水的坐标报给服务端。</li>
 * </ul>
 */
public record PlaceWaterloggedPayload(boolean active, List<BlockPos> waterPositions) implements CustomPayload {
	public static final CustomPayload.Id<PlaceWaterloggedPayload> ID =
			new CustomPayload.Id<>(Identifier.of(CarpetHaoAdditionExtension.MOD_ID, "easy_place_active"));

	public static final PacketCodec<RegistryByteBuf, PlaceWaterloggedPayload> CODEC =
			PacketCodec.of(
					(payload, buf) -> {
						buf.writeBoolean(payload.active());
						buf.writeVarInt(payload.waterPositions().size());
						for (BlockPos pos : payload.waterPositions()) {
							buf.writeBlockPos(pos);
						}
					},
					buf -> {
						boolean active = buf.readBoolean();
						int size = buf.readVarInt();
						List<BlockPos> positions = new java.util.ArrayList<>(size);
						for (int i = 0; i < size; i++) {
							positions.add(buf.readBlockPos());
						}
						return new PlaceWaterloggedPayload(active, positions);
					});

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

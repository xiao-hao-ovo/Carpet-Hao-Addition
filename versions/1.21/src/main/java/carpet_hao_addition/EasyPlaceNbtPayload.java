package carpet_hao_addition;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * 完整 NBT 通道的包:传投影里该位置的方块实体 NBT(协议值装不下标牌文字等)。
 * {@code stateNbt} 是可选的投影方块状态,目前只有珊瑚类会填,用于服务端强制设置。
 */
public record EasyPlaceNbtPayload(BlockPos pos, NbtCompound stateNbt, NbtCompound nbt) implements CustomPayload {
	public static final CustomPayload.Id<EasyPlaceNbtPayload> ID =
			new CustomPayload.Id<>(Identifier.of(CarpetHaoAdditionExtension.MOD_ID, "easy_place_nbt"));

	public static final PacketCodec<RegistryByteBuf, EasyPlaceNbtPayload> CODEC =
			PacketCodec.of(
					(payload, buf) -> {
						buf.writeBlockPos(payload.pos());
						buf.writeNbt(payload.stateNbt());
						buf.writeNbt(payload.nbt());
					},
					buf -> new EasyPlaceNbtPayload(buf.readBlockPos(), buf.readNbt(), buf.readNbt()));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

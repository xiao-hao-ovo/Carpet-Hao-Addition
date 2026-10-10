package carpet_hao_addition;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 完整 NBT 通道的包:传投影里该位置的方块实体 NBT(协议值装不下标牌文字等)。
 */
public record BlockDataPayload(BlockPos pos, CompoundTag nbt) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BlockDataPayload> ID =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CarpetHaoAdditionExtension.HAO_MOD_ID, "easy_place_nbt"));

	public static final StreamCodec<RegistryFriendlyByteBuf, BlockDataPayload> CODEC =
			StreamCodec.of(
					(buf, payload) -> {
						buf.writeBlockPos(payload.pos());
												buf.writeNbt(payload.nbt());
					},
					buf -> new BlockDataPayload(buf.readBlockPos(), buf.readNbt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}

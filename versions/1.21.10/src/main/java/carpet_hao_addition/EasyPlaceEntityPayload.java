package carpet_hao_addition;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * easyPlaceEntity 的自定义包:客户端告诉服务端「投影里这一格有这些实体,请按精确位置放出来」。
 * <p>
 * 用列表而不是单个实体,是因为投影里**同一格可能叠着多个实体**(如矿车 + 船):
 * 客户端把该格所有"还没有放过的"实体一起发过来,服务端逐条确认并生成,一次操作就能摆全。
 * <p>
 * 坐标是**投影里记录的精确世界坐标**(保留小数),朝向也是投影里的,所以放出来与投影完全对齐。
 */
public record EasyPlaceEntityPayload(List<EntitySpawn> spawns, int count) implements CustomPayload {
	public static final CustomPayload.Id<EasyPlaceEntityPayload> ID =
			new CustomPayload.Id<>(Identifier.of(CarpetHaoAdditionExtension.MOD_ID, "easy_place_entity"));

	public static final PacketCodec<RegistryByteBuf, EasyPlaceEntityPayload> CODEC =
			PacketCodec.of(
					(payload, buf) -> {
						buf.writeVarInt(payload.spawns().size());
						for (EntitySpawn spawn : payload.spawns()) {
							EntitySpawn.CODEC.encode(buf, spawn);
						}
						buf.writeVarInt(payload.count());
					},
					buf -> {
						int size = buf.readVarInt();
						List<EntitySpawn> spawns = new ArrayList<>(size);
						for (int i = 0; i < size; i++) {
							spawns.add(EntitySpawn.CODEC.decode(buf));
						}
						return new EasyPlaceEntityPayload(spawns, buf.readVarInt());
					});

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

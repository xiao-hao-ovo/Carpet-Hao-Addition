package carpet_hao_addition;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

/**
 * 一条"要放置的实体":类型 + **投影里的精确世界坐标** + 朝向 + **实体 NBT**。
 * <p>
 * NBT 是从投影里原样取出的(已把 {@code Pos} / {@code Rotation} 改写成世界坐标与变换后的朝向),
 * 服务端用它还原实体 —— 这样连**乘客**一起还原:例如投影里"矿车镶在船里"其实是
 * 船带着矿车乘客(同一个 NBT 的 {@code Passengers}),只按类型创建的话乘客就丢了。
 * <p>
 * {@code key} 是**投影里那个实体的身份**({@code 子区域#序号})。去重只能靠它:
 * 投影里常常有几个实体坐标完全重合(例如两辆叠在一起的漏斗矿车,局部坐标与 NBT 都一样、
 * 只有 UUID 不同),按坐标根本分不出"这是第二个"。
 */
public record EntitySpawn(Identifier type, String key, double x, double y, double z, float yaw,
		float pitch, NbtCompound nbt) {
	public static final PacketCodec<RegistryByteBuf, EntitySpawn> CODEC =
			PacketCodec.of(
					(spawn, buf) -> {
						buf.writeIdentifier(spawn.type());
						buf.writeString(spawn.key(), 256);
						buf.writeDouble(spawn.x());
						buf.writeDouble(spawn.y());
						buf.writeDouble(spawn.z());
						buf.writeFloat(spawn.yaw());
						buf.writeFloat(spawn.pitch());
						buf.writeNbt(spawn.nbt());
					},
					buf -> new EntitySpawn(
							buf.readIdentifier(),
							buf.readString(256),
							buf.readDouble(),
							buf.readDouble(),
							buf.readDouble(),
							buf.readFloat(),
							buf.readFloat(),
							buf.readNbt()));
}

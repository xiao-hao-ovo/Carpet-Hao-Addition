package carpet_hao_addition;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * easyPlaceWaterlogged 的自定义包。三样信息合并在一个包里:
 * <ul>
 *   <li>{@code active} —— 客户端表明「此刻正在轻松放置照投影施工」,服务端据此决定
 *       是否给刚放下的含水方块补水(位置由服务端从放置事件直接获得);</li>
 *   <li>{@code kind} —— 本次请求要在 {@code positions} 上放什么({@link #KIND_WATER} /
 *       {@link #KIND_LAVA} / {@link #KIND_LAVA_CAULDRON});{@link #KIND_NONE} 表示仅活跃标记;</li>
 *   <li>{@code positions} —— 需要放置的坐标(一次操作最多一格)。</li>
 * </ul>
 * 水、岩浆、装岩浆的炼药锅都没有对应的可放置物品(原版与 Litematica 都不会放它们),所以由客户端
 * 把需要补的坐标与类型报给服务端,服务端按类型放置并扣掉材料(水→冰,岩浆→岩浆块,装岩浆的炼药锅→炼药锅)。
 */
public record PlaceWaterloggedPayload(boolean active, int kind, List<BlockPos> positions) implements CustomPayload {
	/** 仅活跃标记,不放置任何东西。 */
	public static final int KIND_NONE = 0;
	/** 放一格水源(投影里是水源或气泡柱)。 */
	public static final int KIND_WATER = 1;
	/** 放一格源岩浆(投影里是岩浆)。 */
	public static final int KIND_LAVA = 2;
	/** 标记"待灌岩浆的炼药锅格"(投影里是装岩浆的炼药锅):炼药锅正常放置,服务端在其落下后灌岩浆。 */
	public static final int KIND_LAVA_CAULDRON = 3;
	/** 只是一个"待补水"标记:{@code positions} 里的格子随后放上方块时要补含水(本身不放置任何东西)。 */
	public static final int KIND_WATERLOG = 4;

	public static final CustomPayload.Id<PlaceWaterloggedPayload> ID =
			new CustomPayload.Id<>(Identifier.of(CarpetHaoAdditionExtension.MOD_ID, "easy_place_active"));

	public static final PacketCodec<RegistryByteBuf, PlaceWaterloggedPayload> CODEC =
			PacketCodec.of(
					(payload, buf) -> {
						buf.writeBoolean(payload.active());
						buf.writeVarInt(payload.kind());
						buf.writeVarInt(payload.positions().size());
						for (BlockPos pos : payload.positions()) {
							buf.writeBlockPos(pos);
						}
					},
					buf -> {
						boolean active = buf.readBoolean();
						int kind = buf.readVarInt();
						int size = buf.readVarInt();
						List<BlockPos> positions = new java.util.ArrayList<>(size);
						for (int i = 0; i < size; i++) {
							positions.add(buf.readBlockPos());
						}
						return new PlaceWaterloggedPayload(active, kind, positions);
					});

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

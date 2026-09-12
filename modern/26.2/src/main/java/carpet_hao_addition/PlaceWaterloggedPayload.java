package carpet_hao_addition;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * easyPlaceWaterlogged 的自定义包。三样信息合并在一个包里:
 * <ul>
 *   <li>{@code active} —— 客户端表明「此刻正在轻松放置照投影施工」;</li>
 *   <li>{@code kind} —— 本次请求要在 {@code positions} 上放什么({@link #KIND_WATER} /
 *       {@link #KIND_LAVA} / {@link #KIND_LAVA_CAULDRON} / {@link #KIND_WATERLOG});{@link #KIND_NONE} 表示无;</li>
 *   <li>{@code positions} —— 需要处理的坐标(放流体/炼药锅时一格;待补水时可多格)。</li>
 * </ul>
 * 水、岩浆、装岩浆的炼药锅都没有"直接可放"的物品形式,原版与 Litematica 都不会正确落下,所以由客户端
 * 把它们报给服务端;服务端按类型放置并扣材料(水→冰,岩浆→岩浆块,装岩浆的炼药锅→炼药锅+岩浆块)。
 */
public record PlaceWaterloggedPayload(boolean active, int kind, List<BlockPos> positions) implements CustomPacketPayload {
	/** 仅活跃标记,不放置任何东西。 */
	public static final int KIND_NONE = 0;
	/** 放一格水源(投影里是水源或气泡柱)。 */
	public static final int KIND_WATER = 1;
	/** 放一格源岩浆(投影里是岩浆)。 */
	public static final int KIND_LAVA = 2;
	/** 标记"待灌岩浆的炼药锅格":炼药锅正常放置,服务端在其落下后灌岩浆。 */
	public static final int KIND_LAVA_CAULDRON = 3;
	/** 待补水格:{@code positions} 里的格子随后放上方块时要补含水。 */
	public static final int KIND_WATERLOG = 4;

	public static final CustomPacketPayload.Type<PlaceWaterloggedPayload> ID =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CarpetHaoAdditionExtension.MOD_ID, "easy_place_active"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlaceWaterloggedPayload> CODEC =
			StreamCodec.of(
					(buf, payload) -> {
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
						List<BlockPos> positions = new ArrayList<>(size);
						for (int i = 0; i < size; i++) {
							positions.add(buf.readBlockPos());
						}
						return new PlaceWaterloggedPayload(active, kind, positions);
					});

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}

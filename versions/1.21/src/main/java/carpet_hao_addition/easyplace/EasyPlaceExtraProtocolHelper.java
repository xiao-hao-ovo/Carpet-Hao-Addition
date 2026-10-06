package carpet_hao_addition.easyplace;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** 协议值编解码:把方块状态塞进点击坐标 z 里。编码 {@code z + ((v<<1)+2)},解码 {@code ((int)z - 2) >>> 1};相对坐标 ≥ 2 即协议坐标。 */
public final class EasyPlaceExtraProtocolHelper {
	/** 含水标记位(本项目由 easyPlaceWaterlogged 规则负责,这里保持关闭)。 */
	public static final int WATERLOGGED_BIT = 1 << 16;

	/** 协议坐标的起始阈值:相对坐标 ≥ 2.0 视为协议值。 */
	private static final double PROTOCOL_THRESHOLD = 2.0D;

	private EasyPlaceExtraProtocolHelper() {
	}

	/** 该相对坐标是不是协议值。 */
	public static boolean isProtocol(double relativeHitDim) {
		return relativeHitDim >= PROTOCOL_THRESHOLD;
	}

	/** 是不是"扩展协议"值(第 3 位被置起)。 */
	public static boolean isExtraProtocol(int protocolValue) {
		return (protocolValue & 0b0000_1000) == 0b0000_1000;
	}

	/** 原始点击位置(等价于 Mojang 的 getClickedPos)。 */
	public static BlockPos getClickedPos(ItemPlacementContext context) {
		if (context.canReplaceExisting()) {
			return context.getBlockPos();
		}
		return context.getBlockPos().offset(context.getSide().getOpposite());
	}

	/** 点击位置在方块内的相对 X。 */
	public static double getRelativeHitX(Vec3d hitPos, BlockPos blockPos) {
		return hitPos.x - (double) blockPos.getX();
	}

	/** 点击位置在方块内的相对 Z。 */
	public static double getRelativeHitZ(Vec3d hitPos, BlockPos blockPos) {
		return hitPos.z - (double) blockPos.getZ();
	}

	/** 从小数位解出协议值。 */
	public static int decodeProtocolValueFromHitDim(double relativeHitDim) {
		return ((int) relativeHitDim - 2) >>> 1;
	}

	/** 把协议值编码进小数位。 */
	public static double encodeProtocolValueToHitDim(double relativeHitDim, int protocolValue) {
		return relativeHitDim + (double) ((protocolValue << 1) + 2);
	}

	/** 扩展协议值 → 原始协议值。 */
	public static int extraProtocolValueToRawProtocolValue(int protocolValue) {
		return ((protocolValue & 0b1111_0000) >>> 1) | (protocolValue & 0b0000_0111);
	}

	/** 原始协议值 → 扩展协议值。 */
	public static int rawProtocolValueToExtraProtocolValue(int protocolValue) {
		return ((protocolValue & 0b0111_1000) << 1) | (protocolValue & 0b0000_0111) | 0b0000_1000;
	}

	/** 置起扩展位。 */
	public static int addExtraProtocolBit(int protocolValue) {
		return protocolValue | 0b0000_1000;
	}

	/** 清掉扩展位。 */
	public static int removeExtraProtocolBit(int protocolValue) {
		return protocolValue & ~0b0000_1000;
	}

	/** 把协议值编码进 hitVec 的 Z 分量(只改 z,不动 x/y)。 */
	public static Vec3d encodeProtocolValueToHitVecZ(int protocolAdditionValue, Vec3d hitVec) {
		return new Vec3d(hitVec.x, hitVec.y, encodeProtocolValueToHitDim(hitVec.z, protocolAdditionValue));
	}

	/** 取状态里第一个方向类型的属性(没有就返回 null)。 */
	public static Property<Direction> getFirstDirectionProperty(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (Direction.class.isAssignableFrom(property.getType())) {
				@SuppressWarnings("unchecked")
				Property<Direction> directionProperty = (Property<Direction>) property;
				return directionProperty;
			}
		}
		return null;
	}

	/** 含水交给 easyPlaceWaterlogged 规则,这里恒返回 0。 */
	public static int waterloggedBit(BlockState state) {
		return 0;
	}
}

package carpet_hao_addition.easyplace;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * 协议位的「藏」与「取」—— 把要传的状态位藏进点击坐标 z 的小数部分。
 *
 * <p>约定：相对 z 坐标 {@code >= 2.0} 就表示这一击带着协议值，值等于
 * {@code ((int) z - 2) >>> 1}。于是 z 的小数位成了一个免费信道：既不占用任何方块状态属性，
 * 也不影响投影原本按坐标挑放置面的逻辑。
 *
 * <p>含水不走这条通道 —— 那是 {@code easyPlaceWaterlogged} 规则的事，
 * 由 {@code WaterloggedFillHandler} 经它自己的载荷传递。
 */
public final class PlacementBitTools {

	/** 相对 z 坐标达到这个值就认为这次点击带着协议值。 */
	private static final double PROTOCOL_THRESHOLD = 2.0D;

	private PlacementBitTools() {
	}

	/** 这一击是否带着协议值。 */
	public static boolean isProtocolHit(double zFraction) {
		return zFraction >= PROTOCOL_THRESHOLD;
	}

	/** 这一击实际瞄准的方块位置。 */
	public static BlockPos clickedPos(ItemPlacementContext context) {
		if (context.canReplaceExisting()) {
			return context.getBlockPos();
		}
		return context.getBlockPos().offset(context.getSide().getOpposite());
	}

	/** 点击点相对方块原点的 x 偏移。 */
	public static double hitOffsetX(Vec3d hitPos, BlockPos pos) {
		return hitPos.x - (double) pos.getX();
	}

	/** 点击点相对方块原点的 z 偏移 —— 协议值就藏在它的小数位上。 */
	public static double hitOffsetZ(Vec3d hitPos, BlockPos pos) {
		return hitPos.z - (double) pos.getZ();
	}

	/** 从 z 的小数位读回协议值。 */
	public static int readBits(double zFraction) {
		return ((int) zFraction - 2) >>> 1;
	}

	/** 把协议值写进 z 的小数位（只动 z，x / y 保持原样）。 */
	public static Vec3d bitsToHitVec(int bits, Vec3d hitVec) {
		return new Vec3d(hitVec.x, hitVec.y, writeBits(hitVec.z, bits));
	}

	private static double writeBits(double zFraction, int bits) {
		return zFraction + (double) ((bits << 1) + 2);
	}
}

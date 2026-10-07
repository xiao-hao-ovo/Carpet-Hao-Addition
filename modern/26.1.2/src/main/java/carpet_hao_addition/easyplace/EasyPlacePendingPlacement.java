package carpet_hao_addition.easyplace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端跨 mixin 共享的「本次投影放置」缓存:记录本次投影放置的位置/状态/编码后的 hitVec,
 * 供 {@code doEasyPlaceAction} 返回后补放多阶段方块、以及修正珊瑚墙扇的点击面。
 * <p>
 * 1.21.8 的 litematica 把协议钩子都放在 {@code WorldUtils} 上,缓存可以放同一个 mixin 的
 * {@code @Unique} 字段里;26.x 起 litematica 把 {@code applyCarpetProtocolHitVec} /
 * {@code applyPlacementProtocolV3} 移到了 {@code EasyPlaceUtils},两个 mixin 要共享这份缓存,
 * 所以改放在这里。
 */
public final class EasyPlacePendingPlacement {
	/** 本次投影放置的目标方块坐标。 */
	public static BlockPos pos = null;
	/** 投影里该方块应该是什么状态。 */
	public static BlockState schematic = null;
	/** 我们编码后的 hitVec(交给原版放置流程的命中点)。 */
	public static Vec3 hitVec = null;

	/** 用完即清,避免影响下一次放置。 */
	public static void clear() {
		pos = null;
		schematic = null;
		hitVec = null;
	}

	private EasyPlacePendingPlacement() {
	}
}

package carpet_hao_addition.easyplace;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

/**
 * 「这一次放置动作发生在哪」的临时状态。
 *
 * <p>投影的轻松放置是「先模拟一次放置拿到状态、再真正放」两步，期间要靠这几个静态字段
 * 把目标位置/目标方块/额外标记从编码阶段带到放置阶段。它们随放置动作成对地开与关，
 * 所以集中放在这里，不跟协议位编解码混在一起。
 */
public final class PlacementWindow {

	private static boolean open = false;
	private static long flags = 0;
	private static BlockPos targetPos = BlockPos.ZERO;
	private static Block targetBlock = Blocks.AIR;

	private PlacementWindow() {
	}

	/** 是否正处在一次投影放置动作之中。 */
	public static boolean isOpen() {
		return open;
	}

	public static void setOpen(boolean value) {
		open = value;
	}

	/** 覆盖全部标记位。 */
	public static void setFlags(long value) {
		flags = value;
	}

	/** 置起某个标记位（如「别自动改铁轨形状」）。 */
	public static void raiseFlag(long flag) {
		flags |= flag;
	}

	public static boolean hasFlag(long flag) {
		return (flags & flag) == flag;
	}

	public static BlockPos targetPos() {
		return targetPos;
	}

	public static void setTargetPos(BlockPos pos) {
		targetPos = pos;
	}

	public static Block targetBlock() {
		return targetBlock;
	}

	public static void setTargetBlock(Block block) {
		targetBlock = block;
	}
}

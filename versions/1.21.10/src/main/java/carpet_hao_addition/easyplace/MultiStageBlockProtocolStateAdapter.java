package carpet_hao_addition.easyplace;

import net.minecraft.block.BlockState;

/** 多阶段方块协议适配器:给"需要点好几次才叠得出来"的方块用(雪层/海泡菜/蜡烛/藤蔓等)。 */
public interface MultiStageBlockProtocolStateAdapter {
	/** 决定最多要点几次。 */
	void hao$setLoopCount(LoopContext ctx);

	/** 第 {@code ctx.loopIndex} 次点击要用的协议值。 */
	int hao$toProtocolValueLoop(LoopContext ctx);

	/** 循环上下文:在多次点击之间传递状态。 */
	class LoopContext {
		/** 投影里要求的状态。 */
		public BlockState stateSchematic = null;
		/** 客户端世界里当前的状态(每次点击后会更新)。 */
		public BlockState stateClient = null;
		/** 总共要点几次。 */
		public int loopCount = 0;
		/** 当前是第几次(从 0 开始)。 */
		public int loopIndex = 0;
		/** 适配器自用的暂存数据。 */
		public Object data;
	}
}

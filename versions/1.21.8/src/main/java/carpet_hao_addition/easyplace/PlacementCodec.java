package carpet_hao_addition.easyplace;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;

/**
 * 一个「状态编解码器」：负责把方块状态里的某一类信息与协议位互相转换。
 *
 * <p>与旧实现「每个方块写一个类」不同，这里以<b>状态类型</b>为单位拆分：
 * 所有水平朝向方块共用同一个编解码器，所有开关类方块共用另一个。
 * 一个方块如果需要还原多种状态，就在 {@link PlacementRules} 里挂多个编解码器。
 *
 * <p>约定：{@link #encode} 与 {@link #decode} 必须严格互逆 —— 协议值经服务端解开后，
 * 还原出的状态要与投影中的一致。任何一侧改了位段，另一侧必须同步。
 */
public interface PlacementCodec {

	/**
	 * 把状态编码进协议位。
	 *
	 * @param state 要编码的方块状态
	 * @param bits  已累积的协议位（可能已被同一方块的前几个编解码器写过）
	 * @return 新的协议位
	 */
	int encode(BlockState state, int bits);

	/**
	 * 从协议位还原状态。
	 *
	 * @param bits    解出的协议位
	 * @param state   服务端按原版规则算出的基础状态
	 * @param context 放置上下文（个别编解码器需要查世界或点击位置）
	 * @return 还原后的状态；返回 {@code null} 表示本编解码器不改变状态
	 */
	BlockState decode(int bits, BlockState state, ItemPlacementContext context);

	/**
	 * 该方块是否需要「连续右键点几次才叠到目标」。
	 * 典型是雪层、海泡菜这类一次只加一层、以及藤蔓这类一次只长一个方向的方块。
	 */
	default boolean multiStage() {
		return false;
	}

	/**
	 * 距离投影目标还差几次点击。返回 0 表示已达目标、不需要补点。
	 *
	 * @param schematic 投影要求的状态
	 * @param client    客户端世界里当前的状态
	 */
	default int missingSteps(BlockState schematic, BlockState client) {
		return 0;
	}

	/**
	 * 第 {@code index} 次点击要用的协议值（{@code index} 从 0 开始）。
	 *
	 * @param schematic 投影要求的状态
	 * @param client    客户端世界里当前的状态
	 */
	default int stepBits(BlockState schematic, BlockState client, int index) {
		return 0;
	}
}

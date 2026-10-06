package carpet_hao_addition.easyplace;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;

/** 方块协议状态适配器:把方块状态与"协议值"互相转换。 */
public interface BlockProtocolStateAdapter {
	/**
	 * 把 {@code fromState} 里的附加状态编码成协议值(叠加到 {@code protocolValue} 上)。
	 *
	 * @param protocolValue 已有的协议值
	 * @param fromState     要编码的方块状态
	 * @return 新的协议值
	 */
	int hao$toProtocolValue(int protocolValue, BlockState fromState);

	/**
	 * 从协议值还原方块状态。
	 *
	 * @param extraProtocolValue 解出的协议值
	 * @param fromState          基础状态
	 * @param context            放置上下文
	 * @return 还原后的状态;返回 {@code null} 表示不做改动
	 */
	BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context);

	/** 协议类型:叠加({@link ProtocolType#ADDED})还是整体替换({@link ProtocolType#REPLACE})。 */
	ProtocolType hao$getProtocolType();

	enum ProtocolType {
		/** 在原有状态上叠加属性。 */
		ADDED,
		/** 用协议值整体决定状态。 */
		REPLACE
	}
}

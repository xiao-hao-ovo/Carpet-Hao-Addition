package carpet_hao_addition.easyplace;

import net.minecraft.item.ItemStack;

/** 物品栈协议数据适配器:额外把"手上拿的那个物品"的信息编码进协议值。 */
public interface ItemStackProtocolDataAdapter {
	/** 把物品栈上的附加信息编码成协议值。 */
	int hao$toProtocolValueAddition(ItemStack fromStack);

	/** 从协议值还原物品栈(用于放置时把状态带进去)。 */
	ItemStack hao$fromProtocolValueAddition(int extraProtocolValue, ItemStack fromStack);
}

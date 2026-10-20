package carpet_hao_addition.easyplace;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * 「物品数据」编解码器 —— 处理那些写不进方块状态的放置信息。
 *
 * <p>有一类东西不属于任何方块状态：告示牌的文字颜色与发光、合成器被扳手禁用的槽位、
 * 信标选中的效果。它们要么躺在手持物的 BlockEntityTag 里，要么挂在投影世界的方块实体上，
 * 所以另开一条位通道，与 {@link PlacementCodec} 负责的方块状态位互不重叠。
 *
 * <p>四个方法都给了「当作没这回事」的默认实现，实现者只覆盖自己关心的那一个：
 * 告示牌不关心方块实体，信标不关心手持物。
 */
public interface ItemDataCodec {

	/** 客户端瞄准投影时：把手持物上的数据编成协议位。 */
	default int encodeStack(ItemStack stack) {
		return 0;
	}

	/** 服务端真正放置时：把协议位写回手持物，让原版放置流程把数据带进方块实体。 */
	default ItemStack decodeStack(int bits, ItemStack stack) {
		return stack;
	}

	/** 客户端拿得到投影世界的方块实体时直接用方块实体编码 —— 比解析 NBT 更可靠。 */
	default int encodeBlockEntity(BlockEntity blockEntity) {
		return 0;
	}

	/** 只在拿得到投影文件里的方块实体 NBT、拿不到方块实体本体时使用。 */
	default int encodeNbt(NbtCompound tag) {
		return 0;
	}

	/**
	 * 协议位是否已经能**完整**表达这一处需要还原的方块实体数据。
	 *
	 * <p>能的话就不必再走「完整 NBT 通道」—— 那条通道是客户端按位置发包、服务端
	 * 按位置缓存在取，密集放置时相邻位置的数据容易互相串（合成器锁定槽位、
	 * 标牌文字都可能串到隔壁）。</p>
	 *
	 * <ul>
	 *   <li>合成器的 9 位禁用槽位装得下 → {@code true}</li>
	 *   <li>信标的两个效果 id 装得下 → {@code true}</li>
	 *   <li>告示牌的文字装不下 → 保持 {@code false}，仍然需要完整 NBT</li>
	 * </ul>
	 */
	default boolean protocolCoversAll() {
		return false;
	}
}

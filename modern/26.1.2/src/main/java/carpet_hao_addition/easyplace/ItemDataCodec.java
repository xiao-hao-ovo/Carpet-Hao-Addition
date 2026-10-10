package carpet_hao_addition.easyplace;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

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
	default int encodeNbt(CompoundTag tag) {
		return 0;
	}

	/** 把 NBT 包成物品的方块实体数据组件。 */
	static net.minecraft.world.item.component.TypedEntityData<net.minecraft.world.level.block.entity.BlockEntityType<?>> wrapBlockEntityData(CompoundTag tag) {
		net.minecraft.world.level.block.entity.BlockEntityType<?> type =
				net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
						net.minecraft.resources.Identifier.tryParse(tag.getString("id").orElse("")))
						.map(net.minecraft.core.Holder.Reference::value)
						.orElse(null);
		return net.minecraft.world.item.component.TypedEntityData.of(type, tag);
	}

	/** 从方块实体数据组件取回 NBT。 */
	static CompoundTag unwrapBlockEntityData(net.minecraft.world.item.component.TypedEntityData<?> data) {
		return data == null ? null : data.copyTagWithoutId();
	}
}

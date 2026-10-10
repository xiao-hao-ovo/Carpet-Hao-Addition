package carpet_hao_addition.easyplace;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * 合成器被禁用的槽位 —— 9 个槽各占一位，整体放在 bit4-12。
 *
 * <p>被扳手点过的槽位在投影里是"关掉"的，但那是方块实体状态而非方块状态，
 * 因此走物品数据通道：客户端把 9 位塞进协议值，服务端再写回手持物的
 * {@code disabled_slots}，原版放置时就会原样搬进方块实体。
 */
public final class CrafterItemData implements ItemDataCodec {

	private static final String DISABLED_SLOTS = "disabled_slots";
	private static final int SLOT_COUNT = 9;
	private static final int SHIFT = 4;
	private static final int MASK = 0b1_1111_1111;
	private static final String BLOCK_ENTITY_ID = "minecraft:crafter";
	private static final int[] NO_SLOTS = new int[0];

	@Override
	public int encodeStack(ItemStack stack) {
		NbtCompound tag = blockEntityTag(stack);
		if (tag == null) {
			return 0;
		}
		int disabled = 0;
		for (int slot : tag.getIntArray(DISABLED_SLOTS).orElse(NO_SLOTS)) {
			if (slot >= 0 && slot < SLOT_COUNT) {
				disabled |= 1 << slot;
			}
		}
		return (disabled & MASK) << SHIFT;
	}

	@Override
	public int encodeBlockEntity(BlockEntity blockEntity) {
		if (!(blockEntity instanceof CrafterBlockEntity crafter)) {
			return 0;
		}
		int disabled = 0;
		for (int slot = 0; slot < SLOT_COUNT; ++slot) {
			if (crafter.isSlotDisabled(slot)) {
				disabled |= 1 << slot;
			}
		}
		return (disabled & MASK) << SHIFT;
	}

	@Override
	public ItemStack decodeStack(int bits, ItemStack stack) {
		int disabled = (bits >>> SHIFT) & MASK;
		if (disabled == 0) {
			return stack;
		}
		NbtCompound tag = blockEntityTag(stack);
		if (tag != null && tag.contains(DISABLED_SLOTS)) {
			return stack;
		}
		if (tag == null) {
			tag = new NbtCompound();
		}

		int[] slots = new int[Integer.bitCount(disabled)];
		int next = 0;
		for (int slot = 0; slot < SLOT_COUNT; ++slot) {
			if ((disabled & (1 << slot)) != 0) {
				slots[next++] = slot;
			}
		}
		tag.putIntArray(DISABLED_SLOTS, slots);
		tag.putString("id", BLOCK_ENTITY_ID);

		ItemStack restored = stack.copy();
		restored.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(tag));
		return restored;
	}

	private static NbtCompound blockEntityTag(ItemStack stack) {
		NbtComponent data = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		return data == null ? null : data.copyNbt();
	}
}

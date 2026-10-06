package carpet_hao_addition.easyplace.adapter;


import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.block.enums.Orientation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.type.NbtComponent; //?MC >= 12005 && MC < 12110
//?< 1.20.5 ? import net.minecraft.nbt.NbtCompound;

public class CrafterBlockProtocolAdapter implements BlockProtocolStateAdapter, ItemStackProtocolDataAdapter {
    public static final CrafterBlockProtocolAdapter INSTANCE = new CrafterBlockProtocolAdapter();

    public CrafterBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int orientationOrdinal = fromState.get(Properties.ORIENTATION).ordinal();
        return protocolValue | ((orientationOrdinal + 1) & 0b0000_1111);
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int orientationOrdinal = Math.floorMod((extraProtocolValue & 0b0000_1111) - 1, 12);
        return fromState.with(Properties.ORIENTATION, Orientation.values()[orientationOrdinal]);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }

    @Override
    public int hao$toProtocolValueAddition(ItemStack fromStack) {
        NbtCompound tagBlockEntity = getBlockEntityTag(fromStack);
        if (tagBlockEntity == null) {
            return 0;
        }

        int[] disSlots = getDisabledSlots(tagBlockEntity);
        int bits = 0;
        int mask = 1;
        for (int slotIdx : disSlots) {
            if (slotIdx > -1 && slotIdx < 9) {
                bits |= (mask << slotIdx);
            }
        }
        return (bits & 0b0001_1111_1111) << 4;
    }

    @Override
    public ItemStack hao$fromProtocolValueAddition(int extraProtocolValue, ItemStack fromStack) {
        int slotBits = (extraProtocolValue >>> 4) & 0b0001_1111_1111;
        int disCount = Integer.bitCount(slotBits);
        if (disCount == 0) {
            return fromStack;
        }

        int[] disSlots = new int[disCount];
        int slotIdx = 0;
        int mask = 1;
        for (int i = 0; i < 9; ++i) {
            if ((slotBits & mask) == mask) {
                disSlots[slotIdx++] = i;
            }
            mask <<= 1;
        }

        
        NbtCompound tagBlockEntity = getBlockEntityTag(fromStack);
        if (tagBlockEntity != null && tagBlockEntity.contains("disabled_slots")) {
            return fromStack;
        }
        if (tagBlockEntity == null) {
            tagBlockEntity = new NbtCompound();
        }
        tagBlockEntity.putIntArray("disabled_slots", disSlots);
        return setBlockEntityTag(fromStack, tagBlockEntity);
    }

    

    private static NbtCompound getBlockEntityTag(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyNbt();
    }
    private static ItemStack setBlockEntityTag(ItemStack stack, NbtCompound tag) {
        ItemStack stackCopy = stack.copy();
        tag.putString("id", "minecraft:crafter");
        stackCopy.set(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA, net.minecraft.component.type.NbtComponent.of(tag));
        return stackCopy;
    }

    
    private static int[] getDisabledSlots(NbtCompound tag) {
        if (!tag.contains("disabled_slots")) {
            return new int[0];
    }
        return tag.getIntArray("disabled_slots");
    }
}

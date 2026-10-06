package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.registry.entry.RegistryEntry; //?>= 1.20.5
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.BlockState;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.type.NbtComponent; //?MC >= 12005 && MC < 12110
//?< 1.20.5 ? import net.minecraft.nbt.NbtCompound;

public class BeaconBlockProtocolAdapter implements BlockProtocolStateAdapter, ItemStackProtocolDataAdapter {
    public static final BeaconBlockProtocolAdapter INSTANCE = new BeaconBlockProtocolAdapter();

    public BeaconBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        return 0;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        return fromState;
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }

    @Override
    public int hao$toProtocolValueAddition(ItemStack fromStack) {
        return 0;
    }

    @Override
    public ItemStack hao$fromProtocolValueAddition(int extraProtocolValue, ItemStack fromStack) {
        int primaryId = (extraProtocolValue >>> 7) & 0x7F;
        int secondaryId = extraProtocolValue & 0x7F;
        if (primaryId == 0 && secondaryId == 0) {
            return fromStack;
        }

        ItemStack stackCopy = fromStack.copy();
        NbtCompound tag = getBlockEntityTag(stackCopy);
        if (tag == null) {
            tag = new NbtCompound();
        }
        if (tag.contains("primary_effect") || tag.contains("secondary_effect")) {
            return fromStack;
        }

        Registry<StatusEffect> registry = Registries.STATUS_EFFECT;
        if (primaryId != 0) {
            StatusEffect primary = registry.get(primaryId - 1);
            if (primary != null) {
                tag.putString("primary_effect", registry.getId(primary).toString());
            }
        }
        if (secondaryId != 0) {
            StatusEffect secondary = registry.get(secondaryId - 1);
            if (secondary != null) {
                tag.putString("secondary_effect", registry.getId(secondary).toString());
            }
        }
        return setBlockEntityTag(stackCopy, tag);
    }

    public static int encodeEffects(RegistryEntry<StatusEffect> primary, RegistryEntry<StatusEffect> secondary)
    {
        int p = 0;
        int s = 0;
        if (primary != null) {
            StatusEffect effect = primary.value();
            p = Registries.STATUS_EFFECT.getRawId(effect) + 1;
        }
        if (secondary != null) {
            StatusEffect effect = secondary.value();
            s = Registries.STATUS_EFFECT.getRawId(effect) + 1;
        }
        return (p << 7) | s;
    }


    private static NbtCompound getBlockEntityTag(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyNbt();
    }
    private static ItemStack setBlockEntityTag(ItemStack stack, NbtCompound tag) {
        ItemStack stackCopy = stack.copy();
        tag.putString("id", "minecraft:beacon");
        stackCopy.set(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA, net.minecraft.component.type.NbtComponent.of(tag));
        return stackCopy;
    }
}

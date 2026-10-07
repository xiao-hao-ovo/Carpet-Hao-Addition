package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityTypes;


public class BeaconBlockProtocolAdapter implements BlockProtocolStateAdapter, ItemStackProtocolDataAdapter {
    public static final BeaconBlockProtocolAdapter INSTANCE = new BeaconBlockProtocolAdapter();

    public BeaconBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        return 0;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        return fromState;
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }

    @Override
    public int hao$toProtocolValueAddition(ItemStack fromStack) {
        return 0;
    }

    @Override
    public @NotNull ItemStack hao$fromProtocolValueAddition(int extraProtocolValue, ItemStack fromStack) {
        int primaryId = (extraProtocolValue >>> 7) & 0x7F;
        int secondaryId = extraProtocolValue & 0x7F;
        if (primaryId == 0 && secondaryId == 0) {
            return fromStack;
        }

        ItemStack stackCopy = fromStack.copy();
        CompoundTag tag = getBlockEntityTag(stackCopy);
        if (tag == null) {
            tag = new CompoundTag();
        }
        if (tag.contains("primary_effect") || tag.contains("secondary_effect")) {
            return fromStack;
        }

        Registry<MobEffect> registry = BuiltInRegistries.MOB_EFFECT;
        if (primaryId != 0) {
            MobEffect primary = registry.byId(primaryId - 1);
            if (primary != null) {
                tag.putString("primary_effect", registry.getKey(primary).toString());
            }
        }
        if (secondaryId != 0) {
            MobEffect secondary = registry.byId(secondaryId - 1);
            if (secondary != null) {
                tag.putString("secondary_effect", registry.getKey(secondary).toString());
            }
        }
        return setBlockEntityTag(stackCopy, tag);
    }

    public static int encodeEffects(@Nullable Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary)
    {
        int p = 0;
        int s = 0;
        if (primary != null) {
            MobEffect effect = primary.value();
            p = BuiltInRegistries.MOB_EFFECT.getId(effect) + 1;
        }
        if (secondary != null) {
            MobEffect effect = secondary.value();
            s = BuiltInRegistries.MOB_EFFECT.getId(effect) + 1;
        }
        return (p << 7) | s;
    }

    private static @Nullable CompoundTag getBlockEntityTag(ItemStack stack) {
        TypedEntityData<?> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTagWithoutId();
    }
    private static ItemStack setBlockEntityTag(ItemStack stack, CompoundTag tag) {
    ItemStack stackCopy = stack.copy();
        stackCopy.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityTypes.BEACON, tag));
    return stackCopy;
    }
}


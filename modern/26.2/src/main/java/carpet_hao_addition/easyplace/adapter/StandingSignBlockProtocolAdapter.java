package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StandingSignBlockProtocolAdapter implements BlockProtocolStateAdapter, ItemStackProtocolDataAdapter {
    public static final StandingSignBlockProtocolAdapter INSTANCE = new StandingSignBlockProtocolAdapter();

    private static final int BIT_GLOWING = 0b10_0000;
    private static final int BIT_COLOR_MASK = 0b1111 << 6;
    private static final int BIT_COLOR_SHIFT = 6;
    private static final int BIT_BACK_GLOWING = 0b1000_0000_0000;
    private static final int BIT_BACK_COLOR_MASK = 0b1111 << 12;
    private static final int BIT_BACK_COLOR_SHIFT = 12;
    private static final int BIT_WAXED = 0b100_0000_0000;

    public StandingSignBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        if (fromState.getBlock() instanceof StandingSignBlock) {
            int rotation = fromState.getValue(StandingSignBlock.ROTATION);
            return protocolValue | (rotation & 0b0000_1111);
        }
        if (fromState.getBlock() instanceof WallSignBlock) {
            int facing = fromState.getValue(WallSignBlock.FACING).get3DDataValue();
            return protocolValue | (facing & 0b0000_0111);
        }
        return protocolValue;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        if (fromState.getBlock() instanceof StandingSignBlock) {
            int rotation = extraProtocolValue & 0b0000_1111;
            return fromState.setValue(StandingSignBlock.ROTATION, rotation);
        }
        if (fromState.getBlock() instanceof WallSignBlock) {
            int facingData = extraProtocolValue & 0b0000_0111;
            Direction[] dirs = Direction.values();
            Direction facing = facingData < dirs.length ? dirs[facingData] : Direction.NORTH;
            return fromState.setValue(WallSignBlock.FACING, facing);
        }
        return fromState;
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }

    @Override
    public int hao$toProtocolValueAddition(ItemStack fromStack) {
        int attributes = 0;
        CompoundTag tag = getBlockEntityTag(fromStack);
        if (tag == null) {
            return 0;
        }
        attributes |= encodeSignTextFromTag(tag, "front_text", BIT_GLOWING, BIT_COLOR_MASK, BIT_COLOR_SHIFT);
        attributes |= encodeSignTextFromTag(tag, "back_text", BIT_BACK_GLOWING, BIT_BACK_COLOR_MASK, BIT_BACK_COLOR_SHIFT);
        if (tag.getBoolean("is_waxed").orElse(false)) attributes |= BIT_WAXED;
        return attributes;
    }

    @Override
    public @NotNull ItemStack hao$fromProtocolValueAddition(int extraProtocolValue, ItemStack fromStack) {
        ItemStack stackCopy = fromStack.copy();

        boolean glowing = (extraProtocolValue & BIT_GLOWING) != 0;
        int colorOrdinal = (extraProtocolValue & BIT_COLOR_MASK) >>> BIT_COLOR_SHIFT;
        boolean backGlowing = (extraProtocolValue & BIT_BACK_GLOWING) != 0;
        int backColorOrdinal = (extraProtocolValue & BIT_BACK_COLOR_MASK) >>> BIT_BACK_COLOR_SHIFT;

        DyeColor[] colors = DyeColor.values();
        DyeColor color = colorOrdinal < colors.length ? colors[colorOrdinal] : DyeColor.BLACK;
        DyeColor backColor = backColorOrdinal < colors.length ? colors[backColorOrdinal] : DyeColor.BLACK;

        CompoundTag tag = getBlockEntityTag(stackCopy);
        if (tag == null) {
            tag = new CompoundTag();
        }
        applySignTextProperties(tag, "front_text", color, glowing);
        applySignTextProperties(tag, "back_text", backColor, backGlowing);
        return setBlockEntityTag(stackCopy, tag);
    }

    private static int encodeSignTextFromTag(CompoundTag tag, String key, int glowingBit, int colorMask, int colorShift) {
        int attributes = 0;
        CompoundTag text = tag.getCompound(key).orElse(null);
        if (text != null && !text.isEmpty()) {
            if (text.getBoolean("has_glowing_text").orElse(false)) attributes |= glowingBit;
            String colorName = text.getString("color").orElse("");
            for (DyeColor c : DyeColor.values()) {
                if (c.getName().equals(colorName)) {
                    attributes |= (c.ordinal() & 0b1111) << colorShift;
                    break;
                }
            }
        }
        return attributes;
    }

    private static void applySignTextProperties(CompoundTag tag, String key, DyeColor color, boolean glowing) {
        CompoundTag text = tag.getCompound(key).orElse(null);
        if (text == null) {
            text = new CompoundTag();
            tag.put(key, text);
        }
        if (!text.contains("messages")) {
            net.minecraft.nbt.ListTag messages = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < 4; ++i) {
                messages.add(net.minecraft.nbt.StringTag.valueOf(""));
            }
            text.put("messages", messages);
        }
        text.putString("color", color.getName());
        text.putBoolean("has_glowing_text", glowing);
        tag.put(key, text);
    }

    private static @Nullable CompoundTag getBlockEntityTag(ItemStack stack) {
        net.minecraft.world.item.component.TypedEntityData<?> data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTagWithoutId();
    }
    
    private static ItemStack setBlockEntityTag(ItemStack stack, CompoundTag tag) {
        ItemStack stackCopy = stack.copy();
    stackCopy.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.TypedEntityData.of(net.minecraft.world.level.block.entity.BlockEntityTypes.SIGN, tag));
    return stackCopy;
    }
}


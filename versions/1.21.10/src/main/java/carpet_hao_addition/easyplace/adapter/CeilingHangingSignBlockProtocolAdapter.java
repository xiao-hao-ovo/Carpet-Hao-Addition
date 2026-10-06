package carpet_hao_addition.easyplace.adapter;

import net.minecraft.state.property.Properties;
import net.minecraft.component.DataComponentTypes;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.BlockState;

public class CeilingHangingSignBlockProtocolAdapter implements BlockProtocolStateAdapter, ItemStackProtocolDataAdapter {
    public static final CeilingHangingSignBlockProtocolAdapter INSTANCE = new CeilingHangingSignBlockProtocolAdapter();

    private static final int BIT_GLOWING = 0b10_0000;
    private static final int BIT_COLOR_MASK = 0b1111 << 6;
    private static final int BIT_COLOR_SHIFT = 6;
    private static final int BIT_BACK_GLOWING = 0b1000_0000_0000;
    private static final int BIT_BACK_COLOR_MASK = 0b1111 << 12;
    private static final int BIT_BACK_COLOR_SHIFT = 12;
    private static final int BIT_WAXED = 0b100_0000_0000;

    public CeilingHangingSignBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        if (fromState.getBlock() instanceof HangingSignBlock) {
            int rotation = fromState.get(HangingSignBlock.ROTATION);
            boolean isAttached = fromState.get(HangingSignBlock.ATTACHED);
            return protocolValue
                    | (rotation & 0b0000_1111)
                    | (isAttached ? 0b0001_0000 : 0b0000_0000);
        }
        if (fromState.getBlock() instanceof WallHangingSignBlock) {
            Direction _f_facing = (Direction) fromState.get(Properties.HORIZONTAL_FACING);
        int facing = _f_facing.getIndex();
            return protocolValue | (facing & 0b0000_0111);
        }
        return protocolValue;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        if (fromState.getBlock() instanceof HangingSignBlock) {
            int rotation = extraProtocolValue & 0b0000_1111;
            boolean isAttached = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
            return fromState
                    .with(HangingSignBlock.ROTATION, rotation)
                    .with(HangingSignBlock.ATTACHED, isAttached);
        }
        if (fromState.getBlock() instanceof WallHangingSignBlock) {
            int facingData = extraProtocolValue & 0b0000_0111;
            Direction[] dirs = Direction.values();
            Direction facing = facingData < dirs.length ? dirs[facingData] : Direction.NORTH;
            return fromState.with(Properties.HORIZONTAL_FACING, facing);
        }
        return fromState;
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }

    @Override
    public int hao$toProtocolValueAddition(ItemStack fromStack) {
        int attributes = 0;
        NbtCompound tag = this.getBlockEntityTag(fromStack);
        if (tag == null) {
            return 0;
        }
        attributes |= encodeSignTextFromTag(tag, "front_text", BIT_GLOWING, BIT_COLOR_MASK, BIT_COLOR_SHIFT);
        attributes |= encodeSignTextFromTag(tag, "back_text", BIT_BACK_GLOWING, BIT_BACK_COLOR_MASK, BIT_BACK_COLOR_SHIFT);
        if (tag.getBoolean("is_waxed").orElse(false)) attributes |= BIT_WAXED;
        return attributes;
    }

    @Override
    public ItemStack hao$fromProtocolValueAddition(int extraProtocolValue, ItemStack fromStack) {
        ItemStack stackCopy = fromStack.copy();

        boolean glowing = (extraProtocolValue & BIT_GLOWING) != 0;
        int colorOrdinal = (extraProtocolValue & BIT_COLOR_MASK) >>> BIT_COLOR_SHIFT;
        boolean backGlowing = (extraProtocolValue & BIT_BACK_GLOWING) != 0;
        int backColorOrdinal = (extraProtocolValue & BIT_BACK_COLOR_MASK) >>> BIT_BACK_COLOR_SHIFT;

        DyeColor[] colors = DyeColor.values();
        DyeColor color = colorOrdinal < colors.length ? colors[colorOrdinal] : DyeColor.BLACK;
        DyeColor backColor = backColorOrdinal < colors.length ? colors[backColorOrdinal] : DyeColor.BLACK;

        NbtCompound tag = getBlockEntityTag(stackCopy);
        if (tag == null) {
            tag = new NbtCompound();
        }
        applySignTextProperties(tag, "front_text", color, glowing);
        applySignTextProperties(tag, "back_text", backColor, backGlowing);
        return setBlockEntityTag(stackCopy, tag);
    }

    private static int encodeSignTextFromTag(NbtCompound tag, String key, int glowingBit, int colorMask, int colorShift) {
        int attributes = 0;
        NbtCompound text = tag.getCompound(key).orElse(null);
        if (text != null && !text.isEmpty()) {
            if (text.getBoolean("has_glowing_text").orElse(false)) attributes |= glowingBit;
            String colorName = text.getString("color").orElse("");
            for (DyeColor c : DyeColor.values()) {
                if (c.getId().equals(colorName)) {
                    attributes |= (c.ordinal() & 0b1111) << colorShift;
                    break;
                }
            }
        }
        return attributes;
    }

    private static void applySignTextProperties(NbtCompound tag, String key, DyeColor color, boolean glowing) {
        NbtCompound text = tag.getCompound(key).orElse(null);
        if (text == null) {
            text = new NbtCompound();
            tag.put(key, text);
        }
        if (!text.contains("messages")) {
            net.minecraft.nbt.NbtList messages = new net.minecraft.nbt.NbtList();
            for (int i = 0; i < 4; ++i) {
                messages.add(net.minecraft.nbt.NbtString.of("\"\""));
            }
            text.put("messages", messages);
        }
        text.putString("color", color.getId());
        text.putBoolean("has_glowing_text", glowing);
        tag.put(key, text);
    }

    private static NbtCompound getBlockEntityTag(ItemStack stack) {
        net.minecraft.entity.TypedEntityData<?> data = stack.get(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyNbtWithoutId();
    }

    private static ItemStack setBlockEntityTag(ItemStack stack, NbtCompound tag) {
        ItemStack stackCopy = stack.copy();
        tag.putString("id", "minecraft:hanging_sign");
        stackCopy.set(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA, net.minecraft.entity.TypedEntityData.create(net.minecraft.block.entity.BlockEntityType.HANGING_SIGN, tag));
        return stackCopy;
    }
}

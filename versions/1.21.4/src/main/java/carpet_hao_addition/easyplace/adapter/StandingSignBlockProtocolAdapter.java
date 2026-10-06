package carpet_hao_addition.easyplace.adapter;

import net.minecraft.state.property.Properties;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.BlockState;

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
        if (fromState.getBlock() instanceof SignBlock) {
            int rotation = fromState.get(SignBlock.ROTATION);
            return protocolValue | (rotation & 0b0000_1111);
        }
        if (fromState.getBlock() instanceof WallSignBlock) {
            Direction _f_facing = (Direction) fromState.get(Properties.HORIZONTAL_FACING);
        int facing = _f_facing.getId();
            return protocolValue | (facing & 0b0000_0111);
        }
        return protocolValue;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        if (fromState.getBlock() instanceof SignBlock) {
            int rotation = extraProtocolValue & 0b0000_1111;
            return fromState.with(SignBlock.ROTATION, rotation);
        }
        if (fromState.getBlock() instanceof WallSignBlock) {
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
        NbtCompound tag = getBlockEntityTag(fromStack);
        if (tag == null) {
            return 0;
        }
        attributes |= encodeSignTextFromTag(tag, "front_text", BIT_GLOWING, BIT_COLOR_MASK, BIT_COLOR_SHIFT);
        attributes |= encodeSignTextFromTag(tag, "back_text", BIT_BACK_GLOWING, BIT_BACK_COLOR_MASK, BIT_BACK_COLOR_SHIFT);
        if (tag.getBoolean("is_waxed")) attributes |= BIT_WAXED;
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
        NbtCompound text = tag.getCompound(key);
        if (text != null && !text.isEmpty()) {
            if (text.getBoolean("has_glowing_text")) attributes |= glowingBit;
            String colorName = text.getString("color");
            for (DyeColor c : DyeColor.values()) {
                if (c.getName().equals(colorName)) {
                    attributes |= (c.ordinal() & 0b1111) << colorShift;
                    break;
                }
            }
        }
        return attributes;
    }

    private static void applySignTextProperties(NbtCompound tag, String key, DyeColor color, boolean glowing) {
        NbtCompound text = tag.getCompound(key);
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
        text.putString("color", color.getName());
        text.putBoolean("has_glowing_text", glowing);
        tag.put(key, text);
    }


    private static NbtCompound getBlockEntityTag(ItemStack stack) {
        net.minecraft.component.type.NbtComponent data = stack.get(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyNbt();
    }
    private static ItemStack setBlockEntityTag(ItemStack stack, NbtCompound tag) {
        ItemStack stackCopy = stack.copy();
        tag.putString("id", "minecraft:sign");
        stackCopy.set(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA, net.minecraft.component.type.NbtComponent.of(tag));
        return stackCopy;
    }
}

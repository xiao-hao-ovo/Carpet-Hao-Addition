package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        if (fromState.getBlock() instanceof CeilingHangingSignBlock) {
            int rotation = fromState.getValue(CeilingHangingSignBlock.ROTATION);
            boolean isAttached = fromState.getValue(CeilingHangingSignBlock.ATTACHED);
            return protocolValue
                    | (rotation & 0b0000_1111)
                    | (isAttached ? 0b0001_0000 : 0b0000_0000);
        }
        if (fromState.getBlock() instanceof WallHangingSignBlock) {
            int facing = fromState.getValue(WallHangingSignBlock.FACING).get3DDataValue();
            return protocolValue | (facing & 0b0000_0111);
        }
        return protocolValue;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        if (fromState.getBlock() instanceof CeilingHangingSignBlock) {
            int rotation = extraProtocolValue & 0b0000_1111;
            boolean isAttached = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
            return fromState
                    .setValue(CeilingHangingSignBlock.ROTATION, rotation)
                    .setValue(CeilingHangingSignBlock.ATTACHED, isAttached);
        }
        if (fromState.getBlock() instanceof WallHangingSignBlock) {
            int facingData = extraProtocolValue & 0b0000_0111;
            Direction[] dirs = Direction.values();
            Direction facing = facingData < dirs.length ? dirs[facingData] : Direction.NORTH;
            return fromState.setValue(WallHangingSignBlock.FACING, facing);
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
        CompoundTag tag = this.getBlockEntityTag(fromStack);
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
		System.out.println("[hao-sign] 编码 color='" + colorName + "' key=" + key);
            for (DyeColor c : DyeColor.values()) {
                if (c.getName().equals(colorName)) {
                    attributes |= (c.ordinal() & 0b1111) << colorShift;
                    break;
                }
            }
        }
        return attributes;
    }

    /**
     * 把颜色 / 发光写回方块实体 NBT。
     * <p>
     * **必须走 SignText.CODEC**:26.x 的标牌 NBT 就是用它序列化的 ——
     * SignBlockEntity.saveAdditional 里是 valueOutput.store("front_text", SignText.CODEC, frontText),
     * 读回来也走同一个 codec。手写 color 键(1.21.8 的字符串、int 都试过)都不是它认的格式,
     * 结果就是文字能还原、颜色永远是黑的。
     */
    private static void applySignTextProperties(CompoundTag tag, String key, DyeColor color, boolean glowing) {
        SignText text = tag.getCompound(key)
                .flatMap(compound -> SignText.CODEC.parse(NbtOps.INSTANCE, compound).result())
                .orElse(SignText.EMPTY);
        SignText updated = text.withColor(color).withGlowingText(glowing);
        SignText.CODEC.encodeStart(NbtOps.INSTANCE, updated)
                .result()
                .ifPresent(encoded -> tag.put(key, (CompoundTag) encoded));
    }

    private static @Nullable CompoundTag getBlockEntityTag(ItemStack stack) {
        net.minecraft.world.item.component.TypedEntityData<?> data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTagWithoutId();
    }
    
    private static ItemStack setBlockEntityTag(ItemStack stack, CompoundTag tag) {
        ItemStack stackCopy = stack.copy();
    stackCopy.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.TypedEntityData.of(net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN, tag));
    return stackCopy;
    }
}


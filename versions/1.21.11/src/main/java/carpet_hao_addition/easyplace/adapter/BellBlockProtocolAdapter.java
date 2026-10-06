package carpet_hao_addition.easyplace.adapter;

import net.minecraft.block.enums.Attachment;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.BellBlock;
import net.minecraft.block.BlockState;


public class BellBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final BellBlockProtocolAdapter INSTANCE = new BellBlockProtocolAdapter();

    private static final int BIT_FACING_MASK = 0b111;
    private static final int BIT_ATTACHMENT_MASK = 0b0011_0000;
    private static final int BIT_ATTACHMENT_SHIFT = 4;

    public BellBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int attachmentOrdinal = fromState.get(BellBlock.ATTACHMENT).ordinal();
        int facingBits = ((horizontalFacingIndex(fromState.get(BellBlock.FACING)) + 1) & BIT_FACING_MASK);
        int bits = facingBits | ((attachmentOrdinal & 0b11) << BIT_ATTACHMENT_SHIFT);
        return protocolValue | bits;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        // 注意: 不能用 % 3 —— Yarn 的 Attachment 有【4】个值
        // (FLOOR, CEILING, SINGLE_WALL, DOUBLE_WALL), DOUBLE_WALL(3) % 3 == 0 会被解成 FLOOR,
        // 表现就是"钟放出来贴附类型不对"(实测 double_wall 被还原成 floor)。
        // 用 & 0b11 正好覆盖 4 个值。上游同样写成 % 3, 是它的 bug。
        int attachmentOrdinal = ((extraProtocolValue & BIT_ATTACHMENT_MASK) >>> BIT_ATTACHMENT_SHIFT) & 0b11;
        BlockState state = fromState.with(BellBlock.ATTACHMENT, net.minecraft.block.enums.Attachment.values()[attachmentOrdinal]);

        int facingIndex = (extraProtocolValue & BIT_FACING_MASK) - 1;
        if (facingIndex >= 0 && facingIndex <= 3) {
            Direction facing = horizontalFacing(facingIndex);
            if (BellBlock.FACING.getValues().contains(facing)) {
                state = state.with(BellBlock.FACING, facing);
            }
        }
        return state;
    }

    private static int horizontalFacingIndex(Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static Direction horizontalFacing(int index) {
        return switch (index) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

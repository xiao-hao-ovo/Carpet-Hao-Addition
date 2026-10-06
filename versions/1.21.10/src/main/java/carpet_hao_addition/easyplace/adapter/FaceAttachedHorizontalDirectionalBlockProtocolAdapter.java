package carpet_hao_addition.easyplace.adapter;

import net.minecraft.state.property.Properties;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockFace;

public class FaceAttachedHorizontalDirectionalBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final FaceAttachedHorizontalDirectionalBlockProtocolAdapter INSTANCE = new FaceAttachedHorizontalDirectionalBlockProtocolAdapter();

    private static final int BIT_FACING_MASK = 0b111;
    private static final int BIT_FACE_MASK = 0b0011_0000;
    private static final int BIT_FACE_SHIFT = 4;

    public FaceAttachedHorizontalDirectionalBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int faceOrdinal = fromState.get(WallMountedBlock.FACE).ordinal();
        int facingBits = ((horizontalFacingIndex(fromState.get(Properties.HORIZONTAL_FACING)) + 1) & BIT_FACING_MASK);
        int bits = facingBits | ((faceOrdinal & 0b11) << BIT_FACE_SHIFT);
        return protocolValue | bits;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int faceOrdinal = ((extraProtocolValue & BIT_FACE_MASK) >>> BIT_FACE_SHIFT) % 3;
        net.minecraft.block.enums.BlockFace att = net.minecraft.block.enums.BlockFace.values()[faceOrdinal];
        BlockState state = fromState.with(WallMountedBlock.FACE, att);

        int facingIndex = (extraProtocolValue & BIT_FACING_MASK) - 1;
        if (facingIndex >= 0 && facingIndex <= 3) {
            Direction facing = horizontalFacing(facingIndex);
            if (Properties.HORIZONTAL_FACING.getValues().contains(facing)) {
                state = state.with(Properties.HORIZONTAL_FACING, facing);
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

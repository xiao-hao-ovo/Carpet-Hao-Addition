package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.BlockState;

public class HorizontalDirectionalBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final HorizontalDirectionalBlockProtocolAdapter INSTANCE = new HorizontalDirectionalBlockProtocolAdapter();

    public HorizontalDirectionalBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        Direction facing = fromState.get(HorizontalFacingBlock.FACING);
        return switch (facing) {
            case NORTH -> 0b0000;
            case EAST -> 0b0001;
            case SOUTH -> 0b0010;
            case WEST -> 0b0011;
            default -> 0b0000;
        };
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int directionIndex = extraProtocolValue & 0b0011;

        Direction facing = switch (directionIndex) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };

        return fromState.with(HorizontalFacingBlock.FACING, facing);
    }

    @Override
    public BlockProtocolStateAdapter.ProtocolType hao$getProtocolType() {
        return BlockProtocolStateAdapter.ProtocolType.ADDED;
    }
}

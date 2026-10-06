package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.BlockState;

public class CampfireBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final CampfireBlockProtocolAdapter INSTANCE = new CampfireBlockProtocolAdapter();

    public CampfireBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        Direction facing = fromState.get(HorizontalFacingBlock.FACING);
        int facingBits = switch (facing) {
            case NORTH -> 0b0000;
            case EAST -> 0b0001;
            case SOUTH -> 0b0010;
            case WEST -> 0b0011;
            default -> 0b0000;
        };
        boolean isLit = fromState.get(CampfireBlock.LIT);
        int litBits = (isLit ? 0b0001_0000 : 0b0000_0000);
        return protocolValue | facingBits | litBits;
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
        boolean isLit = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
        return fromState
                .with(HorizontalFacingBlock.FACING, facing)
                .with(CampfireBlock.LIT, isLit);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

package carpet_hao_addition.easyplace.adapter;

import net.minecraft.block.enums.BlockHalf;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.StairShape;

public class StairBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final StairBlockProtocolAdapter INSTANCE = new StairBlockProtocolAdapter();

    public StairBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int facingOrdinal = fromState.get(StairsBlock.FACING).ordinal() - 2;
        int halfOrdinal = fromState.get(StairsBlock.HALF).ordinal();
        int shapeOrdinal = fromState.get(StairsBlock.SHAPE).ordinal();
        return (facingOrdinal & 0b0000_0011) |
        (halfOrdinal & 0b0000_0001) << 2 |
        (shapeOrdinal & 0b0000_0111) << 3;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int facingOrdinal = (extraProtocolValue & 0b0000_0011) + 2;
        int halfOrdinal = (extraProtocolValue & 0b0000_0100) >>> 2;
        int shapeOrdinal = (extraProtocolValue & 0b0011_1000) >>> 3;
        return fromState
                .with(StairsBlock.FACING, Direction.values()[facingOrdinal])
                .with(StairsBlock.HALF, BlockHalf.values()[halfOrdinal])
                .with(StairsBlock.SHAPE, StairShape.values()[shapeOrdinal]);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

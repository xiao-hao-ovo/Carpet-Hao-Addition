package carpet_hao_addition.easyplace.adapter;

import net.minecraft.state.property.Properties;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.WallBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WallShape;

public class WallBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final WallBlockProtocolAdapter INSTANCE = new WallBlockProtocolAdapter();

    public WallBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        WallShape north = fromState.get(WallBlock.NORTH_SHAPE);
        WallShape east = fromState.get(WallBlock.EAST_SHAPE);
        WallShape south = fromState.get(WallBlock.SOUTH_SHAPE);
        WallShape west = fromState.get(WallBlock.WEST_SHAPE);
        boolean up = fromState.get(WallBlock.UP);

        int bits = 0;
        bits |= (north.ordinal() & 0b11);
        bits |= (east.ordinal() & 0b11) << 2;
        bits |= (south.ordinal() & 0b11) << 4;
        bits |= (west.ordinal() & 0b11) << 6;
        if (up) bits |= 1 << 8;

        return bits;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        WallShape north = WallShape.values()[((extraProtocolValue) & 0b11) % 3];
        WallShape east = WallShape.values()[((extraProtocolValue >> 2) & 0b11) % 3];
        WallShape south = WallShape.values()[((extraProtocolValue >> 4) & 0b11) % 3];
        WallShape west = WallShape.values()[((extraProtocolValue >> 6) & 0b11) % 3];
        boolean up = ((extraProtocolValue >> 8) & 0b1) == 0b1;

        return fromState
                .with(WallBlock.NORTH_SHAPE, north)
                .with(WallBlock.EAST_SHAPE, east)
                .with(WallBlock.SOUTH_SHAPE, south)
                .with(WallBlock.WEST_SHAPE, west)
                .with(WallBlock.UP, up);
    }

    @Override
    public BlockProtocolStateAdapter.ProtocolType hao$getProtocolType() {
        return BlockProtocolStateAdapter.ProtocolType.ADDED;
    }
}

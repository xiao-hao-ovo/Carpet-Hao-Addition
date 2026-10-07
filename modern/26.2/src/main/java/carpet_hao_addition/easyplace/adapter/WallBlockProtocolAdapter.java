package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WallBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final WallBlockProtocolAdapter INSTANCE = new WallBlockProtocolAdapter();

    public WallBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        WallSide north = fromState.getValue(WallBlock.NORTH);
        WallSide east = fromState.getValue(WallBlock.EAST);
        WallSide south = fromState.getValue(WallBlock.SOUTH);
        WallSide west = fromState.getValue(WallBlock.WEST);
        boolean up = fromState.getValue(WallBlock.UP);

        int bits = 0;
        bits |= (north.ordinal() & 0b11);
        bits |= (east.ordinal() & 0b11) << 2;
        bits |= (south.ordinal() & 0b11) << 4;
        bits |= (west.ordinal() & 0b11) << 6;
        if (up) bits |= 1 << 8;

        return bits;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        WallSide north = WallSide.values()[((extraProtocolValue) & 0b11) % 3];
        WallSide east = WallSide.values()[((extraProtocolValue >> 2) & 0b11) % 3];
        WallSide south = WallSide.values()[((extraProtocolValue >> 4) & 0b11) % 3];
        WallSide west = WallSide.values()[((extraProtocolValue >> 6) & 0b11) % 3];
        boolean up = ((extraProtocolValue >> 8) & 0b1) == 0b1;

        return fromState
                .setValue(WallBlock.NORTH, north)
                .setValue(WallBlock.EAST, east)
                .setValue(WallBlock.SOUTH, south)
                .setValue(WallBlock.WEST, west)
                .setValue(WallBlock.UP, up);
    }

    @Override
    public @NotNull BlockProtocolStateAdapter.ProtocolType hao$getProtocolType() {
        return BlockProtocolStateAdapter.ProtocolType.ADDED;
    }
}

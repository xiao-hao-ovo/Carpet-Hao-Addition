package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RedStoneWireBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final RedStoneWireBlockProtocolAdapter INSTANCE = new RedStoneWireBlockProtocolAdapter();

    private static final int BIT_IS_DOT = 0b0001;

    public RedStoneWireBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isDot = !fromState.getValue(RedstoneWireBlock.NORTH).isConnected()
                && !fromState.getValue(RedstoneWireBlock.EAST).isConnected()
                && !fromState.getValue(RedstoneWireBlock.SOUTH).isConnected()
                && !fromState.getValue(RedstoneWireBlock.WEST).isConnected();
        return isDot ? BIT_IS_DOT : 0b0000;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isDot = (extraProtocolValue & BIT_IS_DOT) == BIT_IS_DOT;
        if (!isDot) {
            return fromState;
        }
        return fromState.setValue(RedstoneWireBlock.NORTH, RedstoneSide.NONE)
                .setValue(RedstoneWireBlock.EAST, RedstoneSide.NONE)
                .setValue(RedstoneWireBlock.SOUTH, RedstoneSide.NONE)
                .setValue(RedstoneWireBlock.WEST, RedstoneSide.NONE);
    }

    @Override
    public @NotNull BlockProtocolStateAdapter.ProtocolType hao$getProtocolType() {
        return BlockProtocolStateAdapter.ProtocolType.ADDED;
    }
}

package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WireConnection;

public class RedStoneWireBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final RedStoneWireBlockProtocolAdapter INSTANCE = new RedStoneWireBlockProtocolAdapter();

    private static final int BIT_IS_DOT = 0b0001;

    public RedStoneWireBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isDot = !fromState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH).isConnected()
                && !fromState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST).isConnected()
                && !fromState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH).isConnected()
        && !fromState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST).isConnected();
        return isDot ? BIT_IS_DOT : 0b0000;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        boolean isDot = (extraProtocolValue & BIT_IS_DOT) == BIT_IS_DOT;
        if (!isDot) {
            return fromState;
        }
        return fromState.with(RedstoneWireBlock.WIRE_CONNECTION_NORTH, WireConnection.NONE)
                .with(RedstoneWireBlock.WIRE_CONNECTION_EAST, WireConnection.NONE)
                .with(RedstoneWireBlock.WIRE_CONNECTION_SOUTH, WireConnection.NONE)
        .with(RedstoneWireBlock.WIRE_CONNECTION_WEST, WireConnection.NONE);
    }

    @Override
    public BlockProtocolStateAdapter.ProtocolType hao$getProtocolType() {
        return BlockProtocolStateAdapter.ProtocolType.ADDED;
    }
}

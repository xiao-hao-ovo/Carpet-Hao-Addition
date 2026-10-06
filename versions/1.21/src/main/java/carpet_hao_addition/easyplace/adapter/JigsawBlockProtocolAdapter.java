package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.block.enums.Orientation;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.BlockState;

public class JigsawBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final JigsawBlockProtocolAdapter INSTANCE = new JigsawBlockProtocolAdapter();

    public JigsawBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int orientationOrdinal = fromState.get(JigsawBlock.ORIENTATION).ordinal();
        return protocolValue | ((orientationOrdinal + 1) & 0b0000_1111);
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int orientationOrdinal = Math.floorMod((extraProtocolValue & 0b0000_1111) - 1, 12);
        return fromState.with(JigsawBlock.ORIENTATION, Orientation.values()[orientationOrdinal]);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

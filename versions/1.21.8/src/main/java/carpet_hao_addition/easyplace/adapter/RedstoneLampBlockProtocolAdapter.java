package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.BlockState;

public class RedstoneLampBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final RedstoneLampBlockProtocolAdapter INSTANCE = new RedstoneLampBlockProtocolAdapter();

    public RedstoneLampBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isLit = fromState.get(RedstoneLampBlock.LIT);
        int bits = (isLit ? 0b0001_0000 : 0b0000_0000);
        return protocolValue | bits;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        boolean isLit = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
        return fromState.with(RedstoneLampBlock.LIT, isLit);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

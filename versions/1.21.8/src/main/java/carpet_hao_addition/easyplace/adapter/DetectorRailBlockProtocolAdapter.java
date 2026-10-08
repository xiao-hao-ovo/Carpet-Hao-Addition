package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.DetectorRailBlock;
import net.minecraft.item.ItemPlacementContext;

/** 探测铁轨:协议里带通电状态。 */
public class DetectorRailBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final DetectorRailBlockProtocolAdapter INSTANCE = new DetectorRailBlockProtocolAdapter();

    public DetectorRailBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        return fromState.get(DetectorRailBlock.POWERED) ? 0b0001 : 0b0000;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        boolean isPower = (extraProtocolValue & 0b0001) == 0b0001;
        return fromState.with(DetectorRailBlock.POWERED, isPower);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

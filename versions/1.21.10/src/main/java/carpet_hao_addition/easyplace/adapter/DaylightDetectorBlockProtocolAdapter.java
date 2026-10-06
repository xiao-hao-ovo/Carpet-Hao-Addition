package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.BlockState;

public class DaylightDetectorBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final DaylightDetectorBlockProtocolAdapter INSTANCE = new DaylightDetectorBlockProtocolAdapter();

    public DaylightDetectorBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isInverted = fromState.get(DaylightDetectorBlock.INVERTED);
        return isInverted ? 0b0001 : 0b0000;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        boolean isInverted = (extraProtocolValue & 0b0001) == 0b0001;
        return fromState.with(DaylightDetectorBlock.INVERTED, isInverted);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

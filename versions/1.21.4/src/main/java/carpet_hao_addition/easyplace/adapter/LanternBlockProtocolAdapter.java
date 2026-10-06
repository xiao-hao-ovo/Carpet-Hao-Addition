package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.BlockState;

public class LanternBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final LanternBlockProtocolAdapter INSTANCE = new LanternBlockProtocolAdapter();

    public LanternBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isHanging = fromState.get(LanternBlock.HANGING);
        return isHanging ? 0b0001 : 0b0000;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        boolean isHanging = (extraProtocolValue & 0b0001) == 0b0001;
        return fromState.with(LanternBlock.HANGING, isHanging);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

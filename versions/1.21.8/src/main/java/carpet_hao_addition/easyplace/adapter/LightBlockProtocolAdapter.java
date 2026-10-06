package carpet_hao_addition.easyplace.adapter;

import net.minecraft.state.property.Properties;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.LightBlock;
import net.minecraft.block.BlockState;

public class LightBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final LightBlockProtocolAdapter INSTANCE = new LightBlockProtocolAdapter();

    public LightBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int level = fromState.get(Properties.LEVEL_15);
        return level & 0b0000_1111;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int level = extraProtocolValue & 0b0000_1111;
        return fromState.with(Properties.LEVEL_15, level);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

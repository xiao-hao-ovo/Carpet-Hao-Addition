package carpet_hao_addition.easyplace.adapter;

import net.minecraft.state.property.Properties;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;

public class SkullBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final SkullBlockProtocolAdapter INSTANCE = new SkullBlockProtocolAdapter();

    public SkullBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        if (!fromState.contains(Properties.ROTATION)) return protocolValue;   // 墙挂变体没有该属性
        int rotation = fromState.get(Properties.ROTATION);
        return (protocolValue & 0b1111_0000) | ((rotation + 1) & 0b0000_1111);
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int rotation = (extraProtocolValue & 0b0000_1111) - 1;
        if (rotation < 0) rotation = 0;
        return fromState.with(Properties.ROTATION, rotation);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

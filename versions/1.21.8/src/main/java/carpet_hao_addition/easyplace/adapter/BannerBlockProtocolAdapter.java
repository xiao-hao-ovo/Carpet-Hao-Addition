package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;

public class BannerBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final BannerBlockProtocolAdapter INSTANCE = new BannerBlockProtocolAdapter();

    public BannerBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        if (!fromState.contains(BannerBlock.ROTATION)) return protocolValue;   // 墙挂变体没有该属性
        int rotation = fromState.get(BannerBlock.ROTATION);
        return (protocolValue & 0b1111_0000) | ((rotation + 1) & 0b0000_1111);
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int rotation = (extraProtocolValue & 0b0000_1111) - 1;
        if (rotation < 0) rotation = 0;
        return fromState.with(BannerBlock.ROTATION, rotation);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

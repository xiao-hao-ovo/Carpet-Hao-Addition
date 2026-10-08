package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.RailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.item.ItemPlacementContext;

/** 铁轨:协议里带轨道形状,并标记"放置时不要按邻居重算形状"。 */
public class RailBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final RailBlockProtocolAdapter INSTANCE = new RailBlockProtocolAdapter();

    public RailBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int shapeOrdinal = fromState.get(RailBlock.SHAPE).ordinal();
        return (shapeOrdinal & 0b0000_1111) | 0b0001_0000;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int shapeOrdinal = (extraProtocolValue & 0b0000_1111) % 10;
        boolean noUpdate = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;

        if (noUpdate) {
            BetterEasyPlaceProtocolHandler.setPlaceFlag(
                    BetterEasyPlaceProtocolHandler.EASY_PLACE_RAIL_BLOCK_NO_SHAPE_UPDATE);
        }

        return fromState.with(RailBlock.SHAPE, RailShape.values()[shapeOrdinal]);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

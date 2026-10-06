package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.StructureBlockMode;

public class StructureBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final StructureBlockProtocolAdapter INSTANCE = new StructureBlockProtocolAdapter();

    public StructureBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int modeOrdinal = fromState.get(StructureBlock.MODE).ordinal();
        return modeOrdinal & 0b0011;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int modeOrdinal = extraProtocolValue & 0b0011;
        return fromState.with(StructureBlock.MODE, StructureBlockMode.values()[modeOrdinal]);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

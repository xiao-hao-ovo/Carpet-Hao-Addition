package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.BlockState;

public class HugeMushroomBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final HugeMushroomBlockProtocolAdapter INSTANCE = new HugeMushroomBlockProtocolAdapter();

    public HugeMushroomBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        return (fromState.get(MushroomBlock.NORTH) ? 0b0000_0001 : 0b0000_0000) |
        (fromState.get(MushroomBlock.EAST) ? 0b0000_0010 : 0b0000_0000) |
        (fromState.get(MushroomBlock.SOUTH) ? 0b0000_0100 : 0b0000_0000) |
        (fromState.get(MushroomBlock.WEST) ? 0b0000_1000 : 0b0000_0000) |
        (fromState.get(MushroomBlock.UP) ? 0b0001_0000 : 0b0000_0000) |
        (fromState.get(MushroomBlock.DOWN) ? 0b0010_0000 : 0b0000_0000);
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        return fromState
                .with(MushroomBlock.NORTH, (extraProtocolValue & 0b0000_0001) == 0b0000_0001)
                .with(MushroomBlock.EAST, (extraProtocolValue & 0b0000_0010) == 0b0000_0010)
                .with(MushroomBlock.SOUTH, (extraProtocolValue & 0b0000_0100) == 0b0000_0100)
                .with(MushroomBlock.WEST, (extraProtocolValue & 0b0000_1000) == 0b0000_1000)
                .with(MushroomBlock.UP, (extraProtocolValue & 0b0001_0000) == 0b0001_0000)
                .with(MushroomBlock.DOWN, (extraProtocolValue & 0b0010_0000) == 0b0010_0000);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

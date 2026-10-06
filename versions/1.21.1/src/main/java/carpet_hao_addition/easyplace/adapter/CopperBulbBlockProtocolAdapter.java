package carpet_hao_addition.easyplace.adapter;


import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.BulbBlock;
import net.minecraft.block.BlockState;

public class CopperBulbBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final CopperBulbBlockProtocolAdapter INSTANCE = new CopperBulbBlockProtocolAdapter();

    public CopperBulbBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int lit = fromState.get(BulbBlock.LIT) ? 0b0001 : 0b0000;
        int powered = fromState.get(BulbBlock.POWERED) ? 0b0010 : 0b0000;
        return lit | powered;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        
        boolean lit = (extraProtocolValue & 0b0001) == 0b0001;
        boolean powered = (extraProtocolValue & 0b0010) == 0b0010;
        return fromState.with(BulbBlock.LIT, lit).with(BulbBlock.POWERED, powered);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

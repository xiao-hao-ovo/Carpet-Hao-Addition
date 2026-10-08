package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.Direction;

/** 漏斗:协议里带 6 向朝向和"是否启用(被红石锁住)"。 */
public class HopperBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final HopperBlockProtocolAdapter INSTANCE = new HopperBlockProtocolAdapter();

    private static final int BIT_FACING_MASK = 0b0111;
    private static final int BIT_ENABLED_DISABLED = 0b1000;

    public HopperBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        Direction facing = fromState.get(HopperBlock.FACING);
        int v = protocolValue;
        v |= ((facing.getIndex() + 1) & BIT_FACING_MASK);
        if (!fromState.get(HopperBlock.ENABLED)) {
            v |= BIT_ENABLED_DISABLED;
        }
        return v;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        BlockState state = fromState;
        int facingIndex = (extraProtocolValue & BIT_FACING_MASK) - 1;
        if (facingIndex >= 0 && facingIndex <= 5) {
            Direction facing = Direction.byIndex(facingIndex);
            if (state.contains(HopperBlock.FACING) && HopperBlock.FACING.getValues().contains(facing)) {
                state = state.with(HopperBlock.FACING, facing);
            }
        }
        boolean enabled = (extraProtocolValue & BIT_ENABLED_DISABLED) == 0;
        return state.with(HopperBlock.ENABLED, enabled);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

package carpet_hao_addition.easyplace.adapter;

import net.minecraft.state.property.Properties;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.BlockState;

public class CommandBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final CommandBlockProtocolAdapter INSTANCE = new CommandBlockProtocolAdapter();

    private static final int BIT_FACING_MASK = 0b111;
    private static final int BIT_CONDITIONAL = 0b0001_0000;

    public CommandBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        // Command block FACING is the full 6-direction property; encode it as (3D data value + 1)
        // so that DOWN (value 0) still produces a non-zero addition and is never skipped.
        Direction cmdFacing = (Direction) fromState.get(Properties.FACING);
        int facingBits = ((cmdFacing.getIndex() + 1) & BIT_FACING_MASK);
        boolean isConditional = fromState.get(Properties.CONDITIONAL);
        int conditionalBits = (isConditional ? BIT_CONDITIONAL : 0);
        return protocolValue | facingBits | conditionalBits;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        boolean isConditional = (extraProtocolValue & BIT_CONDITIONAL) == BIT_CONDITIONAL;
        BlockState state = fromState.with(Properties.CONDITIONAL, isConditional);

        int facing3d = (extraProtocolValue & BIT_FACING_MASK) - 1;
        if (facing3d >= 0 && facing3d <= 5) {
            Direction facing = Direction.values()[Math.max(0, Math.min(5, facing3d))];
            if (Properties.FACING.getValues().contains(facing)) {
                state = state.with(Properties.FACING, facing);
            }
        }
        return state;
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

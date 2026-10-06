package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;

public class FlowerBedBlockProtocolAdapter implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    public static final FlowerBedBlockProtocolAdapter INSTANCE = new FlowerBedBlockProtocolAdapter();

    private static final Property<Direction> FACING = net.minecraft.block.FlowerbedBlock.HORIZONTAL_FACING;
    private static final IntProperty AMOUNT = net.minecraft.block.FlowerbedBlock.FLOWER_AMOUNT;

    public FlowerBedBlockProtocolAdapter() {
    }

    private static boolean isTarget(BlockState state) {
        return state.getBlock() instanceof net.minecraft.block.FlowerbedBlock;
    }

    @Override
    public void hao$setLoopCount(LoopContext ctx) {
        int curAmount = isTarget(ctx.stateClient) ? ctx.stateClient.get(AMOUNT) : 0;
        int targetAmount = ctx.stateSchematic.get(AMOUNT);

        if (targetAmount > curAmount) {
            ctx.loopCount = targetAmount - curAmount;
        } else {
            ctx.loopCount = 0;
        }
    }

    @Override
    public int hao$toProtocolValueLoop(LoopContext ctx) {
        int facingOrdinal = ctx.stateSchematic.get(FACING).ordinal() - 2;
        int maxAmount = ctx.stateSchematic.get(AMOUNT) - 1;

        return ((facingOrdinal & 0b0011) << 2) |
        (maxAmount & 0b0011);
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int facingOrdinal = ((extraProtocolValue & 0b1100) >>> 2) + 2;
        int maxAmount = (extraProtocolValue & 0b0011) + 1;

        World world = context.getWorld();
        BlockState blockWorldState = world.getBlockState(EasyPlaceExtraProtocolHelper.getClickedPos(context));

        BlockState newState = fromState;

        
        if (!isTarget(blockWorldState)) {
            newState = newState.with(FACING, Direction.values()[facingOrdinal]);
        }

        if (newState.get(AMOUNT) > maxAmount) {
            return null;
        }

        return newState;
    }

    
    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        return 0;
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

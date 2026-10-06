package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.block.BlockState;

public class TurtleEggBlockProtocolAdapter implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    public static final TurtleEggBlockProtocolAdapter INSTANCE = new TurtleEggBlockProtocolAdapter();

    public TurtleEggBlockProtocolAdapter() {
    }

    @Override
    public void hao$setLoopCount(LoopContext ctx) {
        boolean isTurtleEgg = ctx.stateClient.getBlock() instanceof TurtleEggBlock;
        int curEggs = isTurtleEgg ? ctx.stateClient.get(TurtleEggBlock.EGGS) : 0;
        int targetEggs = ctx.stateSchematic.get(TurtleEggBlock.EGGS);

        if (targetEggs > curEggs) {
            ctx.loopCount = targetEggs - curEggs;
        } else {
            ctx.loopCount = 0;
        }
    }

    @Override
    public int hao$toProtocolValueLoop(LoopContext ctx) {
        return (ctx.stateSchematic.get(TurtleEggBlock.EGGS) - 1) & 0b0011;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int maxEggs = (extraProtocolValue & 0b0011) + 1;
        if (fromState.get(TurtleEggBlock.EGGS) > maxEggs) {
            return null;
        }
        return fromState;
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

package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SeaPickleBlockProtocolAdapter implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    public static final SeaPickleBlockProtocolAdapter INSTANCE = new SeaPickleBlockProtocolAdapter();

    public SeaPickleBlockProtocolAdapter() {
    }

    @Override
    public void hao$setLoopCount(LoopContext ctx) {
        boolean isSeaPickle = ctx.stateClient.getBlock() instanceof SeaPickleBlock;
        int curPickles = isSeaPickle ? ctx.stateClient.getValue(SeaPickleBlock.PICKLES) : 0;
        int targetPickles = ctx.stateSchematic.getValue(SeaPickleBlock.PICKLES);

        if (targetPickles > curPickles) {
            ctx.loopCount = targetPickles - curPickles;
        } else {
            ctx.loopCount = 0;
        }
    }

    @Override
    public int hao$toProtocolValueLoop(LoopContext ctx) {
        return (ctx.stateSchematic.getValue(SeaPickleBlock.PICKLES) - 1) & 0b0011;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int maxPickles = (extraProtocolValue & 0b0011) + 1;
        if (fromState.getValue(SeaPickleBlock.PICKLES) > maxPickles) {
            return null;
        }
        return fromState;
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        return 0;
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

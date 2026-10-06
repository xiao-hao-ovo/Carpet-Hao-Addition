package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.BlockState;

public class SnowLayerBlockProtocolAdapter implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    public static final SnowLayerBlockProtocolAdapter INSTANCE = new SnowLayerBlockProtocolAdapter();

    public SnowLayerBlockProtocolAdapter() {
    }

    @Override
    public void hao$setLoopCount(LoopContext ctx) {
        boolean isSnow = ctx.stateClient.getBlock() instanceof SnowBlock;
        int curLayers = isSnow ? ctx.stateClient.get(SnowBlock.LAYERS) : 0;
        int targetLayers = ctx.stateSchematic.get(SnowBlock.LAYERS);

        if (targetLayers > curLayers) {
            ctx.loopCount = targetLayers - curLayers;
        } else {
            ctx.loopCount = 0;
        }
    }

    @Override
    public int hao$toProtocolValueLoop(LoopContext ctx) {
        return (ctx.stateSchematic.get(SnowBlock.LAYERS) - 1) & 0b0111;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int maxLayers = (extraProtocolValue & 0b0111) + 1;
        if (fromState.get(SnowBlock.LAYERS) > maxLayers) {
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

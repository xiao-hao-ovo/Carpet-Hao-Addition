package carpet_hao_addition.easyplace;

import net.minecraft.world.level.block.state.BlockState;

public interface MultiStageBlockProtocolStateAdapter {
    void hao$setLoopCount(LoopContext ctx);

    int hao$toProtocolValueLoop(LoopContext ctx);

    class LoopContext {
        public BlockState stateSchematic = null;
        public BlockState stateClient = null;
        public int loopCount = 0;
        public int loopIndex = 0;
        public Object data;
    }
}

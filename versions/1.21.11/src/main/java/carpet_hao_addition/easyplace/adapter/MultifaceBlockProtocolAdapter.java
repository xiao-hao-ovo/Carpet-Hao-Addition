package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;

import net.minecraft.block.Blocks;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.world.World;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.block.BlockState;

import java.util.ArrayList;
import java.util.List;

public class MultifaceBlockProtocolAdapter implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    public static final MultifaceBlockProtocolAdapter INSTANCE = new MultifaceBlockProtocolAdapter();

    public MultifaceBlockProtocolAdapter() {
    }

    private static boolean hasDirection(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof MultifaceBlock)) {
            return false;
        }
        return MultifaceBlock.hasDirection(state, direction);
    }

    
    @Override
    public void hao$setLoopCount(LoopContext ctx) {
        List<Integer> requireDirection = new ArrayList<>();
        ctx.data = requireDirection;

        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; ++i) {
            Direction direction = directions[i];
            if (hasDirection(ctx.stateSchematic, direction) &&
                    !hasDirection(ctx.stateClient, direction)) {
                requireDirection.add(i);
                ++ctx.loopCount;
            }
        }
    }

    
    @Override
    public int hao$toProtocolValueLoop(LoopContext ctx) {
        @SuppressWarnings("unchecked")
        List<Integer> requireDirection = (List<Integer>) ctx.data;

        if (ctx.loopIndex >= requireDirection.size()) {
            return 0b0111;
        }

        return requireDirection.get(ctx.loopIndex) & 0b0111;
    }

    
    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int index = extraProtocolValue & 0b0111;
        Direction[] directions = Direction.values();
        if (index >= directions.length) {
            return null;
        }

        World world = context.getWorld();
        BlockState blockWorldState = world.getBlockState(EasyPlaceExtraProtocolHelper.getClickedPos(context));

        // Yarn 1.21.8 没有 Mojang 那个 getPlacementState(BlockState, BlockView, BlockPos, Direction) 重载,
        // 这里直接用 getProperty(Direction) 给对应面置位。
        Direction dir = directions[index];
        BlockState base = fromState.getBlock() instanceof MultifaceBlock
                ? fromState
                : blockWorldState.getBlock() instanceof MultifaceBlock
                        ? blockWorldState
                        : net.minecraft.block.Blocks.GLOW_LICHEN.getDefaultState();
        return base.with(MultifaceBlock.getProperty(dir), true);
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

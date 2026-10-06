package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;

import net.minecraft.block.MultifaceBlock;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.VineBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.BooleanProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VineBlockProtocolAdapter implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    public static final VineBlockProtocolAdapter INSTANCE = new VineBlockProtocolAdapter();

    private static final Direction[] DIRECTIONS =
            Arrays.stream(Direction.values())
                    .filter(d -> d != Direction.DOWN)
                    .toArray(Direction[]::new);

    public VineBlockProtocolAdapter() {
    }

    private static boolean hasDirection(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof VineBlock)) {
            return false;
        }

        BooleanProperty booleanProperty = VineBlock.FACING_PROPERTIES.get(direction);
        return state.contains(booleanProperty) && state.get(booleanProperty);
    }

    @Override
    public void hao$setLoopCount(LoopContext ctx) {
        List<Integer> requireDirection = new ArrayList<>();
        ctx.data = requireDirection;

        for (int i = 0; i < DIRECTIONS.length; ++i) {
            Direction direction = DIRECTIONS[i];

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

    private static BlockState withDirection(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        BooleanProperty property = VineBlock.FACING_PROPERTIES.get(direction);
        boolean isVine = state.getBlock() instanceof VineBlock;

        if (isVine && state.get(property)) {
            return null;
        }

        if (!MultifaceBlock.canGrowOn(world, pos.offset(direction), direction)) {
            return null;
        }

        BlockState baseState = isVine ? state : Blocks.VINE.getDefaultState();
        return baseState.with(property, true);
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int index = extraProtocolValue & 0b0111;
        if (index >= DIRECTIONS.length) {
            return null;
        }

        World world = context.getWorld();
        BlockPos blockPos = EasyPlaceExtraProtocolHelper.getClickedPos(context);
        BlockState blockWorldState = world.getBlockState(blockPos);

        return withDirection(blockWorldState, world, blockPos, DIRECTIONS[index]);
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

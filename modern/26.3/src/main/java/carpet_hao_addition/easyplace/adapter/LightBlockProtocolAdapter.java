package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final LightBlockProtocolAdapter INSTANCE = new LightBlockProtocolAdapter();

    public LightBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int level = fromState.getValue(LightBlock.LEVEL);
        return level & 0b0000_1111;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int level = extraProtocolValue & 0b0000_1111;
        return fromState.setValue(LightBlock.LEVEL, level);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

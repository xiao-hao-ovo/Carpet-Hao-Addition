package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComposterBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final ComposterBlockProtocolAdapter INSTANCE = new ComposterBlockProtocolAdapter();

    public ComposterBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        return fromState.getValue(ComposterBlock.LEVEL) & 0b0000_1111;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        if (!true) return fromState;
        int level = Math.min(extraProtocolValue & 0b0000_1111, ComposterBlock.MAX_LEVEL);
        return fromState.setValue(ComposterBlock.LEVEL, level);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

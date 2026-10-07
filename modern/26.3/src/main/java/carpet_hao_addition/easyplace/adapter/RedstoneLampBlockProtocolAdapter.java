package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RedstoneLampBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final RedstoneLampBlockProtocolAdapter INSTANCE = new RedstoneLampBlockProtocolAdapter();

    public RedstoneLampBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isLit = fromState.getValue(RedstoneLampBlock.LIT);
        int bits = (isLit ? 0b0001_0000 : 0b0000_0000);
        return protocolValue | bits;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isLit = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
        return fromState.setValue(RedstoneLampBlock.LIT, isLit);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

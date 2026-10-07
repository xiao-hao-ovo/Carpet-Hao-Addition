package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JigsawBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final JigsawBlockProtocolAdapter INSTANCE = new JigsawBlockProtocolAdapter();

    public JigsawBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int orientationOrdinal = fromState.getValue(JigsawBlock.ORIENTATION).ordinal();
        return protocolValue | ((orientationOrdinal + 1) & 0b0000_1111);
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int orientationOrdinal = Math.floorMod((extraProtocolValue & 0b0000_1111) - 1, 12);
        return fromState.setValue(JigsawBlock.ORIENTATION, FrontAndTop.values()[orientationOrdinal]);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

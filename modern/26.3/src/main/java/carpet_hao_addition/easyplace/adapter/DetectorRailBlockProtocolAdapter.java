package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DetectorRailBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final DetectorRailBlockProtocolAdapter INSTANCE = new DetectorRailBlockProtocolAdapter();

    public DetectorRailBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isPower = fromState.getValue(DetectorRailBlock.POWERED);
        return isPower ? 0b0001 : 0b0000;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isPower = (extraProtocolValue & 0b0001) == 0b0001;
        return fromState.setValue(DetectorRailBlock.POWERED, isPower);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

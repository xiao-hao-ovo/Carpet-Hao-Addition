package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DaylightDetectorBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final DaylightDetectorBlockProtocolAdapter INSTANCE = new DaylightDetectorBlockProtocolAdapter();

    public DaylightDetectorBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isInverted = fromState.getValue(DaylightDetectorBlock.INVERTED);
        return isInverted ? 0b0001 : 0b0000;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isInverted = (extraProtocolValue & 0b0001) == 0b0001;
        return fromState.setValue(DaylightDetectorBlock.INVERTED, isInverted);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

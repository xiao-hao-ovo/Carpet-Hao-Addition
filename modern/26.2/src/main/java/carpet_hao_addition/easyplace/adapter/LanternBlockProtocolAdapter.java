package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LanternBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final LanternBlockProtocolAdapter INSTANCE = new LanternBlockProtocolAdapter();

    public LanternBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        boolean isHanging = fromState.getValue(LanternBlock.HANGING);
        return isHanging ? 0b0001 : 0b0000;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        boolean isHanging = (extraProtocolValue & 0b0001) == 0b0001;
        return fromState.setValue(LanternBlock.HANGING, isHanging);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

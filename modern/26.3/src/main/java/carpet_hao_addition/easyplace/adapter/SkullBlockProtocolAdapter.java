package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkullBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final SkullBlockProtocolAdapter INSTANCE = new SkullBlockProtocolAdapter();

    public SkullBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int rotation = fromState.getValue(SkullBlock.ROTATION);
        return (protocolValue & 0b1111_0000) | ((rotation + 1) & 0b0000_1111);
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int rotation = (extraProtocolValue & 0b0000_1111) - 1;
        if (rotation < 0) rotation = 0;
        return fromState.setValue(SkullBlock.ROTATION, rotation);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}


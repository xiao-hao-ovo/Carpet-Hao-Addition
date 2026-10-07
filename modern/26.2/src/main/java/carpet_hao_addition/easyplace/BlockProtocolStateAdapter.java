package carpet_hao_addition.easyplace;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BlockProtocolStateAdapter {
    int hao$toProtocolValue(int protocolValue, BlockState fromState);

    @Nullable
    BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context);

    @NotNull
    ProtocolType hao$getProtocolType();

    enum ProtocolType {
        ADDED,
        REPLACE,
    }
}

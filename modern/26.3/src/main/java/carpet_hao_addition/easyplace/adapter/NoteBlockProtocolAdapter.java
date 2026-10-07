package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoteBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final NoteBlockProtocolAdapter INSTANCE = new NoteBlockProtocolAdapter();

    public NoteBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int note = fromState.getValue(NoteBlock.NOTE);
        return note & 0b0001_1111;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        int note = (extraProtocolValue & 0b0001_1111) % 25;
        return fromState.setValue(NoteBlock.NOTE, note);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

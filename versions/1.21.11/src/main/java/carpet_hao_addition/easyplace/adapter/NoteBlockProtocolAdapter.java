package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.BlockState;

public class NoteBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final NoteBlockProtocolAdapter INSTANCE = new NoteBlockProtocolAdapter();

    public NoteBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int note = fromState.get(NoteBlock.NOTE);
        return note & 0b0001_1111;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int note = (extraProtocolValue & 0b0001_1111) % 25;
        return fromState.with(NoteBlock.NOTE, note);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

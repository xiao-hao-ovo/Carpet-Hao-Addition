package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** 比较器:协议里带朝向和模式(比较 / 减法)。 */
public class ComparatorBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final ComparatorBlockProtocolAdapter INSTANCE = new ComparatorBlockProtocolAdapter();

    public ComparatorBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        Direction facing = fromState.get(Properties.HORIZONTAL_FACING);
        int facingBits = switch (facing) {
            case NORTH -> 0b00;
            case EAST -> 0b01;
            case SOUTH -> 0b10;
            case WEST -> 0b11;
            default -> 0b00;
        };
        boolean isSubtract = fromState.get(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT;
        int modeBits = isSubtract ? 0b0100 : 0;
        return (protocolValue & ~0b1111) | facingBits | modeBits;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int facingBits = extraProtocolValue & 0b0011;
        Direction facing = switch (facingBits) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
        ComparatorMode mode = (extraProtocolValue & 0b0100) != 0
                ? ComparatorMode.SUBTRACT
                : ComparatorMode.COMPARE;

        return fromState
                .with(Properties.HORIZONTAL_FACING, facing)
                .with(ComparatorBlock.MODE, mode);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

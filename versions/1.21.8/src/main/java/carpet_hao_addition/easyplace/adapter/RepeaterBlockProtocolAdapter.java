package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * 中继器:协议里带朝向、档位(DELAY 1~4)和锁定状态。
 * <p>
 * 没有它的时候,轻松放置只能拿到原版 {@code getPlacementState} 给的默认值,DELAY 永远落在 1 档
 * —— 表现就是"投影里是 3 档的中继器,放出来变 1 档"。
 */
public class RepeaterBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final RepeaterBlockProtocolAdapter INSTANCE = new RepeaterBlockProtocolAdapter();

    public RepeaterBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        // bit0-1: 朝向(水平 4 向, ordinal-2);bit2-3: 档位(DELAY-1);bit4: 是否锁定;bit5: 锁定时是否通电。
        int bits = (fromState.get(Properties.HORIZONTAL_FACING).ordinal() - 2) & 0b0000_0011;
        bits |= ((fromState.get(RepeaterBlock.DELAY) - 1) & 0b0000_0011) << 2;

        boolean locked = fromState.get(RepeaterBlock.LOCKED);
        if (locked) {
            bits |= 0b0001_0000;
            if (fromState.get(Properties.POWERED)) {
                bits |= 0b0010_0000;
            }
        }
        return bits;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int facingIndex = (extraProtocolValue & 0b0000_0011) + 2;
        int delay = ((extraProtocolValue >>> 2) & 0b0000_0011) + 1;

        boolean locked = (extraProtocolValue & 0b0001_0000) != 0;
        BlockState newState = fromState
                .with(Properties.HORIZONTAL_FACING, Direction.values()[facingIndex])
                .with(RepeaterBlock.DELAY, delay)
                .with(RepeaterBlock.LOCKED, locked);

        if (locked) {
            newState = newState.with(Properties.POWERED, (extraProtocolValue & 0b0010_0000) != 0);
        }
        return newState;
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * 活塞:协议里带朝向和"是否已伸出"。
 * <p>
 * 1.21.x 的类名是 {@link PistonBlock}(26.x 叫 {@code PistonBaseBlock})。
 */
public class PistonBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final PistonBlockProtocolAdapter INSTANCE = new PistonBlockProtocolAdapter();

    public PistonBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        int bits = protocolValue;
        bits |= ((fromState.get(Properties.FACING).ordinal() + 1) & 0b0000_0111) << 5;
        return bits;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        boolean isExtended = (extraProtocolValue & 0b0001_0000) == 0b0001_0000;
        int facingIndex = ((extraProtocolValue >>> 5) & 0b0000_0111) - 1;
        if (facingIndex < 0) {
            facingIndex = 0;
        }
        Direction facing = Direction.values()[facingIndex % 6];

        if (isExtended) {
            BetterEasyPlaceProtocolHandler.setPlaceFlag(
                    BetterEasyPlaceProtocolHandler.EASY_PLACE_PISTON_NO_UPDATE);
            if ((extraProtocolValue & 0b1_0000_0000) != 0) {
                BetterEasyPlaceProtocolHandler.setPlaceFlag(
                        BetterEasyPlaceProtocolHandler.EASY_PLACE_PISTON_PLACE_HEAD);
            }
        }

        return fromState
                .with(Properties.EXTENDED, isExtended)
                .with(Properties.FACING, facing);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

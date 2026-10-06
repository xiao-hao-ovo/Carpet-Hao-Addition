package carpet_hao_addition.easyplace.adapter;

import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.CoralWallFanBlock;
import net.minecraft.block.BlockState;

public class CoralWallFanBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final CoralWallFanBlockProtocolAdapter INSTANCE = new CoralWallFanBlockProtocolAdapter();

    public CoralWallFanBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        // 不硬编码具体 property: 活珊瑚墙扇用 AbstractCoralWallFanBlock.FACING,
        // 而【失活】珊瑚墙扇(DeadCoralWallFanBlock)有它自己的 FACING —— 两者不是同一个类。
        // 取"第一个方向属性"对两者都成立。
        Property<Direction> facingProperty = EasyPlaceExtraProtocolHelper.getFirstDirectionProperty(fromState);
        if (facingProperty == null) {
            return protocolValue;
        }
        return (fromState.get(facingProperty).ordinal() - 2) & 0b0000_0011;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        Property<Direction> facingProperty = EasyPlaceExtraProtocolHelper.getFirstDirectionProperty(fromState);
        if (facingProperty == null) {
            return fromState;
        }
        int facingIndex = (extraProtocolValue & 0b0000_0011) + 2;
        Direction facing = Direction.values()[facingIndex];
        return fromState.with(facingProperty, facing);
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

package carpet_hao_addition.easyplace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class EasyPlaceExtraProtocolHelper {
    public static final int WATERLOGGED_BIT = 1 << 16;

    public static boolean isProtocol(double relativeHitDim) {
        return relativeHitDim >= (double) 2.0F;
    }

    /**
     * 原始点击位置 —— Mojang 的 {@code BlockPlaceContext#getClickedPos()} 本身就是这个语义,
     * 这里只做一层同名包装, 让从 1.21.8 移植过来的 mixin 不必改写调用点。
     */
    public static BlockPos getClickedPos(net.minecraft.world.item.context.BlockPlaceContext context) {
        return context.getClickedPos();
    }

    public static boolean isExtraProtocol(int protocolValue) {
        return (protocolValue & 0b0000_1000) == 0b0000_1000;
    }

    public static double getRelativeHitX(Vec3 hitPos, BlockPos blockPos) {
        return hitPos.x - (double) blockPos.getX();
    }

    public static double getRelativeHitZ(Vec3 hitPos, BlockPos blockPos) {
        return hitPos.z - (double) blockPos.getZ();
    }

    public static int decodeProtocolValueFromHitDim(double relativeHitDim) {
        return ((int) relativeHitDim - 2) >>> 1;
    }

    public static double encodeProtocolValueToHitDim(double relativeHitDim, int protocolValue) {
        return relativeHitDim + (double) ((protocolValue << 1) + 2);
    }

    public static int extraProtocolValueToRawProtocolValue(int protocolValue) {
        return ((protocolValue & 0b1111_0000) >>> 1) | (protocolValue & 0b0000_0111);
    }

    public static int rawProtocolValueToExtraProtocolValue(int protocolValue) {
        return ((protocolValue & 0b0111_1000) << 1) | (protocolValue & 0b0000_0111) | 0b0000_1000;
    }

    public static int addExtraProtocolBit(int protocolValue) {
        return protocolValue | 0b0000_1000;
    }

    public static int removeExtraProtocolBit(int protocolValue) {
        return protocolValue & ~((int) 0b0000_1000);
    }

    public static Vec3 encodeProtocolValueToHitVecZ(int protocolAdditionValue, Vec3 hitVec) {
        return new Vec3(hitVec.x, hitVec.y, encodeProtocolValueToHitDim(hitVec.z, protocolAdditionValue));
    }

    public static @Nullable Property<Direction> getFirstDirectionProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (Direction.class.isAssignableFrom(property.getValueClass())) {
                @SuppressWarnings("unchecked")
                Property<Direction> directionProperty = (Property<Direction>) property;
                return directionProperty;
            }
        }
        return null;
    }

    public static int waterloggedBit(BlockState state) {
        if ("false".equals("false")) {
            return 0;
        }
        if (!(state.getBlock() instanceof SimpleWaterloggedBlock)) {
            return 0;
        }
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return 0;
        }
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? EasyPlaceExtraProtocolHelper.WATERLOGGED_BIT : 0;
    }
}

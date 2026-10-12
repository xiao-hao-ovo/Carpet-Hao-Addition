package carpet_hao_addition.easyplace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 多阶段方块(雪层/海泡菜)补放的暂存区。
 * <p>
 * 编码端(Litematica 的 EasyPlaceUtils)与补放端(WorldUtils)是两个不同的类，
 * 对应的 mixin 也必须是两个，没法共享 @Unique 字段，所以状态放这里。
 */
public final class PendingMultiStage {
    private static BlockPos pos = null;
    private static BlockState schematic = null;
    private static Vec3 hitVec = null;

    private PendingMultiStage() {
    }

    public static void note(BlockPos p, BlockState state, Vec3 vec) {
        pos = p;
        schematic = state;
        hitVec = vec;
    }

    /** 取出并清空（一次性）。 */
    public static BlockPos pos() {
        BlockPos p = pos;
        pos = null;
        return p;
    }

    public static BlockState schematic() {
        BlockState s = schematic;
        schematic = null;
        return s;
    }

    public static Vec3 hitVec() {
        Vec3 v = hitVec;
        hitVec = null;
        return v;
    }

    public static void clear() {
        pos = null;
        schematic = null;
        hitVec = null;
    }
}

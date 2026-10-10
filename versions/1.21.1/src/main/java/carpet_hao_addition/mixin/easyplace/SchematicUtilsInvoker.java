package carpet_hao_addition.mixin.easyplace;

import fi.dy.masa.litematica.util.EasyPlaceUtils;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(EasyPlaceUtils.class)
public interface SchematicUtilsInvoker {
    @Invoker("cacheEasyPlacePosition")
    static void invokeCacheEasyPlacePosition(BlockPos pos) {}
}

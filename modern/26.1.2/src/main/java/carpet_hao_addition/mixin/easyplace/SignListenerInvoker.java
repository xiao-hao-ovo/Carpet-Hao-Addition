package carpet_hao_addition.mixin.easyplace;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 鏆撮湶 {@link SignBlockEntity} 鐨?private {@code markUpdated()}銆?*/
@Mixin(SignBlockEntity.class)
public interface SignListenerInvoker {
    @Invoker("markUpdated")
    void hao$callMarkUpdated();
}

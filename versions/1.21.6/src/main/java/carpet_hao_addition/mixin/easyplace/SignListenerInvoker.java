package carpet_hao_addition.mixin.easyplace;

import net.minecraft.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 暴露 {@link SignBlockEntity} 的 private {@code updateListeners()}。 */
@Mixin(SignBlockEntity.class)
public interface SignListenerInvoker {
    @Invoker("updateListeners")
    void hao$callUpdateListeners();
}

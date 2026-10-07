package carpet_hao_addition.mixin.easyplace;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 暴露 {@link SignBlockEntity} 的 private {@code markUpdated()}(1.21.8 的 Yarn 名是 updateListeners)。 */
@Mixin(SignBlockEntity.class)
public interface SignBlockEntityInvoker {
    @Invoker("markUpdated")
    void hao$callUpdateListeners();
}

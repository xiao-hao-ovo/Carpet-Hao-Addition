package carpet_hao_addition.easyplace;

import org.spongepowered.asm.mixin.Unique;

public interface ISignBlockEntity {
    @Unique
    boolean hao$isPendingWaxed();

    @Unique
    void hao$setPendingWaxed(boolean pending);
}


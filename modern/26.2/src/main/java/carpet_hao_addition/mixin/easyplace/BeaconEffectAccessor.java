package carpet_hao_addition.mixin.easyplace;

import net.minecraft.core.Holder; //?>= 1.20.5
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface BeaconEffectAccessor {
    @Accessor("primary")
    Holder<MobEffect> hao$getPrimaryPower();

    @Accessor("secondary")
    Holder<MobEffect> hao$getSecondaryPower();
}

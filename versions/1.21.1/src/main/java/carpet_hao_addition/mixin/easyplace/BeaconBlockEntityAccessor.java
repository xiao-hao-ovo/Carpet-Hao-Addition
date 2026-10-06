package carpet_hao_addition.mixin.easyplace;

import net.minecraft.registry.entry.RegistryEntry; //?>= 1.20.5
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface BeaconBlockEntityAccessor {
    @Accessor("primary")
    RegistryEntry<StatusEffect> hao$getPrimaryPower();

    @Accessor("secondary")
    RegistryEntry<StatusEffect> hao$getSecondaryPower();
}

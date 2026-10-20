package carpet_hao_addition.mixin.easyplace;

import net.minecraft.registry.entry.RegistryEntry; //?>= 1.20.5
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读信标方块实体里 private 的 primary / secondary 效果，供 {@code BeaconItemData} 编码用。
 */
@Mixin(BeaconBlockEntity.class)
public interface BeaconEffectAccessor {
		@Accessor("primary")
		RegistryEntry<StatusEffect> hao$getPrimaryPower();

		@Accessor("secondary")
		RegistryEntry<StatusEffect> hao$getSecondaryPower();
}

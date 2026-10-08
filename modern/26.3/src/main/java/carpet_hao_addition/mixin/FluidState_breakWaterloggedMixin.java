package carpet_hao_addition.mixin;

import carpet_hao_addition.WorldEaterProMaxSettings;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 增强世界吞噬者(ProMax 的核心):让**含水方块**也能被炸。
 * <p>
 * 爆炸算抗性时用的是 {@code max(方块抗性, 流体抗性)},而水的流体抗性是 100 —— 这就是原版
 * "含水方块防爆"的来源(台阶、珊瑚扇、栅栏……一旦含水就等于 100 抗性,TNT 完全炸不动)。
 * <p>
 * 与其去改各个 {@code ExplosionDamageCalculator} 实现(Simple / EntityBased 都覆写了),
 * 不如直接从源头下手:规则开启时把**水的**流体抗性归零,{@code max(方块抗性, 0)} 自然就等于
 * 方块自身抗性。这样不管爆炸用的是哪个计算器实现都成立。
 */
@Mixin(FluidState.class)
public abstract class FluidState_breakWaterloggedMixin {
	@Inject(method = "getExplosionResistance", at = @At("RETURN"), cancellable = true)
	private void hao$ignoreWaterResistance(CallbackInfoReturnable<Float> cir) {
		if (!WorldEaterProMaxSettings.isEnabled()) {
			return;
		}
		if (cir.getReturnValue() <= 0.0F) {
			return;
		}
		FluidState self = (FluidState) (Object) this;
		if (self.is(FluidTags.WATER)) {
			cir.setReturnValue(0.0F);
		}
	}
}

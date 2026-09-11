package carpet_hao_addition.mixin;

import carpet_hao_addition.DirectDropsSettings;
import carpet_hao_addition.directdrops.DirectDropContext;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * directEntityDrops:生物(含盔甲架)被玩家击杀时标记掉落归属。
 * <p>
 * 走 {@code LivingEntity.dropLoot} —— 所有生物的死亡掉落都汇聚到这里,覆盖近战、箭矢、
 * 三叉戟、爆炸等各类致死方式:只要伤害来源能追溯到玩家,掉落就归该玩家。
 * <p>
 * <b>版本差异</b>:1.21 / 1.21.1 的签名是 {@code dropLoot(DamageSource, boolean)};
 * 从 1.21.2 起变成 {@code dropLoot(ServerWorld, DamageSource, boolean)}。这里按本层
 * 实际签名写死了完整描述符,避免运行时才暴露 InvalidInjectionException。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_directEntityDropsMixin {
	@Inject(method = "dropLoot(Lnet/minecraft/entity/damage/DamageSource;Z)V", at = @At("HEAD"))
	private void hao$beginDirectEntityDrops(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
		ServerPlayerEntity owner = null;
		if (DirectDropsSettings.entityDropsEnabled() && source != null
				&& source.getAttacker() instanceof ServerPlayerEntity attacker) {
			owner = attacker;
		}
		DirectDropContext.push(owner);
	}

	// 无条件弹栈:与 HEAD 严格配对。
	@Inject(method = "dropLoot(Lnet/minecraft/entity/damage/DamageSource;Z)V", at = @At("RETURN"))
	private void hao$endDirectEntityDrops(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
		DirectDropContext.pop();
	}
}

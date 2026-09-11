package carpet_hao_addition.mixin;

import carpet_hao_addition.DirectDropsSettings;
import carpet_hao_addition.directdrops.DirectDropContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * directEntityDrops:生物(含盔甲架)被玩家击杀时标记掉落归属。
 * <p>
 * 走 {@code dropAllDeathLoot} —— 所有生物的死亡掉落都汇聚到这里,覆盖近战、箭矢、
 * 三叉戟、爆炸等各类致死方式:只要伤害来源能追溯到玩家,掉落就归该玩家。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_directEntityDropsMixin {
	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
	private void hao$beginDirectEntityDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
		ServerPlayer owner = null;
		if (DirectDropsSettings.entityDropsEnabled() && source != null
				&& source.getEntity() instanceof ServerPlayer attacker) {
			owner = attacker;
		}
		DirectDropContext.push(owner);
	}

	// 无条件弹栈:与 HEAD 严格配对。
	@Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
	private void hao$endDirectEntityDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
		DirectDropContext.pop();
	}
}

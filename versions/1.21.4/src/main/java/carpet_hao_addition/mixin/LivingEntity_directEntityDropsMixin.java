package carpet_hao_addition.mixin;

import carpet_hao_addition.DirectDropsSettings;
import carpet_hao_addition.directdrops.DirectDropContext;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * directEntityDrops:生物(含盔甲架)被玩家击杀时标记掉落归属。
 * <p>
 * 走 {@link LivingEntity#dropLoot(ServerWorld, DamageSource)} —— 所有生物的死亡掉落都
 * 汇聚到这里,覆盖近战、箭矢、三叉戟、爆炸等各类致死方式:只要伤害来源能追溯到玩家,
 * 掉落就归该玩家。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_directEntityDropsMixin {
	@Inject(method = "dropLoot", at = @At("HEAD"))
	private void hao$beginDirectEntityDrops(ServerWorld world, DamageSource source, CallbackInfo ci) {
		ServerPlayerEntity owner = null;
		if (DirectDropsSettings.entityDropsEnabled() && source != null
				&& source.getAttacker() instanceof ServerPlayerEntity attacker) {
			owner = attacker;
		}
		DirectDropContext.push(owner);
	}

	// 无条件弹栈:与 HEAD 严格配对。
	@Inject(method = "dropLoot", at = @At("RETURN"))
	private void hao$endDirectEntityDrops(ServerWorld world, DamageSource source, CallbackInfo ci) {
		DirectDropContext.pop();
	}
}

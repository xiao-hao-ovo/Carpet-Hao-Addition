package carpet_hao_addition.mixin;

import carpet_hao_addition.DirectDropsSettings;
import carpet_hao_addition.directdrops.DirectDropContext;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * directEntityDrops:玩家近战攻击时标记掉落归属,覆盖非生物实体。
 * <p>
 * 矿车、船、画、物品展示框这类实体不走 {@code LivingEntity.dropLoot},而是在各自被
 * 「打坏」时直接掉落。它们全部经由玩家近战攻击触发,所以在 {@code attack} 的调用栈内
 * 标记归属,即可覆盖这些实体的掉落。
 * <p>
 * 与 {@link LivingEntity_directEntityDropsMixin} 叠加时,栈结构保证两层归属互不干扰。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_directEntityDropsMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void hao$beginAttackDrops(Entity target, CallbackInfo ci) {
		ServerPlayerEntity owner = null;
		if (DirectDropsSettings.entityDropsEnabled() && (Object) this instanceof ServerPlayerEntity serverPlayer) {
			owner = serverPlayer;
		}
		DirectDropContext.push(owner);
	}

	// 无条件弹栈:与 HEAD 严格配对。
	@Inject(method = "attack", at = @At("RETURN"))
	private void hao$endAttackDrops(Entity target, CallbackInfo ci) {
		DirectDropContext.pop();
	}
}

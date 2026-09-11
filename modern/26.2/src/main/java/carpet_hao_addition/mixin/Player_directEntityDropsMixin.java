package carpet_hao_addition.mixin;

import carpet_hao_addition.DirectDropsSettings;
import carpet_hao_addition.directdrops.DirectDropContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * directEntityDrops:玩家近战攻击时标记掉落归属,覆盖非生物实体。
 * <p>
 * 矿车、船、画、物品展示框这类实体不走 {@code dropAllDeathLoot},而是在各自被
 * 「打坏」时直接掉落。它们全部经由玩家近战攻击触发,所以在 {@code attack} 的调用栈内
 * 标记归属,即可覆盖这些实体的掉落。
 * <p>
 * 与 {@link LivingEntity_directEntityDropsMixin} 叠加时,栈结构保证两层归属互不干扰。
 */
@Mixin(Player.class)
public abstract class Player_directEntityDropsMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void hao$beginAttackDrops(Entity target, CallbackInfo ci) {
		ServerPlayer owner = null;
		if (DirectDropsSettings.entityDropsEnabled() && (Object) this instanceof ServerPlayer serverPlayer) {
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

package carpet_hao_addition.mixin;

import carpet_hao_addition.AutoMendingHandler;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * haoAutoMending(移植于 Carpet WuHu Addition):在 {@code ServerPlayer.tick} 的末尾挂上检查
 * (每 20 tick 实际执行一次)。
 * <p>
 * 注入的是 {@code ServerPlayer} **自己声明**的 {@code tick()}(它覆写了 {@code Player.tick}),
 * 所以能注入成功 —— {@code @Inject} 只认目标类自身声明的方法。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_autoMendingMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void hao$autoMending(CallbackInfo ci) {
		AutoMendingHandler.tick((ServerPlayer) (Object) this);
	}
}

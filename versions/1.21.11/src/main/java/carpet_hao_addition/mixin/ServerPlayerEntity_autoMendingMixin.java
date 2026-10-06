package carpet_hao_addition.mixin;

import carpet_hao_addition.AutoMendingHandler;

import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * autoMending_new(移植于 Carpet WuHu Addition):在 {@code ServerPlayerEntity.tick} 的末尾挂上检查
 * (每 20 tick 实际执行一次)。
 * <p>
 * 注入的是 {@code ServerPlayerEntity} **自己声明**的 {@code tick()}(它覆写了
 * {@code PlayerEntity.tick}),所以能注入成功 —— {@code @Inject} 只认目标类自身声明的方法。
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntity_autoMendingMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void hao$autoMending(CallbackInfo ci) {
		AutoMendingHandler.tick((ServerPlayerEntity) (Object) this);
	}
}

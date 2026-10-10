package carpet_hao_addition.mixin;

import carpet_hao_addition.RocketShulkerHandler;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务端启动时注册 haoRocketShulker 的每 tick 检查(副手烟花自动补给)。
 * <p>
 * 与 haoProjectionWaterlogged 一样用 mixin 而不是 entrypoint:该 handler 只存在于 1.21.8 这一层,
 * 而 {@code fabric.mod.json} 是各版本层共用的,不能在里面声明只属于个别层的入口类。
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_rocketShulkerMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void hao$registerRocketShulkerTick(CallbackInfo ci) {
		RocketShulkerHandler.registerTickHandler();
	}
}

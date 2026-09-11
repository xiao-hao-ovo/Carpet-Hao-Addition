package carpet_hao_addition.mixin;

import carpet_hao_addition.PlaceWaterloggedHandler;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务端启动时注册 easyPlaceWaterlogged 的 payload 类型与接收端。
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_easyPlaceWaterloggedMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void hao$registerWaterloggedPayload(CallbackInfo ci) {
		PlaceWaterloggedHandler.registerServerReceiver();
	}
}

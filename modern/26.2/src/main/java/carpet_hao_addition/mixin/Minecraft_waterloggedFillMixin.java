package carpet_hao_addition.mixin;

import carpet_hao_addition.ClientWaterlogNotifier;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * haoProjectionWaterlogged(客户端)的右键入口:挂在 {@code Minecraft.startUseItem} 的 HEAD 上。
 * <p>移植自 1.21.8 的 {@code MinecraftClient_waterloggedFillMixin}。</p>
 */
@Mixin(Minecraft.class)
public abstract class Minecraft_waterloggedFillMixin {
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void hao$waterlogBeforeUseItem(CallbackInfo ci) {
		if (!FabricLoader.getInstance().isModLoaded("litematica")) {
			return;
		}
		if (ClientWaterlogNotifier.onUseItem((Minecraft) (Object) this)) {
			ci.cancel();
		}
	}
}
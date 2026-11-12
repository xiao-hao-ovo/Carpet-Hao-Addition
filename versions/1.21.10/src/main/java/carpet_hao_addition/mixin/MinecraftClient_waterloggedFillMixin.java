package carpet_hao_addition.mixin;

import carpet_hao_addition.waterlogged.ClientWaterlogNotifier;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.MinecraftClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * haoProjectionWaterlogged(客户端)的右键入口:挂在 {@code MinecraftClient.doItemUse} 的 HEAD 上。
 * <p>
 * 为什么还要这一条:0.23.7 的移植版把 {@code EasyPlaceUtils.shouldDoEasyPlaceActions()} 写成了恒
 * {@code false},{@code WorldUtils.doEasyPlaceAction} 在不少操作里根本不会被调用,只挂那条链路时
 * 「先放干的铁活板门、再蹲下右键补水」一个包都发不出去。
 * <p>
 * 但规则的前提条件仍是「投影轻松放置必须开着」:{@link ClientWaterlogNotifier#onUseItem} 一进来就先查
 * {@code Configs.Generic.EASY_PLACE_MODE},关掉时这里不会拦任何操作。
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClient_waterloggedFillMixin {
	@Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
	private void hao$waterlogBeforeUseItem(CallbackInfo ci) {
		if (!FabricLoader.getInstance().isModLoaded("litematica")) {
			return;
		}
		if (ClientWaterlogNotifier.onUseItem((MinecraftClient) (Object) this)) {
			ci.cancel();
		}
	}
}

package carpet_hao_addition.mixin;

import carpet_hao_addition.waterlogged.ClientWaterlogNotifier;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * haoProjectionWaterlogged(客户端)的放置拦截:挂在 {@code ClientPlayerInteractionManager.interactBlock} 上。
 * <p>
 * 打印机(litematica-printer)在自己的 tick 里直接调 {@code interactBlock} 放置方块,不经过
 * {@code MinecraftClient.doItemUse},所以只在 doItemUse 上 {@code ci.cancel()} 拦不住它。
 * {@code interactBlock} 是原版 / 打印机 / Sakura 移植各路放置最终都会经过的汇合点:
 * 当准星正对一个「纯补水」目标格时,把这次放置整段取消,别用手上的方块重复放出去。
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManager_waterloggedFillMixin {
	@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
	private void hao$blockWaterlogInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
			CallbackInfoReturnable<ActionResult> cir) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.crosshairTarget instanceof BlockHitResult cross
				&& ClientWaterlogNotifier.isPureRefillTarget(client, cross.getBlockPos())) {
			cir.setReturnValue(ActionResult.SUCCESS);
		}
	}
}

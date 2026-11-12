package carpet_hao_addition.mixin;

import carpet_hao_addition.ClientWaterlogNotifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * haoProjectionWaterlogged(客户端)的放置拦截:挂在 {@code MultiPlayerGameMode.useItemOn} 上。
 * <p>移植自 1.21.8 的 {@code ClientPlayerInteractionManager_waterloggedFillMixin}。</p>
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameMode_waterloggedFillMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void hao$blockWaterlogInteract(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		Minecraft client = Minecraft.getInstance();
		if (client.hitResult instanceof BlockHitResult cross
				&& ClientWaterlogNotifier.isPureRefillTarget(client, cross.getBlockPos())) {
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
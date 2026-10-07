package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import carpet_hao_addition.easyplace.ClientEasyPlaceProtocolHelper;
import carpet_hao_addition.easyplace.EasyPlacePendingPlacement;
import fi.dy.masa.litematica.util.EasyPlaceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 协议 hitVec 钩子:把 litematica 交给原版的命中点换成我们编码过的(前几位是协议值)。
 * <p>
 * 26.x 起 litematica 把这两个钩子从 {@code WorldUtils} 移到了 {@code EasyPlaceUtils},
 * 所以这里对 {@code EasyPlaceUtils} 注入;缓存写在 {@link EasyPlacePendingPlacement} 里共享给
 * {@link WorldUtilsMixin}(那边的 {@code doEasyPlaceAction} 返回后要用)。
 */
@Mixin(EasyPlaceUtils.class)
public abstract class EasyPlaceUtilsMixin {
	@Inject(
			method = "applyCarpetProtocolHitVec",
			at = @At(value = "RETURN"),
			require = 0,
			cancellable = true)
	private static void hao_replaceHitPos(BlockPos pos, BlockState state, Vec3 hitVecIn, CallbackInfoReturnable<Vec3> cir) {
		if (BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
			Vec3 out = ClientEasyPlaceProtocolHelper.encodeHitPosItemData(cir.getReturnValue(), pos, state);
			System.out.println("[hao-easyplace] [CarpetVec] pos=" + pos + " in=" + cir.getReturnValue() + " out=" + out);
			cir.setReturnValue(out);
		}
	}

	@Inject(
			method = "applyPlacementProtocolV3",
			at = @At(value = "RETURN"),
			require = 0,
			cancellable = true)
	private static void hao_replaceHitPosV3(BlockPos pos, BlockState state, Vec3 hitVecIn, CallbackInfoReturnable<Vec3> cir) {
		if (!BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
			return;
		}
		Vec3 encoded = ClientEasyPlaceProtocolHelper.encodeHitPosItemData(cir.getReturnValue(), pos, state);
		System.out.println("[hao-easyplace] [V3] pos=" + pos + " state=" + state
				+ " litematica返回=" + cir.getReturnValue() + " 我们编码后=" + encoded);
		EasyPlacePendingPlacement.pos = pos;
		EasyPlacePendingPlacement.schematic = state;
		EasyPlacePendingPlacement.hitVec = encoded;
		cir.setReturnValue(encoded);
	}
}

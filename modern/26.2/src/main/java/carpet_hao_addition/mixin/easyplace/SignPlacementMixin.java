package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * haoProjectionPlacement：轻松放置标牌时，让 {@code updateCustomBlockEntityTag} 的【返回值】为 false。
 *
 * <p>1.21.8 里对应的是 {@code SignItem#postPlacement}（Yarn 名），26.x 的对应方法是实例版
 * {@code updateCustomBlockEntityTag(BlockPos, Level, Player, ItemStack, BlockState)} —— 两者同一个位置、
 * 同一个语义：返回 true 会让原版认为「这次放置已经成功处理」，照投影连续铺标牌时就会打断放置节奏、
 * 出现漏放。</p>
 */
@Mixin(SignItem.class)
public class SignPlacementMixin {
	@Inject(
			method = "updateCustomBlockEntityTag(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Z",
			at = @At("RETURN"),
			cancellable = true)
	private void hao$suppressSuccess(BlockPos pos, Level world, Player player, ItemStack stack,
			BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (ProjectionPlacement.isProjected()
				&& ProjectionPlacement.placeTarget().equals(pos)
				&& world.getBlockState(pos).getBlock() instanceof SignBlock) {
			cir.setReturnValue(false);
		}
	}
}

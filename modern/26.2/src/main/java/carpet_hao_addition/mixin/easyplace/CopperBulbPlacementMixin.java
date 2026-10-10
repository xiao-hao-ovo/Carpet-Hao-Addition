package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * easy place 放铜灯(1.21.x 的 Yarn 名是 {@link CopperBulbBlock})时不让它翻转 lit 状态。
 * <p>
 * 1.21.x 的方法名与 26.x 不同:{@code onPlace} / {@code neighborChanged} 在 Yarn 里分别叫
 * {@code onBlockAdded} / {@code neighborUpdate},而且 {@code CopperBulbBlock} 自己**都没覆写**
 * (它只覆写了 {@code update},26.x 里对应的方法叫 {@code checkAndFlip})。
 * {@code @Inject} 只认目标类自己声明的方法,所以只注入 {@code update} 这一处即可 ——
 * 放置(onBlockAdded)与邻居更新(neighborUpdate)最终都会走它。
 */
@Mixin(value = CopperBulbBlock.class, priority = 900)
public abstract class CopperBulbPlacementMixin {
	@Inject(method = "update", at = @At("HEAD"), cancellable = true)
	private void hao$cancelFlip(BlockState state, ServerLevel world, BlockPos pos, CallbackInfo ci) {
		if (ProjectionPlacement.isProjected()) {
			ci.cancel();
		}
	}
}

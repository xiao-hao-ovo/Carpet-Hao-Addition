package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * haoBedrockMines(基准层):基岩挖掘进度按黑曜石计算。
 * <p>
 * 在 calcBlockBreakingDelta 本体 HEAD 直接处理“基岩+规则开启→黑曜石”,保证
 * 不依赖外部扩展时一定可挖;另有 ServerPlayerInteractionManager_bedrockMinesMixin
 * 在调用点兜底(与 AMS customBlockHardness 并存时仍可挖)。
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class BlockState_bedrockCanBeMinedMixin {
	@Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true)
	private void hao$bedrockMinesLikeObsidian(PlayerEntity player, BlockView world, BlockPos pos,
			CallbackInfoReturnable<Float> cir) {
		AbstractBlock.AbstractBlockState state = (AbstractBlock.AbstractBlockState) (Object) this;
		if (!BedrockCanBeMinedSettings.isEnabled() || !state.isOf(Blocks.BEDROCK)) {
			return;
		}

		cir.setReturnValue(Blocks.OBSIDIAN.getDefaultState().calcBlockBreakingDelta(player, world, pos));
	}
}

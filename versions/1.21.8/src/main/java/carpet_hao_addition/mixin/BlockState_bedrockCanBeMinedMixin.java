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
 * haoBedrockMines(兜底层):基岩仍不可挖(delta&lt;0,即无人设置可挖硬度)时按黑曜石。
 * <p>
 * 用 TAIL 兜底而非 HEAD:不覆盖其它扩展(如 Carpet-AMS-Addition 的
 * commandCustomBlockHardness set bedrock 0/其它值)对基岩硬度的设置——若外部给了
 * 有效值(≥0)就尊重它;只有真正不可挖(原版 -1)时才按黑曜石兜底。
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class BlockState_bedrockCanBeMinedMixin {
	@Inject(method = "calcBlockBreakingDelta", at = @At("TAIL"), cancellable = true)
	private void hao$bedrockMinesLikeObsidian(PlayerEntity player, BlockView world, BlockPos pos,
			CallbackInfoReturnable<Float> cir) {
		AbstractBlock.AbstractBlockState state = (AbstractBlock.AbstractBlockState) (Object) this;
		if (!BedrockCanBeMinedSettings.isEnabled() || !state.isOf(Blocks.BEDROCK)) {
			return;
		}
		Float current = cir.getReturnValue();
		if (current != null && current < 0f) {
			cir.setReturnValue(Blocks.OBSIDIAN.getDefaultState().calcBlockBreakingDelta(player, world, pos));
		}
	}
}

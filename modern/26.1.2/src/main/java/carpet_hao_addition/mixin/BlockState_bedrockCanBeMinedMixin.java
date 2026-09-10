package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

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
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockState_bedrockCanBeMinedMixin {
	@Inject(method = "getDestroyProgress", at = @At("TAIL"), cancellable = true)
	private void hao$bedrockMinesLikeObsidian(Player player, BlockGetter world, BlockPos pos,
			CallbackInfoReturnable<Float> cir) {
		BlockBehaviour.BlockStateBase state = (BlockBehaviour.BlockStateBase) (Object) this;
		if (!BedrockCanBeMinedSettings.isEnabled() || !state.is(Blocks.BEDROCK)) {
			return;
		}
		Float current = cir.getReturnValue();
		if (current != null && current <= 0f) {
			cir.setReturnValue(Blocks.OBSIDIAN.defaultBlockState().getDestroyProgress(player, world, pos));
		}
	}
}

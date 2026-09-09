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
 * bedrockCanBeMined:基岩的挖掘进度按黑曜石计算。
 * <p>
 * 1.21.8 中基岩就是普通 Block(hardness=-1),无专属类;玩家挖掘推进统一走
 * AbstractBlock$AbstractBlockState.calcBlockBreakingDelta(所有方块状态继承)。
 * 规则开启且目标是基岩时,直接返回黑曜石的挖掘进度,使基岩可以按黑曜石的
 * 速度被挖碎(默认关闭时返回原版 -1,不可挖掘)。
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

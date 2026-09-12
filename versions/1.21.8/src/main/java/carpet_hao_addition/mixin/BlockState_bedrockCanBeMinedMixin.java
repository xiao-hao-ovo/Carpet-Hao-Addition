package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
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
 * 用 RETURN 兜底而非 HEAD:不覆盖其它扩展(如 Carpet-AMS-Addition 的
 * commandCustomBlockHardness set bedrock 0/其它值)对基岩硬度的设置——若外部给了
 * 有效值(≥0)就尊重它;只有真正不可挖(原版 -1)时才按黑曜石兜底。
 * <p>
 * <b>注意</b>:本注入依赖原方法能正常走到 return。若外部扩展(AMS 的
 * commandCustomBlockHardness 就是典型)在 HEAD 处取消该方法,这里会被整段跳过。
 * 因此真正保证"能挖"的是调用点包装:
 * <ul>
 *   <li>客户端 {@link ClientPlayerInteractionManager_bedrockMinesMixin}</li>
 *   <li>服务端 {@link ServerPlayerInteractionManager_bedrockMinesMixin}</li>
 * </ul>
 * 本层保留作为"没有任何外部自定义硬度"时的通用兜底(覆盖挖矿以外的其它调用方)。
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class BlockState_bedrockCanBeMinedMixin {
	@Inject(method = "calcBlockBreakingDelta", at = @At("RETURN"), cancellable = true)
	private void hao$bedrockMinesLikeObsidian(PlayerEntity player, BlockView world, BlockPos pos,
			CallbackInfoReturnable<Float> cir) {
		AbstractBlock.AbstractBlockState state = (AbstractBlock.AbstractBlockState) (Object) this;
		if (!BedrockCanBeMinedSettings.isEnabled() || !state.isOf(Blocks.BEDROCK)) {
			return;
		}
		Float current = cir.getReturnValue();
		if (current != null && current <= 0f) {
			cir.setReturnValue(hao$bedrockDelta((BlockState) (Object) state, player));
		}
	}

	/** 按固定硬度 50 计算挖掘进度(与"基岩可挖时的硬度"一致,不借用黑曜石状态)。 */
	private static float hao$bedrockDelta(BlockState state, PlayerEntity player) {
		int divisor = player.canHarvest(state) ? 30 : 100;
		return player.getBlockBreakingSpeed(state) / 50.0F / divisor;
	}
}

package carpet_hao_addition.mixin;

import carpet_hao_addition.EnhancedWorldEaterSettings;
import carpet_hao_addition.WorldEaterProMaxSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 两条"增强世界吞噬者"规则共用的抗性改写：
 * <ul>
 *   <li>{@code enhancedWorldEater}（AMS 原规则，1.21.8 专属移植）；</li>
 *   <li>{@code haoEnhancedWorldEaterProMax}（本仓库原有规则）。</li>
 * </ul>
 * 两条都开启时 AMS 规则优先；ProMax 额外的「含水方块可炸」由
 * {@code FluidState_breakWaterloggedMixin} 单独处理，不在这里。
 * <p>
 * 原抗性低于 17 的方块保持原版行为；黑名单（基岩、三种铁砧、末地传送门框架/传送门/折跃门）
 * 与 AMS 的 {@code BlockBlackList} 完全一致。
 */
@Mixin(Block.class)
public abstract class Block_explosionResistanceMixin {
	@Inject(method = "getBlastResistance", at = @At("RETURN"), cancellable = true)
	private void hao$capHighResistance(CallbackInfoReturnable<Float> cir) {
		if (cir.getReturnValue() < 17.0F) {
			return;
		}
		Block self = (Block) (Object) this;
		if (self == Blocks.WATER) {
			// 「炸水」开关:水源与流动水都是 Blocks.WATER,抗性 100;开启后按 0 算,
			// 配合 FluidState_breakWaterloggedMixin 把水的流体抗性归零,整格水就能被炸掉。
			if (EnhancedWorldEaterSettings.isWaterDestroyEnabled()) {
				cir.setReturnValue(0.0F);
			}
			return;
		}
		if (self == Blocks.LAVA) {
			// 岩浆不在「炸水」开关范围,保持原抗性。
			return;
		}
		if (self == Blocks.BEDROCK || self == Blocks.ANVIL || self == Blocks.CHIPPED_ANVIL
				|| self == Blocks.DAMAGED_ANVIL || self == Blocks.END_PORTAL_FRAME
				|| self == Blocks.END_PORTAL || self == Blocks.END_GATEWAY) {
			return;
		}
		if (EnhancedWorldEaterSettings.isEnabled()) {
			cir.setReturnValue(EnhancedWorldEaterSettings.blastResistance());
			return;
		}
		if (WorldEaterProMaxSettings.isEnabled()) {
			cir.setReturnValue(WorldEaterProMaxSettings.resistanceCap());
		}
	}
}
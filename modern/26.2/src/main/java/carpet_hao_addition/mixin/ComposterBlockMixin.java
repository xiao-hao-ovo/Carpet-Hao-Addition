package carpet_hao_addition.mixin;

import carpet_hao_addition.GoldenCarrotCompostSettings;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 金胡萝卜堆肥规则(goldenCarrotCompost)的行为接入点。
 * <p>
 * 原理:原版 ComposterBlock.onUseWithItem 会查询静态表
 * {@link ComposterBlock#ITEM_TO_LEVEL_INCREASE_CHANCE} 决定物品能否堆肥及成功率;
 * 规则开启时把金胡萝卜以 100% 概率登记进该表,之后完全走原版“正常可堆肥物品”的
 * 逻辑(addToComposter 加层、消耗一个、音效/事件,层满进入完成流程),无需复刻行为;
 * 规则关闭时在方法入口返回 PASS,金胡萝卜对堆肥桶与普通行为一致。
 */
@Mixin(ComposterBlock.class)
public class ComposterBlockMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void hao$goldenCarrotComposting(ItemStack stack, BlockState state, Level world, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!stack.is(Items.GOLDEN_CARROT)) {
			return;
		}

		if (!GoldenCarrotCompostSettings.isEnabled()) {
			// 规则关闭:不做任何堆肥,行为与普通金胡萝卜一致。
			cir.setReturnValue(InteractionResult.PASS);
			return;
		}

		// 规则开启:幂等地登记为 100% 可堆肥,随后交给原版正常堆肥流程处理。
		ComposterBlock.COMPOSTABLES.put(Items.GOLDEN_CARROT, 1.0F);
	}
}

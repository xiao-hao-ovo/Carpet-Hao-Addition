package carpet_hao_addition.mixin;

import carpet_hao_addition.ShulkerBoxContentHelper;
import carpet_hao_addition.UseDyeOnShulkerBoxSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * useDyeOnShulkerBox(染色那半):手持**染料**右键**潜影盒** → 染成该染料的颜色,
 * **盒内物品与自定义名称原样保留**,并消耗 1 个染料。
 * <p>
 * <b>为什么是 {@code @Mixin(Item.class)} 而不是 {@code DyeItem}:</b>1.21.2 起告示牌染色被重构成
 * {@code SignApplicator} 接口,{@code DyeItem} 因此**不再覆写** {@code useOn}(26.x 的 javap 里它
 * 只有 {@code interactLivingEntity} 与 {@code tryApplyToSign})。{@code @Inject} 只能注入目标类
 * **自身声明**的方法,Mixin 不会去父类找 —— 写 {@code @Mixin(DyeItem.class)} 会在运行时抛
 * {@code InvalidInjectionException: could not find any targets matching 'useOn'}。
 * 而 {@code Item.useOn} 是所有物品的默认实现,染料走的就是它,所以注入到 {@code Item}
 * 再用 {@code instanceof DyeItem} 过滤。HEAD 处不匹配就立刻 return,不影响其它物品。
 * <p>
 * <b>染料颜色怎么拿:</b>26.x 的 {@code DyeItem} 已经没有颜色访问器了,颜色是物品栈上的
 * {@code DataComponents.DYE} 组件,所以从 {@code UseOnContext.getItemInHand()} 上读。
 */
@Mixin(Item.class)
public abstract class DyeItem_useDyeOnShulkerBoxMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void hao$dyeShulkerBox(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!UseDyeOnShulkerBoxSettings.isEnabled()) {
			return;
		}
		if (!((Object) this instanceof DyeItem)) {
			return; // 只有染料才接管,其它物品交给原版
		}
		DyeColor target = context.getItemInHand().get(DataComponents.DYE);
		if (target == null) {
			return; // 拿不到颜色就当不是染料
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ShulkerBoxBlock shulkerBox)) {
			return;
		}
		if (shulkerBox.getColor() == target) {
			return; // 已经是这个颜色,交还原版
		}
		if (level instanceof ServerLevel serverLevel) {
			if (!ShulkerBoxContentHelper.recolor(serverLevel, pos, state,
					ShulkerBoxContentHelper.coloredShulkerBox(target))) {
				return;
			}
			context.getItemInHand().shrink(1);
		}
		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}

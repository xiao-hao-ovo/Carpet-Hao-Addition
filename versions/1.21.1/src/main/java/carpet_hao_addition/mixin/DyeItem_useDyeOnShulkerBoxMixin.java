package carpet_hao_addition.mixin;

import carpet_hao_addition.ShulkerBoxContentHelper;
import carpet_hao_addition.UseDyeOnShulkerBoxSettings;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * haoUseDyeOnShulkerBox(染色那半):手持**染料**右键**潜影盒** → 染成该染料的颜色,
 * **盒内物品与自定义名称原样保留**,并消耗 1 个染料。
 * <p>
 * <b>为什么是 {@code @Mixin(Item.class)} 而不是 {@code DyeItem}:</b>
 * 1.21.2 起告示牌染色被重构成 {@code SignChangingItem} 接口,{@code DyeItem} 因此**不再覆写**
 * {@code useOnBlock}(它只声明了 {@code getColor()})。{@code @Inject} 只能注入目标类**自身声明**
 * 的方法,Mixin 不会去父类找 —— 写 {@code @Mixin(DyeItem.class)} 会在运行时抛
 * {@code InvalidInjectionException: could not find any targets matching 'useOnBlock'}。
 * 而 {@code Item.useOnBlock} 是所有物品的默认实现,染料走的就是它,所以注入到 {@code Item}
 * 再用 {@code instanceof DyeItem} 过滤。HEAD 处不匹配就立刻 return,不影响其它物品。
 */
@Mixin(Item.class)
public abstract class DyeItem_useDyeOnShulkerBoxMixin {
	@Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
	private void hao$dyeShulkerBox(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (!UseDyeOnShulkerBoxSettings.isEnabled()) {
			return;
		}
		if (!((Object) this instanceof DyeItem dyeItem)) {
			return; // 只有染料才接管,其它物品交给原版
		}
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof ShulkerBoxBlock shulkerBox)) {
			return;
		}
		DyeColor target = dyeItem.getColor();
		if (shulkerBox.getColor() == target) {
			return; // 已经是这个颜色,交还原版
		}
		if (world instanceof ServerWorld serverWorld) {
			if (!ShulkerBoxContentHelper.recolor(serverWorld, pos, state, ShulkerBoxBlock.get(target))) {
				return;
			}
			context.getStack().decrement(1);
		}
		cir.setReturnValue(ActionResult.SUCCESS);
	}
}

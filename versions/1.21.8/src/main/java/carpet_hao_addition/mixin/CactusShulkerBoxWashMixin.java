package carpet_hao_addition.mixin;

import carpet_hao_addition.ShulkerBoxContentHelper;
import carpet_hao_addition.UseDyeOnShulkerBoxSettings;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * useDyeOnShulkerBox(洗色那半):**潜行**手持**仙人掌**右键**有颜色的潜影盒** → 洗回无色潜影盒,
 * 盒内物品与自定义名称原样保留,且**不消耗仙人掌**。
 * <p>
 * <b>为什么注入 {@code BlockItem} 而不是 {@code Item}:</b>仙人掌是方块物品({@code BlockItem}),
 * 而 {@code BlockItem} **自己声明并覆写**了 {@code useOnBlock}(放置方块的逻辑),覆写方法不会走到
 * 父类实现 —— 注入到父类 {@code Item} 对仙人掌无效。{@code @Inject} 只能注入目标类**自身声明**的
 * 方法,Mixin 不会替你去父类找。
 * <p>
 * <b>为什么必须潜行:</b>仙人掌右键方块是"放置"操作,不潜行时交给原版,避免误拦正常放置。
 */
@Mixin(BlockItem.class)
public abstract class CactusShulkerBoxWashMixin {
	@Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
	private void hao$washShulkerBox(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (!UseDyeOnShulkerBoxSettings.isEnabled()) {
			return;
		}
		ItemStack stack = context.getStack();
		if (!stack.isOf(Items.CACTUS)) {
			return; // 只有仙人掌才接管,其它方块物品交给原版
		}
		PlayerEntity player = context.getPlayer();
		if (player == null || !player.isSneaking()) {
			return;
		}
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof ShulkerBoxBlock shulkerBox) || shulkerBox.getColor() == null) {
			return; // 只洗有颜色的
		}
		if (world instanceof ServerWorld serverWorld) {
			if (!ShulkerBoxContentHelper.recolor(serverWorld, pos, state, Blocks.SHULKER_BOX)) {
				return;
			}
		}
		cir.setReturnValue(ActionResult.SUCCESS);
	}
}

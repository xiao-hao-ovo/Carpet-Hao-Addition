package carpet_hao_addition.mixin;

import carpet_hao_addition.ShulkerBoxContentHelper;
import carpet_hao_addition.UseDyeOnShulkerBoxSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * useDyeOnShulkerBox(洗色那半):**潜行**手持**仙人掌**右键**有颜色的潜影盒** → 洗回无色潜影盒,
 * 盒内物品与自定义名称原样保留,且**不消耗仙人掌**。
 * <p>
 * <b>为什么注入 {@code BlockItem} 而不是 {@code Item}:</b>仙人掌是方块物品({@code BlockItem}),
 * 而 {@code BlockItem} **自己声明并覆写**了 {@code useOn}(放置方块的逻辑),覆写方法不会走到
 * 父类实现 —— 注入到父类 {@code Item} 对仙人掌无效。{@code @Inject} 只能注入目标类**自身声明**
 * 的方法,Mixin 不会替你去父类找。
 * <p>
 * <b>为什么必须潜行:</b>仙人掌右键方块是"放置"操作,不潜行时交给原版,避免误拦正常放置。
 */
@Mixin(BlockItem.class)
public abstract class CactusShulkerBoxWashMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void hao$washShulkerBox(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!UseDyeOnShulkerBoxSettings.isEnabled()) {
			return;
		}
		ItemStack stack = context.getItemInHand();
		if (stack.getItem() != Items.CACTUS) {
			return; // 只有仙人掌才接管,其它方块物品交给原版
		}
		Player player = context.getPlayer();
		if (player == null || !player.isShiftKeyDown()) {
			return;
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ShulkerBoxBlock shulkerBox) || shulkerBox.getColor() == null) {
			return; // 只洗有颜色的
		}
		if (level instanceof ServerLevel serverLevel) {
			if (!ShulkerBoxContentHelper.recolor(serverLevel, pos, state, Blocks.SHULKER_BOX)) {
				return;
			}
		}
		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}

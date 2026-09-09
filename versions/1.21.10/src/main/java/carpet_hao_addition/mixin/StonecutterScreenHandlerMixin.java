package carpet_hao_addition.mixin;

import carpet_hao_addition.TerracottaUncolorSettings;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.StonecutterScreenHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * terracottaUncolor 规则的产出门控。
 * <p>
 * 16 条"染色陶瓦→陶瓦"的切石配方以数据包形式常驻,界面始终可见;
 * 规则关闭时,在切石机中放入染色陶瓦后点选还原应<b>无产出、不消耗</b>,
 * 因此在 StonecutterScreenHandler.onButtonClick 入口拦截。规则开启则放行原逻辑正常产出。
 */
@Mixin(StonecutterScreenHandler.class)
public abstract class StonecutterScreenHandlerMixin {
	@Unique
	private static final Set<Item> DYED_TERRACOTTA = Set.of(
			Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA,
			Items.LIGHT_BLUE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA,
			Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA,
			Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
			Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA,
			Items.BLACK_TERRACOTTA);

	@Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
	private void hao$gateTerracottaUncolor(net.minecraft.entity.player.PlayerEntity player, int id,
			CallbackInfoReturnable<Boolean> cir) {
		if (TerracottaUncolorSettings.isEnabled()) {
			return;
		}

		StonecutterScreenHandler handler = (StonecutterScreenHandler) (Object) this;
		ItemStack input = handler.input.getStack(0);
		if (DYED_TERRACOTTA.contains(input.getItem())) {
			// 规则关闭:染色陶瓦的还原点击不产出、不消耗。
			cir.setReturnValue(false);
		}
	}
}

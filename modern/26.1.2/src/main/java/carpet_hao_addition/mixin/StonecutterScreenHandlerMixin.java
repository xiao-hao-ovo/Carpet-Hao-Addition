package carpet_hao_addition.mixin;

import carpet_hao_addition.TerracottaUncolorSettings;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.StonecutterMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import net.minecraft.world.level.block.Blocks;

/**
 * terracottaUncolor 规则的产出门控。
 * <p>
 * 16 条"染色陶瓦→陶瓦"的切石配方以数据包形式常驻,界面始终可见;
 * 规则关闭时,在切石机中放入染色陶瓦后点选还原应<b>无产出、不消耗</b>,
 * 因此在 StonecutterMenu.onButtonClick 入口拦截。规则开启则放行原逻辑正常产出。
 */
@Mixin(StonecutterMenu.class)
public abstract class StonecutterScreenHandlerMixin {
	@Unique
	private static final Set<Item> UNCOLOR_INPUTS = Set.of(
			Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA,
			Items.LIGHT_BLUE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA,
			Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA,
			Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
			Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA,
			Items.BLACK_TERRACOTTA,
			Items.WHITE_GLAZED_TERRACOTTA, Items.ORANGE_GLAZED_TERRACOTTA, Items.MAGENTA_GLAZED_TERRACOTTA,
			Items.LIGHT_BLUE_GLAZED_TERRACOTTA, Items.YELLOW_GLAZED_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA,
			Items.PINK_GLAZED_TERRACOTTA, Items.GRAY_GLAZED_TERRACOTTA, Items.LIGHT_GRAY_GLAZED_TERRACOTTA,
			Items.CYAN_GLAZED_TERRACOTTA, Items.PURPLE_GLAZED_TERRACOTTA, Items.BLUE_GLAZED_TERRACOTTA,
			Items.BROWN_GLAZED_TERRACOTTA, Items.GREEN_GLAZED_TERRACOTTA, Items.RED_GLAZED_TERRACOTTA,
			Items.BLACK_GLAZED_TERRACOTTA);

	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void hao$gateTerracottaUncolor(net.minecraft.world.entity.player.Player player, int id,
			CallbackInfoReturnable<Boolean> cir) {
		if (TerracottaUncolorSettings.isEnabled()) {
			return;
		}

		StonecutterMenu handler = (StonecutterMenu) (Object) this;
		ItemStack input = handler.container.getItem(0);
		if (UNCOLOR_INPUTS.contains(input.getItem())) {
			// 规则关闭:还原点击不产出、不消耗(配方数据包已停用,此为兜底)。
			cir.setReturnValue(false);
		}
	}
}

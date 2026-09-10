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
			Items.DYED_TERRACOTTA.white(), Items.DYED_TERRACOTTA.orange(), Items.DYED_TERRACOTTA.magenta(),
			Items.DYED_TERRACOTTA.lightBlue(), Items.DYED_TERRACOTTA.yellow(), Items.DYED_TERRACOTTA.lime(),
			Items.DYED_TERRACOTTA.pink(), Items.DYED_TERRACOTTA.gray(), Items.DYED_TERRACOTTA.lightGray(),
			Items.DYED_TERRACOTTA.cyan(), Items.DYED_TERRACOTTA.purple(), Items.DYED_TERRACOTTA.blue(),
			Items.DYED_TERRACOTTA.brown(), Items.DYED_TERRACOTTA.green(), Items.DYED_TERRACOTTA.red(),
			Items.DYED_TERRACOTTA.black(),
			Items.GLAZED_TERRACOTTA.white(), Items.GLAZED_TERRACOTTA.orange(), Items.GLAZED_TERRACOTTA.magenta(),
			Items.GLAZED_TERRACOTTA.lightBlue(), Items.GLAZED_TERRACOTTA.yellow(), Items.GLAZED_TERRACOTTA.lime(),
			Items.GLAZED_TERRACOTTA.pink(), Items.GLAZED_TERRACOTTA.gray(), Items.GLAZED_TERRACOTTA.lightGray(),
			Items.GLAZED_TERRACOTTA.cyan(), Items.GLAZED_TERRACOTTA.purple(), Items.GLAZED_TERRACOTTA.blue(),
			Items.GLAZED_TERRACOTTA.brown(), Items.GLAZED_TERRACOTTA.green(), Items.GLAZED_TERRACOTTA.red(),
			Items.GLAZED_TERRACOTTA.black());

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

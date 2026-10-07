package carpet_hao_addition.mixin;

import carpet_hao_addition.WackoBeaconsSettings;

import net.minecraft.client.gui.widget.ClickableWidget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 信标效果按钮(客户端)—— 移植自 getcmdrolled/wacko-beacons。
 * <p>
 * 1.21.x 里按层数禁用按钮的地方是 {@code BeaconScreen$BaseButtonWidget.setDisabled(boolean)}
 * (它同时写 {@code disabled} 与父类的 {@code active})。这里在 HEAD 处直接吞掉这次调用并把
 * 按钮重新启用,效果按钮就不再按层数禁用 —— 于是 9 块钻石也能选 Strength(原版要 50 块)。
 * <p>
 * 26.x 的对应位置是 {@code BeaconScreen$BeaconPowerButton.updateStatus(int)}
 * (26.x 把这一族内部类重构成了 {@code BeaconPowerButton}),逻辑等价,只是类/方法名不同。
 * <p>
 * 这里用 {@code @Shadow} 写回 {@code disabled}(目标类自己声明的字段 ✓),
 * 再用转型访问父类 {@code ClickableWidget} 的 {@code active} —— Mixin 的 {@code @Shadow}
 * 只认目标类自己声明的成员,父类字段靠转型最稳。目标类是包内可见的内部类,不能直接转型,
 * 所以用 {@code (ClickableWidget) (Object) this}。
 */
@Mixin(targets = "net.minecraft.client.gui.screen.ingame.BeaconScreen$BaseButtonWidget")
public abstract class WackoBeacons_BeaconButtonWidgetMixin {
	@Shadow
	private boolean disabled;

	@Inject(method = "setDisabled", at = @At("HEAD"), cancellable = true)
	private void hao$forceEnabled(boolean value, CallbackInfo callbackInfo) {
		if (!WackoBeaconsSettings.isEnabled()) {
			return;
		}
		disabled = false;
		((ClickableWidget) (Object) this).active = true;
		callbackInfo.cancel();
	}
}

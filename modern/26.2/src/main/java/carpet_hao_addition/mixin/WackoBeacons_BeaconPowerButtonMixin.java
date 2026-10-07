package carpet_hao_addition.mixin;

import carpet_hao_addition.WackoBeaconsSettings;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 信标效果按钮(客户端)—— 移植自 getcmdrolled/wacko-beacons。
 * <p>
 * {@code BeaconPowerButton.updateStatus(level)} 是原版按信标层数给按钮设 {@code active} 的地方。
 * 在它尾部把按钮重新启用,效果按钮就不再按层数禁用 —— 于是 9 块钻石也能选 Strength(原版要 50 块)。
 * <p>
 * 这里直接 {@code ((AbstractWidget) (Object) this).active = true} 而不写 {@code @Shadow}:
 * {@code active} 是父类 {@code AbstractWidget} 的字段,而 Mixin 的 {@code @Shadow} 只认**目标类自己声明**
 * 的成员 —— 目标类是包内可见的内部类,也没法用 extends 绕过去,直接转型访问反而最稳。
 */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.BeaconScreen$BeaconPowerButton")
public abstract class WackoBeacons_BeaconPowerButtonMixin {
	@Inject(method = "updateStatus", at = @At("TAIL"))
	private void hao$forceActive(int level, CallbackInfo callbackInfo) {
		if (WackoBeaconsSettings.isEnabled()) {
			((AbstractWidget) (Object) this).active = true;
		}
	}
}

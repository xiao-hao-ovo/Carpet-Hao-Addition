package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 金胡萝卜堆肥规则。
 * <p>
 * 开启后,手持金胡萝卜对堆肥桶右键可 100% 堆肥(消耗与效果和普通可堆肥物品一致);
 * 关闭时金胡萝卜对堆肥桶无效果(与正常行为一致)。
 * 默认关闭。规则注册在 Carpet 默认管理器(/carpet),分类 "Hao"。
 * <p>
 * 判断规则是否开启请用 {@link #isEnabled()}:读取 Carpet 管理器的权威值,
 * @Rule 静态字段不保证被 carpet 回写。
 */
public class GoldenCarrotCompostSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "goldenCarrotCompost";

	@Rule(categories = {"Hao"})
	public static boolean goldenCarrotCompost = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

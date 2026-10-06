package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 切石机还原染色陶瓦规则。
 * <p>
 * 16 条"染色陶瓦 → 未染色陶瓦"的 stonecutting 配方以数据包形式常驻
 * (data/carpet-hao-addition/recipe),因此切石机界面始终能看到还原选项;
 * 本规则控制<b>产出</b>:开启后可选还原并正常产出;关闭时点选还原无产出、不消耗。
 * 默认关闭。规则注册在 Carpet 默认管理器(/carpet),分类 "Hao"。
 */
public class TerracottaUncolorSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "terracottaUncolor";

	@Rule(categories = {"Hao"})
	public static boolean terracottaUncolor = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

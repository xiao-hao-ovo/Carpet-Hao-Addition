package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 雪地方解石规则(仿 fabric-carpet 的 renewableDeepslate)。
 * <p>
 * 开启后,在降雪(雪地)生物群系中,岩浆与水的刷石机产物(石头/圆石)将变为方解石,
 * 获得可再生方解石来源;关闭时保持原版行为。默认关闭。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";
 * 判断是否开启请用 {@link #isEnabled()}(读取 carpet 管理器的权威值)。
 */
public class SnowyCalciteSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "snowyCalcite";

	@Rule(categories = {"Hao"})
	public static boolean snowyCalcite = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

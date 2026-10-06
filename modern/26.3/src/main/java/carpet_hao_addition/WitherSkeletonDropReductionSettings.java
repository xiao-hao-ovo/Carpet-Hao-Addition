package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 凋零骷髅掉落物自定义去除规则(移植自 Carpet-FGA-Addition 的
 * zombifiedPiglinDropReduction 思路,针对凋零骷髅)。
 * <p>
 * 选项(默认 false):
 * - false      保持原版掉落;
 * - bone       去除骨头;
 * - coal       去除煤炭;
 * - skull      去除凋零骷髅头颅;
 * - sword      去除死亡时掉落的手持石剑装备;
 * - all        去除以上全部。
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao"。
 */
public class WitherSkeletonDropReductionSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "witherSkeletonDropReduction";

	@Rule(categories = {"Hao"}, options = {"false", "bone", "coal", "skull", "sword", "all"})
	public static String witherSkeletonDropReduction = "false";

	/** 权威读取:carpet 默认管理器中该规则当前值(缺失/异常按 false)。 */
	public static String value() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		if (rule != null && rule.value() instanceof String s) {
			return s;
		}
		return "false";
	}

	/** 指定选项是否处于“去除”状态(bone/coal/skull/sword 各自,all 全去除)。 */
	public static boolean remove(String option) {
		String v = value();
		return v.equals(option) || v.equals("all");
	}
}

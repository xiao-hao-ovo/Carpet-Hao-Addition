package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 「切石机切铜」规则:让切石机也能切出**铜门 / 铜活板门 / 铜灯**。
 * <p>
 * 原版切石机已经支持"铜块 → 切制铜块 / 凿制铜块 / 铜格栅 / 台阶 / 楼梯"
 * (8 个变体共 64 条配方),但这三样只能用工作台合成。本规则开启后补充
 * 24 条 stonecutting 配方(8 变体 × 3 制品),沿用原版"1 铜块 → 4 个产物"的惯例。
 * <p>
 * 实现方式与 {@link TerracottaUncolorSettings 陶瓦还原}一致:配方以数据包形式在服务器启动时
 * 写入 world/datapacks/carpet-hao-addition_copper_stonecutting/,规则开关即时启停。
 * 默认关闭。规则注册在 Carpet 默认管理器(/carpet),分类 "Hao"。
 */
public class CopperStonecuttingSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "copperStonecuttingRecipes";

	@Rule(categories = {"Hao"})
	public static boolean copperStonecuttingRecipes = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 「潜影盒染色」规则(移植自 plusls-carpet-addition 的 {@code useDyeOnShulkerBox}):
 * <ul>
 *   <li>手持**染料**右键**潜影盒** → 把盒子染成该染料的颜色;</li>
 *   <li>手持**水瓶**右键**有颜色的潜影盒** → 洗回无色潜影盒(水瓶变回玻璃瓶);</li>
 *   <li>两种操作都**完整保留盒内物品与自定义名称**;</li>
 *   <li>对着无色潜影盒用染料、或用非水瓶的药水,一律交还原版行为。</li>
 * </ul>
 * 与手动"打掉再放回来"相比,这样染色不会丢东西,也不会破坏里面的潜影盒堆叠结构。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";默认关闭。
 */
public class UseDyeOnShulkerBoxSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "useDyeOnShulkerBox";

	@Rule(categories = {"Hao"})
	public static boolean useDyeOnShulkerBox = false;

	/** 权威读取:是否启用(缺失/异常按 false)。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean value && value;
	}
}

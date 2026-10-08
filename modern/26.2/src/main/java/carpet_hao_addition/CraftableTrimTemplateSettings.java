package carpet_hao_addition;

import carpet.api.settings.Rule;

/**
 * 可合成纹饰模板(haoCraftableTrimTemplates)规则设置。
 * <p>
 * 开启后,把 19 种纹饰/升级模板(18 种首饰纹饰 + 下界合金升级模板)的**可合成**配方
 * 部署到当前世界的数据包。配方沿用原版"复制配方"的形状,只是把必须的**本体模板**那格留空:
 * <pre>
 *   # #       # = 钻石 ×7
 *   #C#       C = 该模板的获取途径材料(见 TrimTemplateRecipeDeployHook)
 *   ###       → 产出 1 个模板
 * </pre>
 * 材料与原版复制配方完全一致(沉船→圆石、沙漠神殿→砂岩、古迹废墟→陶瓦……),
 * 所以不会凭空造出新价位。默认关闭。
 */
public class CraftableTrimTemplateSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoCraftableTrimTemplates";

	/** 是否启用可合成纹饰模板配方。 */
	@Rule(categories = {"Hao"})
	public static boolean haoCraftableTrimTemplates = false;

	/** 规则是否开启。 */
	public static boolean isEnabled() {
		return haoCraftableTrimTemplates;
	}
}

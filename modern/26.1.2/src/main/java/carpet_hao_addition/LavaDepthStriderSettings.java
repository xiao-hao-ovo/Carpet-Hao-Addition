package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 岩浆中的深海探索者规则。
 * <p>
 * 原版中深海探索者(Depth Strider)只在水中生效:{@code LivingEntity.travel} 读取实体的
 * {@code water_movement_efficiency} 属性,并据此调整水中移动的水平阻尼与加速度。
 * 开启本规则后,玩家在岩浆中移动时同样套用「水中 + 该属性」的那套公式,即岩浆不再减速,
 * 手感与穿着深海探索者靴子在水中一致(附魔等级越高越快)。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";默认关闭。
 */
public class LavaDepthStriderSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "lavaDepthStrider";

	@Rule(categories = {"Hao"})
	public static boolean lavaDepthStrider = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true(缺失/异常按 false)。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean value && value;
	}
}

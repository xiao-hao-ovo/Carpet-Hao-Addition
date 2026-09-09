package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 基岩可挖掘规则(移植自 Carpet CuO Addition 的 bedrockCanBeMined)。
 * <p>
 * 开启后,基岩(bedrock)可被挖掘:挖掘时硬度等同黑曜石,
 * 挖碎后掉落基岩物品;关闭时恢复原版(基岩不可破坏)。默认关闭。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";
 * 判断是否开启请用 {@link #isEnabled()}(读取 carpet 管理器的权威值)。
 */
public class BedrockCanBeMinedSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "bedrockCanBeMined";

	@Rule(categories = {"Hao"})
	public static boolean bedrockCanBeMined = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 掉落物直入背包规则(仿 Carpet-DDS-Addition 的 directBlockDrops 并扩展出实体版本)。
 * <p>
 * 两条规则各自独立、默认关闭:
 * <ul>
 *   <li>{@code directBlockDrops} — 玩家破坏方块产生的掉落物(含连锁破坏、失去支撑后产生的同步掉落,
 *       以及被破坏容器内释放的物品)优先直接送入该玩家背包;</li>
 *   <li>{@code directEntityDrops} — 玩家击杀实体产生的掉落物(生物与盔甲架走死亡掉落,矿车、船、
 *       画、物品展示框等非生物实体走各自的破坏掉落)同样优先直接送入该玩家背包。</li>
 * </ul>
 * 两条规则共用同一套「掉落归属」上下文;若背包无法装下,剩余部分仍按原版机制掉落在世界中。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";
 * 判断是否开启请用 {@link #blockDropsEnabled()} / {@link #entityDropsEnabled()}(读取 carpet 管理器的权威值)。
 */
public class DirectDropsSettings {
	/** 方块掉落直入背包的规则名(注册在 carpet 默认管理器)。 */
	public static final String BLOCK_RULE_NAME = "directBlockDrops";
	/** 实体掉落直入背包的规则名(注册在 carpet 默认管理器)。 */
	public static final String ENTITY_RULE_NAME = "directEntityDrops";

	@Rule(categories = {"Hao"})
	public static boolean directBlockDrops = false;

	@Rule(categories = {"Hao"})
	public static boolean directEntityDrops = false;

	/** 方块掉落直入背包是否开启。 */
	public static boolean blockDropsEnabled() {
		return enabled(BLOCK_RULE_NAME);
	}

	/** 实体掉落直入背包是否开启。 */
	public static boolean entityDropsEnabled() {
		return enabled(ENTITY_RULE_NAME);
	}

	/** 任一规则开启即为 true;此时才需要维护掉落归属上下文。 */
	public static boolean anyEnabled() {
		return blockDropsEnabled() || entityDropsEnabled();
	}

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	private static boolean enabled(String ruleName) {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(ruleName);
		return rule != null && rule.value() instanceof Boolean value && value;
	}
}

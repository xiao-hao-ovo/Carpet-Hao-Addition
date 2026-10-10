package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 「轻松放置实体」规则:用 Litematica 的「轻松放置」照着投影施工时,把投影里的**实体**
 * (矿车、漏斗矿车、盔甲架、船、物品展示框等)也一并放出来 —— 这些实体没有"可放置的方块形式",
 * 原版与 Litematica 的轻松放置都不会放下它们。
 * <p>
 * 这条规则**只是总开关**({@code /carpet haoProjectionEntity true|false});
 * **一次放几个由每个玩家自己设置**,用命令:
 * <ul>
 *   <li>{@code /easyPlaceEntityCount &lt;1-64&gt;} —— 设置自己的放置数量;</li>
 *   <li>{@code /easyPlaceEntityCount} —— 查看自己当前的数量。</li>
 * </ul>
 * 数量只保存在内存里(服务器重启后回到默认 1),重跑一次命令即可。
 * <p>
 * 只对**有对应物品**的实体生效(矿车类、船类、盔甲架、物品展示框、画、末影水晶等);
 * 没有对应物品的实体(如凋灵、村民)不会被放置,也不会消耗任何东西。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";默认关闭。
 */
public class EasyPlaceEntitySettings {
	/** 规则名(是否放实体)。 */
	public static final String RULE_NAME = "haoProjectionEntity";
	/** 一次最多放几个。 */
	public static final int MAX_COUNT = 64;
	/** 默认数量。 */
	public static final int DEFAULT_COUNT = 1;

	@Rule(categories = {"Hao"})
	public static boolean haoProjectionEntity = false;

	/** 各玩家自己的"一次放几个"(内存;没设置过的玩家按 {@link #DEFAULT_COUNT})。 */
	private static final Map<UUID, Integer> COUNTS = new HashMap<>();

	/** 默认总是弹选择器界面(即使只命中一个实体)。 */
	public static final boolean DEFAULT_UI = true;

	/** 各玩家自己的"是否总是弹选择器界面"(内存;没设置过的玩家按 {@link #DEFAULT_UI})。 */
	private static final Map<UUID, Boolean> UI_ENABLED = new HashMap<>();

	/** 权威读取:是否启用(缺失/异常按 false)。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean value && value;
	}

	/** 该玩家一次放几个(1-64;没设置过按默认 1)。 */
	public static int count(UUID playerId) {
		Integer value = COUNTS.get(playerId);
		return value == null ? DEFAULT_COUNT : value;
	}

	/** 设置该玩家一次放几个(自动钳制到 1-64;不影响他人)。 */
	public static void setCount(UUID playerId, int value) {
		if (playerId != null) {
			COUNTS.put(playerId, Math.max(1, Math.min(MAX_COUNT, value)));
		}
	}

	/**
	 * 该玩家是否**总是弹选择器界面**。
	 * <p>
	 * 关掉后:只命中一个投影实体时直接放置(快),命中多个时仍然会弹界面(否则没法选要放哪个)。
	 */
	public static boolean uiEnabled(UUID playerId) {
		Boolean value = UI_ENABLED.get(playerId);
		return value == null ? DEFAULT_UI : value;
	}

	/** 设置该玩家的选择器界面开关(不影响他人)。 */
	public static void setUiEnabled(UUID playerId, boolean value) {
		if (playerId != null) {
			UI_ENABLED.put(playerId, value);
		}
	}
}

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
 * 取值三态:
 * <ul>
 *   <li>{@code false} —— 关闭:客户端不触发,服务端也不接受放置请求;</li>
 *   <li>{@code true} —— 开启:客户端在右键轻松放置的那一刻触发;</li>
 *   <li>{@code not_trigger} —— 不触发:客户端的右键**不会**放出实体,但服务端仍然接受请求,
 *       留给别的客户端来触发(例如 HaoMaSa 的投影实体放置,用它的按键放)。</li>
 * </ul>
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

	/** 规则取值。 */
	public enum Trigger {
		/** 关闭。 */
		FALSE,
		/** 开启:由本模组的客户端在右键轻松放置时触发。 */
		TRUE,
		/** 不触发:客户端不主动放,只保留服务端,供别的客户端触发。 */
		NOT_TRIGGER
	}

	@Rule(categories = {"Hao"})
	public static Trigger haoProjectionEntity = Trigger.FALSE;

	/** 各玩家自己的"一次放几个"(内存;没设置过的玩家按 {@link #DEFAULT_COUNT})。 */
	private static final Map<UUID, Integer> COUNTS = new HashMap<>();

	/** 默认总是弹选择器界面(即使只命中一个实体)。 */
	public static final boolean DEFAULT_UI = true;

	/** 各玩家自己的"是否总是弹选择器界面"(内存;没设置过的玩家按 {@link #DEFAULT_UI})。 */
	private static final Map<UUID, Boolean> UI_ENABLED = new HashMap<>();

	/** 权威读取:当前取值(缺失/异常按 {@link Trigger#FALSE})。 */
	public static Trigger value() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Trigger trigger ? trigger : Trigger.FALSE;
	}

	/**
	 * 客户端是否处理**这一次**触发。
	 * <p>
	 * 触发来源分两种,本模组自己分辨不了 —— 由配套客户端通过
	 * {@link EntityPlacementTrigger} 打标记告诉本模组:
	 * <ul>
	 *   <li>{@code true} —— 右键与快捷键都处理;</li>
	 *   <li>{@code not_trigger} —— <b>只有快捷键</b>处理(右键不放实体、也不弹选择器界面);</li>
	 *   <li>{@code false} —— 都不处理。</li>
	 * </ul>
	 *
	 * @param fromHotkey 本次调用是否由配套客户端的快捷键触发
	 */
	public static boolean isTriggerEnabled(boolean fromHotkey) {
		Trigger trigger = value();
		if (trigger == Trigger.TRUE) {
			return true;
		}
		return trigger == Trigger.NOT_TRIGGER && fromHotkey;
	}

	/** 服务端是否接受放置请求:{@code true} 与 {@code not_trigger} 都接受。 */
	public static boolean isServerEnabled() {
		return value() != Trigger.FALSE;
	}

	/**
	 * 该玩家一次放几个(1-64;没设置过按默认 1)。
	 * <p>
	 * 若本次轻松放置是配套客户端的**快捷键**触发的,且它带来了自己的「一次放置数量」,
	 * 则以那个为准 —— 配套客户端的配置界面里也有同一项,玩家在那边改就该在那边生效。
	 * 右键路径会在入口处清掉这个覆盖值(见 {@code EntityPlacementTrigger#clearHotkeyCount})。
	 */
	public static int count(UUID playerId) {
		int override = EntityPlacementTrigger.hotkeyCount();
		if (override >= 1) {
			return Math.max(1, Math.min(MAX_COUNT, override));
		}
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

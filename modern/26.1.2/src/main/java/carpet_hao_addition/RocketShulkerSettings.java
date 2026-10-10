package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 「火箭潜影盒」规则:背包里名称正好是 {@code rocket} 的潜影盒,若里面装着烟花火箭,
 * 就保证玩家指定的那一格一直有烟花可用 —— 那一格空了自动补上一组,用完继续补,
 * 直到盒内烟花耗尽。
 * <p>
 * 这条规则**只是总开关**({@code /carpet haoRocketShulker true|false});
 * 补给位置**按玩家各自保存**,由玩家自己用命令设置:
 * <ul>
 *   <li>{@code /haoRocketShulker offhand} —— 补到副手(默认);</li>
 *   <li>{@code /haoRocketShulker mainhand [1-9]} —— 补到主手快捷栏第 1-9 格。</li>
 * </ul>
 * 设置只保存在内存里(服务器重启后各玩家回到默认:副手、第 1 格),重跑一次命令即可。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";默认关闭。
 */
public class RocketShulkerSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoRocketShulker";

	/** 视作"火箭盒"的潜影盒自定义名称(需完全一致)。 */
	public static final String SHULKER_NAME = "rocket";

	/** 每次补进目标格的烟花数量(一组)。 */
	public static final int REFILL_AMOUNT = 64;

	/** 补给位置。 */
	public enum Target {
		/** 副手(默认)。 */
		OFFHAND,
		/** 主手快捷栏,具体槽位见 {@link Prefs#slot()}。 */
		MAINHAND
	}

	/** 某个玩家的补给偏好。 */
	public record Prefs(Target target, int slot) {
		/** 未设置过的玩家使用的默认偏好:副手、第 1 格。 */
		public static final Prefs DEFAULT = new Prefs(Target.OFFHAND, 1);
	}

	@Rule(categories = {"Hao"})
	public static boolean haoRocketShulker = false;

	/** 各玩家自己的偏好(内存;没设置过的玩家按 {@link Prefs#DEFAULT})。 */
	private static final Map<UUID, Prefs> PREFERENCES = new HashMap<>();

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true(缺失/异常按 false)。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean value && value;
	}

	/** 该玩家的补给位置(没设置过按副手)。 */
	public static Target targetRef(UUID playerId) {
		Prefs prefs = PREFERENCES.get(playerId);
		return prefs == null ? Prefs.DEFAULT.target() : prefs.target();
	}

	/** 该玩家的快捷栏槽位(1-9;没设置过按 1)。 */
	public static int hotbarSlot(UUID playerId) {
		Prefs prefs = PREFERENCES.get(playerId);
		return prefs == null ? Prefs.DEFAULT.slot() : prefs.slot();
	}

	/** 设置该玩家的补给位置(不影响其他人;槽位保持不变)。 */
	public static void setTarget(UUID playerId, Target value) {
		if (playerId == null || value == null) {
			return;
		}
		Prefs old = PREFERENCES.getOrDefault(playerId, Prefs.DEFAULT);
		PREFERENCES.put(playerId, new Prefs(value, old.slot()));
	}

	/** 设置该玩家的快捷栏槽位(自动钳到 1-9;不影响其他人)。 */
	public static void setHotbarSlot(UUID playerId, int slot) {
		if (playerId == null) {
			return;
		}
		Prefs old = PREFERENCES.getOrDefault(playerId, Prefs.DEFAULT);
		PREFERENCES.put(playerId, new Prefs(old.target(), Math.max(1, Math.min(9, slot))));
	}
}

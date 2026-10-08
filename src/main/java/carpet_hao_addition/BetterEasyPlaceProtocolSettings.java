package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 增强轻松放置协议(haoBetterEasyPlaceProtocol)规则设置。
 * <p>
 * 堆肥桶层数就并在这条规则里(原来单独的 {@code haoEasyPlaceComposterLevel} 已合并进来),取值:
 * <ul>
 *   <li>{@code false} —— 关闭(默认);</li>
 *   <li>{@code true} —— 开启,但**不**还原堆肥桶层数;</li>
 *   <li>{@code with_composter_level} —— 开启,连堆肥桶层数也一起按投影还原。</li>
 * </ul>
 */
public class BetterEasyPlaceProtocolSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoBetterEasyPlaceProtocol";

	/** 规则取值。 */
	public enum Mode {
		/** 关闭(默认)。 */
		FALSE,
		/** 开启:不还原堆肥桶层数。 */
		TRUE,
		/** 开启:连堆肥桶层数也一起还原。 */
		WITH_COMPOSTER_LEVEL
	}

	@Rule(categories = {"Hao"})
	public static Mode haoBetterEasyPlaceProtocol = Mode.FALSE;

	/** 权威读取规则取值(缺失/异常按 {@link Mode#FALSE})。 */
	public static Mode mode() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		if (rule != null && rule.value() instanceof Mode value) {
			return value;
		}
		return Mode.FALSE;
	}

	/** 规则是否开启。 */
	public static boolean isEnabled() {
		return mode() != Mode.FALSE;
	}

	/** 轻松放置时是否要还原堆肥桶层数(只有选 {@link Mode#WITH_COMPOSTER_LEVEL} 时才是)。 */
	public static boolean composterLevelEnabled() {
		return mode() == Mode.WITH_COMPOSTER_LEVEL;
	}
}

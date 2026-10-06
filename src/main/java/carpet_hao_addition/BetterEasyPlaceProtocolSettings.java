package carpet_hao_addition;

import carpet.api.settings.Rule;

/** 增强轻松放置协议(haoBetterEasyPlaceProtocol)规则设置。 */
public class BetterEasyPlaceProtocolSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoBetterEasyPlaceProtocol";

	/** 是否启用增强轻松放置协议。 */
	@Rule(categories = {"Hao"})
	public static boolean haoBetterEasyPlaceProtocol = false;

	/** 规则是否开启。 */
	public static boolean isEnabled() {
		return haoBetterEasyPlaceProtocol;
	}
}

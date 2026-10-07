package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 信标效果解锁(客户端)—— 移植自 getcmdrolled/wacko-beacons。
 * <p>
 * 原 mod 是一个 proof of concept:信标的效果在**客户端**才被限制,服务端只校验
 * "效果是否有效"(以及 secondary 与 primary 相同时不给 II 级 + 层数要求),并不校验
 * "这个效果在你当前层数下能不能选"。于是纯客户端改两处就能做到:
 * <ol>
 *   <li><b>Regeneration II</b>:把 Regeneration 挪进第 2 档的效果表,客户端在 1 层信标就能把它
 *       选成主效果;再把 Regeneration 选成副效果(与主效果相同)即得 II 级。</li>
 *   <li><b>省钻石</b>:客户端的效果按钮不再按信标层数禁用,于是 9 块钻石也能选到 Strength I
 *       (原版要 50 块)。</li>
 * </ol>
 * 默认关闭。规则注册在 Carpet 默认管理器(/carpet),分类 "Hao"。
 * <p>
 * 判断规则是否开启请用 {@link #isEnabled()}:读取 Carpet 管理器的权威值,
 * @Rule 静态字段不保证被 carpet 回写。
 */
public class WackoBeaconsSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoWackoBeacons";

	@Rule(categories = {"Hao"})
	public static boolean haoWackoBeacons = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

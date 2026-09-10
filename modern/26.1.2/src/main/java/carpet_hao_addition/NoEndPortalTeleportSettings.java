package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 末地传送门不传送玩家规则(仿 Carpet-AMS-Addition 的
 * commandPlayerNoNetherPortalTeleport 思路,针对末地传送门)。
 * <p>
 * 开启后提供 {@code /playerNoEndPortalTeleport} 名单管理,但<b>本身不改变传送</b>:
 * 只有当 globalMode 开启(所有玩家)或该玩家被加入黑名单后,才不被末地传送门传送;
 * 关闭规则后,名单/全局模式判定不生效,全员恢复原版。默认关闭。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";
 * 判断是否开启请用 {@link #isEnabled()}(读取 carpet 管理器的权威值)。
 */
public class NoEndPortalTeleportSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "noEndPortalTeleport";

	@Rule(categories = {"Hao"})
	public static boolean noEndPortalTeleport = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 「自动经验修补」规则 —— **移植于 Carpet WuHu Addition**(上游规则名 {@code haoAutoMending})。
 * <p>
 * 每 20 tick(1 秒)把玩家**当前等级进度条里已积累的经验**自动拿去修补身上带「经验修补」附魔
 * 且已损坏的装备:能修多少耐久就修多少(受附魔"每点经验修多少耐久"的换算限制),
 * 并从玩家经验里**扣除等量的经验点** —— 不会凭空生耐久而破坏平衡。
 * <p>
 * 与手动补修的差别:原版要等经验被吸收时才会自动修补、且一次只修一点;
 * 这个规则按秒检查,把"卡在等级进度条里"的那部分经验也利用起来。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";默认关闭。
 * 规则名与上游保持一致,便于对照。
 */
public class AutoMendingSettings {
	/** Carpet 规则名(与上游 Carpet WuHu Addition 一致)。 */
	public static final String RULE_NAME = "haoAutoMending";

	@Rule(categories = {"Hao"})
	public static boolean haoAutoMending = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

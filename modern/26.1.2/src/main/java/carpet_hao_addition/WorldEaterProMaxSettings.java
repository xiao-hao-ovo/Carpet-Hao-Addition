package carpet_hao_addition;

import carpet.api.settings.Rule;

/**
 * 增强世界吞噬者 ProMax:让**含水方块**也能被爆炸炸掉。
 * <p>
 * 原版算抗性用 {@code max(方块抗性, 流体抗性)},水的流体抗性是 100 —— 所以含水方块
 * (台阶、珊瑚扇、栅栏……)的有效抗性等于 100,TNT 根本炸不动,这就是原版
 * "含水方块防爆"的原因。本规则只让"流体那一项不参与"。
 * <p>
 * 高抗性方块(黑曜石/铁块/远古残骸等)也能被炸是另一回事 —— 那部分请用
 * Carpet-AMS-Addition 的 {@code enhancedWorldEater},本项目不再重复实现。
 */
public class WorldEaterProMaxSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoEnhancedWorldEaterProMax";

	/** 是否让含水方块也能被炸(水的流体抗性按 0 算)。 */
	@Rule(categories = {"Hao"})
	public static boolean haoEnhancedWorldEaterProMax = false;

	/** 规则是否开启。 */
	public static boolean isEnabled() {
		return haoEnhancedWorldEaterProMax;
	}
}

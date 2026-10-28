package carpet_hao_addition;

import carpet.api.settings.Rule;

/**
 * 增强世界吞噬者 ProMax：让含水方块和黑曜石等高抗性方块也能被爆炸炸毁。
 * <p>
 * 参照 Carpet-AMS-Addition 的 enhancedWorldEater：规则值设为大于等于 0 时，爆炸抗性
 * 大于等于 17 的方块按该值参与爆炸；-1 关闭。基岩、铁砧、末地传送门相关方块仍受保护。
 */
public class WorldEaterProMaxSettings {
	/** Carpet 规则名（注册在 carpet 默认管理器）。 */
	public static final String RULE_NAME = "haoEnhancedWorldEaterProMax";

	/** 高抗性方块的爆炸抗性上限；-1 关闭。 */
	@Rule(options = {"-1", "0", "1", "2", "4", "8", "16"}, categories = {"Hao"}, strict = false)
	public static double haoEnhancedWorldEaterProMax = -1.0D;

	public static boolean isEnabled() {
		return haoEnhancedWorldEaterProMax >= 0.0D;
	}

	public static float resistanceCap() {
		return (float) Math.min(16.0D, Math.max(0.0D, haoEnhancedWorldEaterProMax));
	}
}

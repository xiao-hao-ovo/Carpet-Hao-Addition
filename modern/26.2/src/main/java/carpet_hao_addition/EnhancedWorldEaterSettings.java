package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.Validator;

import net.minecraft.commands.CommandSourceStack;

/**
 * 完全移植自 Carpet-AMS-Addition 的 {@code enhancedWorldEater} 规则。
 * <p>
 * AMS 自 v26.4 起删除了这条规则，这里按 AMS v26.3.1（最后一个还带该规则的 1.21.8 构建）逐行移植：
 * 规则值是爆炸抗性上限（{@code -1} 关闭），取值只允许 {@code -1} 或 {@code 0 ~ 16}；生效时把
 * <b>爆炸抗性不低于 17</b> 的方块按该值参与爆炸，基岩、三种铁砧、末地传送门框架、末地传送门、
 * 末地折跃门仍然保持原抗性。
 * <p>
 * 与 {@link WorldEaterProMaxSettings} 的关系：两条规则互不影响，都开启时本规则优先；ProMax 额外
 * 提供的「含水方块可炸」仍由它自己的 mixin 负责。
 * <p>
 * 只注册在 1.21.8 层（其他 1.21.x / 26.x 层没有这份实现），注册入口是
 * {@code CarpetHaoAdditionExtension#ensureEnhancedWorldEaterRule} 的反射调用。
 */
public class EnhancedWorldEaterSettings {
	/** Carpet 规则名（与 AMS 原规则同名，保证原来的命令/配置不用改）。 */
	public static final String RULE_NAME = "enhancedWorldEater";

	/** 与 AMS 的 {@code BlastResistanceValidator} 一致：只接受 -1 或 0 ~ 16。 */
	public static class BlastResistanceValidator extends Validator<Double> {
		@Override
		public Double validate(CommandSourceStack source, CarpetRule<Double> changingRule,
				Double newValue, String userInput) {
			if (newValue == null) {
				return null;
			}
			double value = newValue;
			if (value == -1.0D || (value >= 0.0D && value <= 16.0D)) {
				return newValue;
			}
			return null;
		}

		@Override
		public String description() {
			return "Value must be -1 or between 0 and 16 / 值必须为 -1 或 0~16";
		}
	}

	/** 高抗性方块的爆炸抗性上限；{@code -1} 关闭。 */
	@Rule(options = {"-1"}, categories = {"Hao"},
			validators = {BlastResistanceValidator.class}, strict = false)
	public static double enhancedWorldEater = -1.0D;

	/** 新开关的规则名。 */
	public static final String WATER_RULE_NAME = "haoEnhancedWorldEaterWater";

	/**
	 * 是否允许世界吞噬者炸掉水和水源（默认关闭）。
	 * <p>
	 * 单独开启即生效，不依赖 {@code enhancedWorldEater} / {@code haoEnhancedWorldEaterProMax}：
	 * 水源和流动水都是 {@code Blocks.WATER}，抗性 100；开启后水方块抗性按 0 算，再把水的流体抗性
	 * 归零（见 {@code FluidState_breakWaterloggedMixin}），爆炸就能把整格水移除。
	 * 与 ProMax 的「含水方块保留水」互不影响：ProMax 仍只处理含水方块，本开关只处理水格本身。
	 */
	@Rule(categories = {"Hao"})
	public static boolean haoEnhancedWorldEaterWater = false;

	/** 权威读取「炸水」开关（缺失/异常按关闭）。 */
	public static boolean isWaterDestroyEnabled() {
		// Carpet 尚未初始化时（例如 Blocks.<clinit> 阶段方块构造器调用 getExplosionResistance），
		// settingsManager 还是 null；此时规则按关闭处理，不能直接解引用。
		if (CarpetServer.settingsManager == null) {
			return false;
		}
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(WATER_RULE_NAME);
		return rule != null && Boolean.TRUE.equals(rule.value());
	}

	/** 权威读取规则取值（缺失/异常按 -1 关闭）。 */
	public static double value() {
		if (CarpetServer.settingsManager == null) {
			return -1.0D;
		}
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		if (rule != null && rule.value() instanceof Double value) {
			return value;
		}
		return -1.0D;
	}

	/** 规则是否开启。 */
	public static boolean isEnabled() {
		return value() != -1.0D;
	}

	/** 生效的爆炸抗性（限制在 0 ~ 16，与 AMS 的 validator 范围一致）。 */
	public static float blastResistance() {
		return (float) Math.min(16.0D, Math.max(0.0D, value()));
	}
}
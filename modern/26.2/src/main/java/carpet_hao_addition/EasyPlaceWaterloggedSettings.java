package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 投影轻松放置时补水的规则(Litematica 联动)。
 * <p>
 * 用 Litematica 的「轻松放置」照着投影施工时,如果投影里某个方块是含水
 * (waterlogged)方块,仅靠原版放置流程是放不出水的(客户端不会凭空造水),
 * 结果就是方块放下去了但水没进去。
 * <p>
 * 开启本规则后:轻松放置含水方块时,会直接构造出含水状态(冰不会被真的放出来),
 * 并从玩家背包(任意位置)消耗 1 个冰;若背包里没有冰,则只放置方块本身,
 * 水不补,其余行为完全不变。
 * <p>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao";默认关闭。
 * 具体实现依赖 Litematica,只在与 Litematica 同版本对应的少数版本层生效。
 */
public class EasyPlaceWaterloggedSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoProjectionWaterlogged";

	@Rule(categories = {"Hao"})
	public static boolean haoProjectionWaterlogged = false;

	/** 触发条件规则名:补料在玩家站立 / 蹲下时触发。 */
	public static final String TRIGGER_RULE_NAME = "haoProjectionWaterloggedTrigger";

	/** 触发条件。 */
	public enum Trigger {
		/** 站立触发:玩家未潜行时才补料。 */
		STANDING,
		/** 蹲下触发:玩家潜行(按住潜行键)时才补料。 */
		SNEAKING,
		/** 两种姿态都触发(默认):站着或蹲下都补料。 */
		ALWAYS
	}

	@Rule(categories = {"Hao"})
	public static Trigger haoProjectionWaterloggedTrigger = Trigger.ALWAYS;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true(缺失/异常按 false)。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean value && value;
	}

	/** 权威读取触发条件(缺失/异常按 {@link Trigger#ALWAYS})。 */
	public static Trigger trigger() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(TRIGGER_RULE_NAME);
		if (rule != null && rule.value() instanceof Trigger value) {
			return value;
		}
		return Trigger.ALWAYS;
	}

	/**
	 * 按玩家当前的潜行状态判断这次是否应该触发补料。
	 *
	 * @param sneaking 玩家此刻是否在潜行(蹲下)
	 */
	public static boolean triggerAllowed(boolean sneaking) {
		return switch (trigger()) {
			case STANDING -> !sneaking;
			case SNEAKING -> sneaking;
			case ALWAYS -> true;
		};
	}
}

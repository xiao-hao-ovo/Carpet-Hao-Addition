package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 投影轻松放置「补料」规则(Litematica 联动)。
 * <p>
 * 用 Litematica 的「轻松放置」照着投影施工时,投影里的水/岩浆/装岩浆的炼药锅/火等都没有对应的
 * 可放置物品,靠原版放不出来。开启本规则后由服务端按投影补齐,并从玩家那里消耗对应材料
 * (水→冰,岩浆→岩浆块,装岩浆的炼药锅→炼药锅+岩浆块,点火→火焰弹或打火石耐久)。
 * <p>
 * 原来的 {@code easyPlaceWaterlogged} 与 {@code easyPlaceWaterloggedShulker} 两条规则已合并为这一条:
 * 背包里没有材料时,是否从背包中的潜影盒取料、以及怎么取,由规则取值决定。
 * <p>
 * 取值:
 * <ul>
 *   <li>{@code false} —— 关闭(默认);</li>
 *   <li>{@code true} —— 开启,只用背包(含快捷栏/副手)里的材料;</li>
 *   <li>{@code shulker_direct} —— 开启,背包没有时直接在潜影盒里扣除(材料不拿到背包);</li>
 *   <li>{@code shulker_take} —— 开启,背包没有时把材料取到背包并留在背包。</li>
 * </ul>
 * 规则注册在 Carpet 默认管理器(/carpet),分类 "Hao"。
 */
public class EasyPlaceWaterloggedSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "easyPlaceWaterlogged";

	/** 规则取值。 */
	public enum Mode {
		/** 关闭(默认):不补料、不点火。 */
		FALSE,
		/** 开启:只用背包(含快捷栏/副手)里的材料。 */
		TRUE,
		/** 开启 + 潜影盒取料(方案一):直接在潜影盒里扣除,材料不拿到背包。 */
		SHULKER_DIRECT,
		/** 开启 + 潜影盒取料(方案二):把材料取到背包并留在背包。 */
		SHULKER_TAKE
	}

	/** 潜影盒取料方式(由规则取值推导)。 */
	public enum ShulkerMode {
		/** 不从潜影盒取。 */
		FALSE,
		/** 方案一:直接在潜影盒里扣除。 */
		DIRECT,
		/** 方案二:取到背包并留在背包。 */
		TAKE
	}

	/** 触发条件规则名:补料在玩家站立 / 蹲下时触发。 */
	public static final String TRIGGER_RULE_NAME = "easyPlaceWaterloggedTrigger";

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
	public static Trigger easyPlaceWaterloggedTrigger = Trigger.ALWAYS;

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

	@Rule(categories = {"Hao"})
	public static Mode easyPlaceWaterlogged = Mode.FALSE;

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

	/** 潜影盒取料方式(缺失/异常按 {@link ShulkerMode#FALSE})。 */
	public static ShulkerMode shulkerMode() {
		return switch (mode()) {
			case SHULKER_DIRECT -> ShulkerMode.DIRECT;
			case SHULKER_TAKE -> ShulkerMode.TAKE;
			default -> ShulkerMode.FALSE;
		};
	}
}

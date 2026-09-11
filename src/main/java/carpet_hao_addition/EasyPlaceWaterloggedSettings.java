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
	public static final String RULE_NAME = "easyPlaceWaterlogged";

	@Rule(categories = {"Hao"})
	public static boolean easyPlaceWaterlogged = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true(缺失/异常按 false)。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean value && value;
	}
}

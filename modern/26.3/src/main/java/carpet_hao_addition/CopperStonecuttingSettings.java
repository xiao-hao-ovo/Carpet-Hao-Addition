package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * 「切石机切铜」规则:让切石机在**任意铜形态之间等值互换**。
 * <p>
 * 原版切石机只支持"铜块 → 切制铜块 / 凿制铜块 / 铜格栅 / 台阶 / 楼梯"
 * 与"切制铜块 → 凿制 / 台阶 / 楼梯"(8 个变体共 64 条配方),其余形态只能用工作台合成。
 * 本规则开启后按原版配方的当量把方向补全:同一变体内任意形态互切、8 个变体之间 1:1 互换、
 * 以及任意变体的铜块一步切出任意其它变体的任意形态(合计约 2000 条 stonecutting 配方)。
 * <p>
 * 当量一律从原版配方反推(铜粒 / 铜锭 / 铜块 / 切石机 / 合成),产出数向下取整,绝不产生套利。
 * 26.x 新增的铜栏杆 / 铜链 / 铜箱子 / 铜灯笼 / 铜火把也在内(铜矛不是纯铜产物,不参与),
 * 它们没有原版切石机配方,按合成配方折算。
 * <p>
 * 实现方式与 {@link TerracottaUncolorSettings 陶瓦还原}一致:配方以数据包形式在服务器启动时
 * 写入 world/datapacks/hao-copper-cut/,规则开关即时启停。
 * 默认关闭。规则注册在 Carpet 默认管理器(/carpet),分类 "Hao"。
 */
public class CopperStonecuttingSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoCopperStonecutting";

	@Rule(categories = {"Hao"})
	public static boolean haoCopperStonecutting = false;

	/** 权威读取:carpet 默认管理器中该规则当前是否为 true。 */
	public static boolean isEnabled() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		return rule != null && rule.value() instanceof Boolean enabled && enabled;
	}
}

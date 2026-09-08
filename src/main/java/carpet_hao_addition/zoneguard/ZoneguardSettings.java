package carpet_hao_addition.zoneguard;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

/**
 * zoneguard 规则定义。
 * <p>
 * 该规则控制 /zoneguard 管理的立方区域内侦测器(观察者)是否被禁用。
 * 默认关闭,即不改变原版行为;开启后区域内侦测器将不再输出信号/更新邻居/发出脉冲。
 * <p>
 * 规则注册到 Carpet 的默认设置管理器(见 CarpetHaoAdditionExtension.onGameStarted),
 * 因此会出现在 /carpet 的分类浏览中:分类 "Hao",规则名 zoneguard;
 * 开关方式为 /carpet zoneguard true|false。
 * <p>
 * 本类位于共享层(所有版本通用):只依赖 carpet API,不引用任何 Minecraft 版本相关类型。
 * <p>
 * 注意:判断规则是否开启请用 {@link #isEnabled()} —— carpet 新规则系统的真实值保存在
 * 默认管理器的 {@link CarpetRule} 中,@Rule 静态字段不保证被回写,直接读字段不可靠。
 */
public class ZoneguardSettings
{
    /** Carpet 规则名(注册在 carpet 默认管理器)。 */
    public static final String RULE_NAME = "zoneguard";

    @Rule(categories = {"Hao"})
    public static boolean zoneguard = false;

    /**
     * 权威读取:Carpet 默认管理器中 zoneguard 规则当前是否为 true。
     * 在 /carpet 命令可用的时机(命令 requires、tick 内)调用是安全的;
     * 若管理器尚未初始化或规则未注册,按关闭(false)处理。
     */
    public static boolean isEnabled()
    {
        CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
        return rule != null && rule.value() instanceof Boolean enabled && enabled;
    }
}

package carpet_hao_addition.zoneguard;

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
 */
public class ZoneguardSettings
{
    @Rule(categories = {"Hao"})
    public static boolean zoneguard = false;
}

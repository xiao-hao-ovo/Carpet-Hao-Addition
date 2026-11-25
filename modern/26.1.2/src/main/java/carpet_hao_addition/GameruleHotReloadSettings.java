package carpet_hao_addition;

import carpet.api.settings.Rule;

/**
 * 「游戏规则热生效」规则。
 * <p>
 * 26.x 的生物生成被拆成 6 条独立 gamerule（{@code spawn_mobs}、{@code spawn_monsters}、
 * {@code spawn_patrols}、{@code spawn_phantoms}、{@code spawn_wandering_traders}、
 * {@code spawn_wardens}）。服务器只在启动时把判定结果缓存进各个 level，
 * 之后 {@code /gamerule} 改值不会刷新缓存，必须重进存档。
 * <p>
 * 开启本规则后，每 tick 检测这 6 条规则的取值，一旦变化就立即重算生成标志，
 * 改完 {@code /gamerule} 立刻生效。
 * <p>
 * 规则注册在 Carpet 默认管理器（{@code /carpet}），分类 "Hao"。默认关闭。
 */
public class GameruleHotReloadSettings
{
    /** Carpet 规则名：生成类游戏规则热生效。 */
    public static final String RULE_NAME = "haoGameruleHotReload";

    /**
     * 生成类游戏规则热生效。
     * <p>
     * 覆盖 {@code spawn_mobs}、{@code spawn_monsters}、{@code spawn_patrols}、
     * {@code spawn_phantoms}、{@code spawn_wandering_traders}、{@code spawn_wardens}。
     * 开启后这些规则的修改立即生效，无需重进存档。
     */
    @Rule(categories = {"Hao"}, options = {"false", "true"}, strict = false)
    public static boolean haoGameruleHotReload = false;
}
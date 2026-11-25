package carpet_hao_addition.gamerule;

import carpet_hao_addition.GameruleHotReloadSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * 让「生成类」游戏规则的修改立即生效，无需重进存档。
 *
 * <p>26.x 把生物生成拆成了 6 条独立 gamerule：{@code spawn_mobs}、{@code spawn_monsters}、
 * {@code spawn_patrols}、{@code spawn_phantoms}、{@code spawn_wandering_traders}、
 * {@code spawn_wardens}。
 *
 * <p>问题在于：服务器只在启动时调用一次 {@code updateMobSpawningFlags()}，把判定结果
 * 缓存进每个 {@code ServerLevel}（通过 {@code setSpawnSettings}）。之后用
 * {@code /gamerule} 改值只动了数据，缓存不会刷新，必须重进存档才生效。
 *
 * <p>这里每 tick 比对一次快照，发现变化就主动调 {@code updateMobSpawningFlags()}
 * 重算并写回缓存。只在规则开启时工作，规则关闭时首行即返回，零开销。
 */
public final class GameRuleHotReload
{
    /** 上一次观测到的 6 条规则取值；null 表示尚未建立基线。 */
    private static Boolean spawnMobs;
    private static Boolean spawnMonsters;
    private static Boolean spawnPatrols;
    private static Boolean spawnPhantoms;
    private static Boolean spawnWanderingTraders;
    private static Boolean spawnWardens;

    private GameRuleHotReload()
    {
    }

    /**
     * 每 tick 调用：检测生成规则是否被改动，改动则刷新生成标志。
     *
     * @param server 当前服务端实例
     */
    public static void tick(MinecraftServer server)
    {
        if (!GameruleHotReloadSettings.haoGameruleHotReload)
        {
            return;
        }

        GameRules rules = server.getGlobalGameRules();
        final boolean curMobs = rules.get(GameRules.SPAWN_MOBS);
        final boolean curMonsters = rules.get(GameRules.SPAWN_MONSTERS);
        final boolean curPatrols = rules.get(GameRules.SPAWN_PATROLS);
        final boolean curPhantoms = rules.get(GameRules.SPAWN_PHANTOMS);
        final boolean curTraders = rules.get(GameRules.SPAWN_WANDERING_TRADERS);
        final boolean curWardens = rules.get(GameRules.SPAWN_WARDENS);

        // 首次运行只建立基线，不触发刷新：此时服务器刚启动，标志本来就是新的。
        if (spawnMobs == null)
        {
            remember(curMobs, curMonsters, curPatrols, curPhantoms, curTraders, curWardens);
            return;
        }

        final boolean changed = spawnMobs != curMobs
                || spawnMonsters != curMonsters
                || spawnPatrols != curPatrols
                || spawnPhantoms != curPhantoms
                || spawnWanderingTraders != curTraders
                || spawnWardens != curWardens;

        if (changed)
        {
            server.updateMobSpawningFlags();
            remember(curMobs, curMonsters, curPatrols, curPhantoms, curTraders, curWardens);
        }
    }

    /**
     * 服务端加载时调用：清空基线，避免用上一个存档的取值做比较。
     */
    public static void reset()
    {
        spawnMobs = null;
        spawnMonsters = null;
        spawnPatrols = null;
        spawnPhantoms = null;
        spawnWanderingTraders = null;
        spawnWardens = null;
    }

    private static void remember(boolean mobs, boolean monsters, boolean patrols,
                                 boolean phantoms, boolean traders, boolean wardens)
    {
        spawnMobs = mobs;
        spawnMonsters = monsters;
        spawnPatrols = patrols;
        spawnPhantoms = phantoms;
        spawnWanderingTraders = traders;
        spawnWardens = wardens;
    }
}
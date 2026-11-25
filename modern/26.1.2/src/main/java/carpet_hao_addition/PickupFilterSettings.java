package carpet_hao_addition;

import carpet.api.settings.Rule;

/**
 * 「拾取黑白名单」规则。
 * <p>
 * 列表写法：逗号分隔，每项是物品 id 或标签。
 * <pre>
 *   /carpet haoPickupBlacklist minecraft:cobblestone, #minecraft:logs
 *   /carpet haoPickupWhitelist diamond, iron_ingot
 *   /carpet haoPickupBlacklist false   # 关闭
 *   /carpet haoPickupBlacklist clear   # 清空
 * </pre>
 * 黑名单优先：同时在两个名单里 → 不能捡。两条都空 = 完全不干预。
 * 规则注册在 Carpet 默认管理器（/carpet），分类 "Hao"。默认关闭。
 */
public class PickupFilterSettings {
    /** Carpet 规则名：黑名单。 */
    public static final String BLACKLIST_RULE_NAME = "haoPickupBlacklist";
    /** Carpet 规则名：白名单。 */
    public static final String WHITELIST_RULE_NAME = "haoPickupWhitelist";

    /**
     * 拾取黑名单：逗号分隔的物品 id 或标签，命中的物品玩家捡不起来。
     * 留空 = 不启用；写 false 关闭；写 clear 清空。
     */
    @Rule(categories = {"Hao"}, options = {"false", "clear"}, strict = false)
    public static String haoPickupBlacklist = "";

    /**
     * 拾取白名单：非空时只有列表里的物品能被捡起，其余都捡不起来。
     * 写法同黑名单；与黑名单同时命中时黑名单优先。
     */
    @Rule(categories = {"Hao"}, options = {"false", "clear"}, strict = false)
    public static String haoPickupWhitelist = "";
}
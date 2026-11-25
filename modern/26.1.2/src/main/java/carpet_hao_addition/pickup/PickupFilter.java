package carpet_hao_addition.pickup;

import carpet_hao_addition.PickupFilterSettings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * 物品拾取黑白名单。
 * <p>
 * 规则值是逗号分隔的列表，每一项可以是：
 * <ul>
 *   <li>物品 id：{@code minecraft:cobblestone}（省略命名空间时按 minecraft 处理）</li>
 *   <li>标签：{@code #minecraft:logs}</li>
 * </ul>
 * 判定顺序：黑名单命中直接拒绝；白名单非空时，只有白名单命中的物品能捡。
 * 两条都为空 = 不启用，完全不干预。
 */
public final class PickupFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger("carpet-hao-addition");

    /** 规则值：物品 id 与标签的 TagKey 都靠注册表创建，先按 id 缓存。 */
    private static String cachedBlackRaw;
    private static Entry cachedBlack;
    private static String cachedWhiteRaw;
    private static Entry cachedWhite;

    /**
     * 当前正被 {@code ServerLevel#addEntity} 添加的物品实体。
     * 用来识别「掉落物直接进背包」的那次 {@code Inventory#add}
     * （carpet 的 {@code blockDropsDirectlyEnterInventory} 就是在 addEntity 里把物品塞进玩家背包）。
     */
    private static final ThreadLocal<ItemEntity> DIRECT_INSERT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ADD_DEPTH = ThreadLocal.withInitial(() -> 0);

    /**
     * 本次方块掉落产生的、被名单拦下的 ItemStack（按对象身份）。
     * 26.x 的 carpet 走 addEntity 路径，这里只作为兜底保留。
     */
    private static final ThreadLocal<Set<ItemStack>> DROPS =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private PickupFilter() {
    }

    /** 这个物品是否禁止被玩家捡起。 */
    public static boolean denies(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Entry black = blacklist();
        if (black != null && black.matches(stack)) {
            return true;
        }
        Entry white = whitelist();
        return white != null && !white.matches(stack);
    }

    /** 进入 {@code ServerLevel#addEntity}：记下正在添加的物品实体。 */
    public static void enterAddEntity(ItemEntity entity) {
        int depth = ADD_DEPTH.get();
        if (depth == 0) {
            DIRECT_INSERT.set(entity);
        }
        ADD_DEPTH.set(depth + 1);
    }

    /** 离开 {@code ServerLevel#addEntity}。 */
    public static void exitAddEntity() {
        int depth = ADD_DEPTH.get() - 1;
        if (depth <= 0) {
            ADD_DEPTH.remove();
            DIRECT_INSERT.remove();
        } else {
            ADD_DEPTH.set(depth);
        }
    }

    /**
     * 兜底：掉落物直接进背包时是否要拦下这次 {@code Inventory#add}。
     * 只拦「同一个 ItemStack 对象」——carpet 传的就是 {@code itemEntity.getItem()}，
     * 判断身份即可，不会误伤同一 tick 里其它的正常入包。
     */
    public static boolean blocksDirectInsert(ItemStack stack) {
        ItemEntity pending = DIRECT_INSERT.get();
        if (pending != null && stack == pending.getItem() && denies(stack)) {
            return true;
        }
        // 掉落表路径的兜底（26.x 的 carpet 通常走不到这里，但保留以防版本差异）
        Set<ItemStack> drops = DROPS.get();
        return !drops.isEmpty() && drops.remove(stack);
    }

    /** 方块掉落表开始生成时调用：清掉上一次的残留。 */
    public static void beginDrops() {
        DROPS.get().clear();
    }

    /** 掉落表生成完毕：把被名单拦下的 ItemStack 记下来，等会儿拦住那次 Inventory#add。 */
    public static void markDrop(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (denies(stack)) {
            DROPS.get().add(stack);
        }
    }

    private static Entry blacklist() {
        String raw = PickupFilterSettings.haoPickupBlacklist;
        if (raw == null) {
            raw = "";
        }
        if (!raw.equals(cachedBlackRaw)) {
            cachedBlackRaw = raw;
            cachedBlack = parse(raw, "黑名单");
        }
        return cachedBlack;
    }

    private static Entry whitelist() {
        String raw = PickupFilterSettings.haoPickupWhitelist;
        if (raw == null) {
            raw = "";
        }
        if (!raw.equals(cachedWhiteRaw)) {
            cachedWhiteRaw = raw;
            cachedWhite = parse(raw, "白名单");
        }
        return cachedWhite;
    }

    private static Entry parse(String raw, String label) {
        // 整条规则支持 false 关闭、clear 清空：/carpet haoPickupBlacklist false
        String whole = stripQuotes(raw.trim());
        if (whole.equalsIgnoreCase("false") || whole.equalsIgnoreCase("off")) {
            LOGGER.info("[hao] 拾取{}已关闭（false）", label);
            return null;
        }
        if (whole.equalsIgnoreCase("clear") || whole.equalsIgnoreCase("none") || whole.equalsIgnoreCase("empty")) {
            LOGGER.info("[hao] 拾取{}已清空", label);
            return null;
        }

        List<Item> items = new ArrayList<>();
        List<TagKey<Item>> tags = new ArrayList<>();
        List<String> shown = new ArrayList<>();
        for (String token : raw.split(",")) {
            String text = stripQuotes(token.trim());
            if (text.isEmpty()) {
                continue;
            }
            if (text.startsWith("#")) {
                Identifier id = resolve(text.substring(1));
                if (id == null) {
                    LOGGER.warn("[hao] 无法解析的物品标签：{}", text);
                    continue;
                }
                tags.add(TagKey.create(net.minecraft.core.registries.Registries.ITEM, id));
                shown.add("#" + id);
            } else {
                Identifier id = resolve(text);
                if (id == null) {
                    LOGGER.warn("[hao] 无法解析的物品 id：{}", text);
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null) {
                    LOGGER.warn("[hao] 注册表里没有这个物品：{}", text);
                    continue;
                }
                items.add(item);
                shown.add(id.toString());
            }
        }
        if (items.isEmpty() && tags.isEmpty()) {
            if (!raw.isBlank()) {
                LOGGER.warn("[hao] 拾取{}没有有效条目，按未启用处理", label);
            }
            return null;
        }

        LOGGER.info("[hao] 拾取{}已启用：{}", label, String.join(", ", shown));
        return new Entry(items, tags);
    }

    /** 解析物品 id / 标签：既接受 {@code minecraft:stone}，也接受省略命名空间的 {@code stone}。 */
    private static Identifier resolve(String text) {
        Identifier id = Identifier.tryParse(text);
        if (id != null) {
            return id;
        }
        try {
            return Identifier.withDefaultNamespace(text);
        } catch (Exception invalid) {
            return null;
        }
    }

    /** 规则值是通过命令输入的，玩家习惯写 {@code ""} 表示清空，这里把成对引号去掉。 */
    private static String stripQuotes(String text) {
        // 命令里写 "" 时，carpet 会把值存成单个引号字符，这里也当成空
        if (text.equals("\"") || text.equals("'")) {
            return "";
        }
        if (text.length() >= 2) {
            char first = text.charAt(0);
            char last = text.charAt(text.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return text.substring(1, text.length() - 1).trim();
            }
        }
        return text;
    }

    /** 解析后的名单：物品与标签各一组，命中任一即可。 */
    private record Entry(List<Item> items, List<TagKey<Item>> tags) {
        boolean matches(ItemStack stack) {
            for (Item item : items) {
                if (stack.is(item)) {
                    return true;
                }
            }
            for (TagKey<Item> tag : tags) {
                if (stack.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }
}
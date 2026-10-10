package carpet_hao_addition.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType; // 26.1.2/26.2 里类名还是单数 EntityType
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 「这个投影实体要花什么」的材料清单,供选择器界面显示(26.x 版:Mojang 名)。
 * <p>
 * 计算口径必须与服务端 {@code EntityPlacementHandler.COST} 一致(那边才是真正扣料的地方),
 * 这里只是把同一套规则搬到客户端来"预告"。包含三部分:
 * <ol>
 *   <li><b>实体本身</b>:载具/装饰各 1 个;生物按原版生成原料(雪傀儡 = 2 雪块 + 1 雕刻南瓜);</li>
 *   <li><b>内容物</b>:{@code Item}(掉落实体)、{@code Items}(容器)、
 *       {@code HandItems}/{@code ArmorItems}(装备);</li>
 *   <li><b>乘客</b>:递归(竹筏上驮着的雪傀儡也要算进去)。</li>
 * </ol>
 */
public final class EntityPlacementCosts {
	/** 可能承载"物品堆"的 NBT 字段(内容物),与服务端保持一致。 */
	private static final String[] CONTENT_FIELDS = {"Items", "HandItems", "ArmorItems"};

	/** 实体类型 → 需要的物品(每种的数量)。顺序即界面里的显示顺序。 */
	private static final Map<EntityType<?>, Map<Item, Integer>> COST = new LinkedHashMap<>();

	static {
		one(EntityType.MINECART, Items.MINECART);
		one(EntityType.CHEST_MINECART, Items.CHEST_MINECART);
		one(EntityType.FURNACE_MINECART, Items.FURNACE_MINECART);
		one(EntityType.HOPPER_MINECART, Items.HOPPER_MINECART);
		one(EntityType.TNT_MINECART, Items.TNT_MINECART);
		one(EntityType.COMMAND_BLOCK_MINECART, Items.COMMAND_BLOCK_MINECART);
		one(EntityType.ARMOR_STAND, Items.ARMOR_STAND);
		one(EntityType.ITEM_FRAME, Items.ITEM_FRAME);
		one(EntityType.GLOW_ITEM_FRAME, Items.GLOW_ITEM_FRAME);
		one(EntityType.PAINTING, Items.PAINTING);
		one(EntityType.END_CRYSTAL, Items.END_CRYSTAL);
		one(EntityType.OAK_BOAT, Items.OAK_BOAT);
		one(EntityType.SPRUCE_BOAT, Items.SPRUCE_BOAT);
		one(EntityType.BIRCH_BOAT, Items.BIRCH_BOAT);
		one(EntityType.JUNGLE_BOAT, Items.JUNGLE_BOAT);
		one(EntityType.ACACIA_BOAT, Items.ACACIA_BOAT);
		one(EntityType.DARK_OAK_BOAT, Items.DARK_OAK_BOAT);
		one(EntityType.MANGROVE_BOAT, Items.MANGROVE_BOAT);
		one(EntityType.CHERRY_BOAT, Items.CHERRY_BOAT);
		one(EntityType.PALE_OAK_BOAT, Items.PALE_OAK_BOAT);
		one(EntityType.BAMBOO_RAFT, Items.BAMBOO_RAFT);
		one(EntityType.OAK_CHEST_BOAT, Items.OAK_CHEST_BOAT);
		one(EntityType.SPRUCE_CHEST_BOAT, Items.SPRUCE_CHEST_BOAT);
		one(EntityType.BIRCH_CHEST_BOAT, Items.BIRCH_CHEST_BOAT);
		one(EntityType.JUNGLE_CHEST_BOAT, Items.JUNGLE_CHEST_BOAT);
		one(EntityType.ACACIA_CHEST_BOAT, Items.ACACIA_CHEST_BOAT);
		one(EntityType.DARK_OAK_CHEST_BOAT, Items.DARK_OAK_CHEST_BOAT);
		one(EntityType.MANGROVE_CHEST_BOAT, Items.MANGROVE_CHEST_BOAT);
		one(EntityType.CHERRY_CHEST_BOAT, Items.CHERRY_CHEST_BOAT);
		one(EntityType.PALE_OAK_CHEST_BOAT, Items.PALE_OAK_CHEST_BOAT);
		one(EntityType.BAMBOO_CHEST_RAFT, Items.BAMBOO_CHEST_RAFT);
		// —— 生物实体:按原版生成原料 ——
		COST.put(EntityType.WITHER, ordered(Items.SOUL_SAND, 4, Items.WITHER_SKELETON_SKULL, 3));
		COST.put(EntityType.IRON_GOLEM, ordered(Items.IRON_BLOCK, 4, Items.CARVED_PUMPKIN, 1));
		COST.put(EntityType.SNOW_GOLEM, ordered(Items.SNOW_BLOCK, 2, Items.CARVED_PUMPKIN, 1));
		one(EntityType.ENDERMITE, Items.ENDER_PEARL);
	}

	private EntityPlacementCosts() {
	}

	private static void one(EntityType<?> type, Item item) {
		COST.put(type, Map.of(item, 1));
	}

	private static Map<Item, Integer> ordered(Item first, int firstCount, Item second, int secondCount) {
		Map<Item, Integer> map = new LinkedHashMap<>();
		map.put(first, firstCount);
		map.put(second, secondCount);
		return map;
	}

	/**
	 * <b>只算实体本身</b>需要的材料(不含内容物与乘客)。
	 * <p>
	 * 用途:判断"能不能放"。内容物/乘客是"有就带、没有就不带"的附加项 ——
	 * 投影里漏斗矿车装着 5 组骨粉、而你身上没骨粉时,依然应该把漏斗矿车放出来(只是不带骨粉)。
	 */
	public static Map<Item, Integer> baseCostFor(EntityType<?> type, int count) {
		Map<Item, Integer> need = new LinkedHashMap<>();
		Map<Item, Integer> cost = type == null ? null : COST.get(type);
		if (cost != null) {
			int amount = Math.max(1, count);
			cost.forEach((item, each) -> need.merge(item, each * amount, Integer::sum));
		}
		return need;
	}

	/** 这一个投影实体(含乘客与内容物)需要的材料清单,按 {@code count} 份计。 */
	public static Map<Item, Integer> materialsFor(EntityType<?> type, CompoundTag nbt, int count) {
		Map<Item, Integer> need = new LinkedHashMap<>();
		collect(type, nbt, Math.max(1, count), need);
		return need;
	}

	private static void collect(EntityType<?> type, CompoundTag nbt, int count, Map<Item, Integer> need) {
		if (type != null) {
			Map<Item, Integer> cost = COST.get(type);
			if (cost != null) {
				cost.forEach((item, amount) -> need.merge(item, amount * count, Integer::sum));
			}
		}
		// 内容物:单个物品(掉落实体)+ 列表(容器/装备)
		CompoundTag single = nbt.getCompound("Item").orElse(null);
		if (single != null) {
			store(single, count, need);
		}
		for (String field : CONTENT_FIELDS) {
			ListTag list = nbt.getList(field).orElse(null);
			if (list == null) {
				continue;
			}
			for (int index = 0; index < list.size(); index++) {
				if (list.get(index) instanceof CompoundTag stack) {
					store(stack, count, need);
				}
			}
		}
		// 乘客(含嵌套)
		ListTag passengers = nbt.getList("Passengers").orElse(null);
		if (passengers == null) {
			return;
		}
		for (int index = 0; index < passengers.size(); index++) {
			if (passengers.get(index) instanceof CompoundTag passenger) {
				collect(typeOf(passenger), passenger, count, need);
			}
		}
	}

	/** 把一个物品堆的 NBT({@code id} / {@code count})计入清单。 */
	private static void store(CompoundTag stack, int count, Map<Item, Integer> need) {
		String raw = stack.getString("id").orElse("");
		Identifier id = raw.isEmpty() ? null : Identifier.tryParse(raw);
		if (id == null) {
			return;
		}
		Item item = BuiltInRegistries.ITEM.getValue(id);
		if (item == null || item == Items.AIR) {
			return;
		}
		int amount = stack.getInt("count").orElse(1);
		need.merge(item, Math.max(1, amount) * count, Integer::sum);
	}

	private static EntityType<?> typeOf(CompoundTag nbt) {
		String raw = nbt.getString("id").orElse("");
		Identifier id = raw.isEmpty() ? null : Identifier.tryParse(raw);
		return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(id);
	}
}

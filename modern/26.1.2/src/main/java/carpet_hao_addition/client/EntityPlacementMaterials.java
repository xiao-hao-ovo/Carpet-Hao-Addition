package carpet_hao_addition.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 客户端侧的「实体类型 → 代表物品」映射(26.x 版:Mojang 名)。
 * <p>
 * 只做两件事:① 选择器里给每个候选画个图标;② 放置前粗筛"背包里到底有没有这个东西",
 * 没有就不产生候选(参考 QuickCraft:没材料就不该出现在候选里,而不是点了之后被服务端默默拒绝)。
 * <p>
 * 真正的材料校验与扣除仍然由服务端 {@code EntityPlacementHandler} 负责 —— 这里认不出来的一律
 * 放行(返回 null 视为"无法判断"),宁可多显示一个也不要把能放的挡掉。
 */
public final class EntityPlacementMaterials {
	private EntityPlacementMaterials() {
	}

	/** 该实体对应的物品;返回 {@code null} 表示"认不出来,交给服务端判断"。 */
	public static Item itemFor(EntityType<?> type, CompoundTag nbt) {
		if (type == null) {
			return null;
		}
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		if (id == null || !"minecraft".equals(id.getNamespace())) {
			return null;
		}
		Item special = specialItem(id.getPath());
		if (special != null) {
			return special;
		}
		// 掉落实体带的是它自己"装着"的物品
		if ("item".equals(id.getPath())) {
			return storedItem(nbt);
		}
		// 船 / 箱船:实体 id 与物品 id 同名(oak_boat、oak_chest_boat ...)
		Item same = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id.getPath()));
		if (same != Items.AIR) {
			return same;
		}
		// 其余普通生物:刷怪蛋
		Item egg = BuiltInRegistries.ITEM.getValue(
				Identifier.withDefaultNamespace(id.getPath() + "_spawn_egg"));
		return egg == Items.AIR ? null : egg;
	}

	/** 界面用的一格图标(空物品表示没有图标)。 */
	public static ItemStack iconFor(Identifier entityId, CompoundTag nbt) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
		Item item = itemFor(type, nbt);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}

	private static Item specialItem(String path) {
		return switch (path) {
			case "armor_stand" -> Items.ARMOR_STAND;
			case "item_frame" -> Items.ITEM_FRAME;
			case "glow_item_frame" -> Items.GLOW_ITEM_FRAME;
			case "painting" -> Items.PAINTING;
			case "end_crystal" -> Items.END_CRYSTAL;
			case "minecart" -> Items.MINECART;
			case "chest_minecart" -> Items.CHEST_MINECART;
			case "hopper_minecart" -> Items.HOPPER_MINECART;
			case "furnace_minecart" -> Items.FURNACE_MINECART;
			case "tnt_minecart" -> Items.TNT_MINECART;
			case "command_block_minecart" -> Items.COMMAND_BLOCK_MINECART;
			case "spawner_minecart" -> Items.MINECART;
			// 构造型生物:代表的"关键材料",用于粗筛
			case "wither" -> Items.SOUL_SAND;
			case "iron_golem" -> Items.IRON_BLOCK;
			case "snow_golem" -> Items.SNOW_BLOCK;
			case "endermite" -> Items.ENDER_PEARL;
			default -> null;
		};
	}

	/** 掉落实体 NBT 里 {@code Item.id} 指向的物品。 */
	private static Item storedItem(CompoundTag nbt) {
		if (nbt == null) {
			return null;
		}
		CompoundTag item = nbt.getCompound("Item").orElse(null);
		if (item == null) {
			return null;
		}
		String raw = item.getString("id").orElse("");
		Identifier itemId = raw.isEmpty() ? null : Identifier.tryParse(raw);
		if (itemId == null) {
			return null;
		}
		Item stored = BuiltInRegistries.ITEM.getValue(itemId);
		return stored == Items.AIR ? null : stored;
	}
}

package carpet_hao_addition;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType; // 26.1.2/26.2 里类名还是单数 EntityType
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * easyPlaceEntity 的服务端实现:按客户端上报的实体 NBT,在投影对应的位置还原实体,并扣除对应物品。
 * <p>
 * 消耗规则:
 * <ul>
 *   <li><b>实体本身与乘客</b>(如"矿车镶在船里")—— <b>必需</b>:付不起就把该乘客从 NBT 里剔掉,
 *       主实体照常放(有多少材料放多少);</li>
 *   <li><b>实体携带的内容物</b>(展示框里的物品 {@code Item}、容器内容 {@code Items}、
 *       盔甲架装备 {@code HandItems}/{@code ArmorItems})—— <b>能付得起就一起放、一起扣</b>;
 *       付不起就把该字段从 NBT 里剔掉,只还原实体本身(不会只放一半、也不会白扣)。</li>
 * </ul>
 * 只有<b>有对应物品</b>的实体才计入消耗;没有对应物品的实体(村民等)不扣料。
 * <p>
 * 26.x 的实体 NBT 还原走 {@link EntityType#loadEntityRecursive(CompoundTag, net.minecraft.world.level.Level,
 * EntitySpawnRequest, EntityProcessor)}(内部会递归创建 {@code Passengers}),
 * 加入世界用 {@code ServerLevel#tryAddFreshEntityWithPassengers}:乘客也会真正进入实体追踪,
 * 否则客户端只知道它"挂在载具上"却不会渲染。
 * <p>
 * 去重(同一处不重复放)由客户端按"世界里已有实体数 ≥ 投影要求数"负责,服务端不再按 key 拦
 * —— 否则会出现"客户端能点、服务端却跳过"的不一致。
 */
public final class EntityPlacementHandler {
	private static final String TAG = "[hao-entity] [服务端]";
	private static boolean payloadRegistered;
	private static boolean receiverRegistered;

	/**
	 * 实体类型 → 需要的物品(每种的数量)。
	 * <p>
	 * "载具/装饰"类实体各要 1 个对应物品;生物类实体按<b>原版的生成原料</b>计:
	 * 凋灵=4 灵魂沙 + 3 凋灵骷髅头,铁傀儡=4 铁块 + 1 雕刻南瓜,雪傀儡=2 雪块 + 1 雕刻南瓜,
	 * 末影螨=1 末影珍珠。原料不齐就不放。
	 */
	private static final Map<EntityType<?>, Map<Item, Integer>> COST = Map.<EntityType<?>, Map<Item, Integer>>ofEntries(
			Map.entry(EntityType.MINECART, Map.of(Items.MINECART, 1)),
			Map.entry(EntityType.CHEST_MINECART, Map.of(Items.CHEST_MINECART, 1)),
			Map.entry(EntityType.FURNACE_MINECART, Map.of(Items.FURNACE_MINECART, 1)),
			Map.entry(EntityType.HOPPER_MINECART, Map.of(Items.HOPPER_MINECART, 1)),
			Map.entry(EntityType.TNT_MINECART, Map.of(Items.TNT_MINECART, 1)),
			Map.entry(EntityType.COMMAND_BLOCK_MINECART, Map.of(Items.COMMAND_BLOCK_MINECART, 1)),
			Map.entry(EntityType.ARMOR_STAND, Map.of(Items.ARMOR_STAND, 1)),
			Map.entry(EntityType.ITEM_FRAME, Map.of(Items.ITEM_FRAME, 1)),
			Map.entry(EntityType.GLOW_ITEM_FRAME, Map.of(Items.GLOW_ITEM_FRAME, 1)),
			Map.entry(EntityType.PAINTING, Map.of(Items.PAINTING, 1)),
			Map.entry(EntityType.END_CRYSTAL, Map.of(Items.END_CRYSTAL, 1)),
			Map.entry(EntityType.OAK_BOAT, Map.of(Items.OAK_BOAT, 1)),
			Map.entry(EntityType.SPRUCE_BOAT, Map.of(Items.SPRUCE_BOAT, 1)),
			Map.entry(EntityType.BIRCH_BOAT, Map.of(Items.BIRCH_BOAT, 1)),
			Map.entry(EntityType.JUNGLE_BOAT, Map.of(Items.JUNGLE_BOAT, 1)),
			Map.entry(EntityType.ACACIA_BOAT, Map.of(Items.ACACIA_BOAT, 1)),
			Map.entry(EntityType.DARK_OAK_BOAT, Map.of(Items.DARK_OAK_BOAT, 1)),
			Map.entry(EntityType.MANGROVE_BOAT, Map.of(Items.MANGROVE_BOAT, 1)),
			Map.entry(EntityType.CHERRY_BOAT, Map.of(Items.CHERRY_BOAT, 1)),
			Map.entry(EntityType.PALE_OAK_BOAT, Map.of(Items.PALE_OAK_BOAT, 1)),
			Map.entry(EntityType.BAMBOO_RAFT, Map.of(Items.BAMBOO_RAFT, 1)),
			Map.entry(EntityType.OAK_CHEST_BOAT, Map.of(Items.OAK_CHEST_BOAT, 1)),
			Map.entry(EntityType.SPRUCE_CHEST_BOAT, Map.of(Items.SPRUCE_CHEST_BOAT, 1)),
			Map.entry(EntityType.BIRCH_CHEST_BOAT, Map.of(Items.BIRCH_CHEST_BOAT, 1)),
			Map.entry(EntityType.JUNGLE_CHEST_BOAT, Map.of(Items.JUNGLE_CHEST_BOAT, 1)),
			Map.entry(EntityType.ACACIA_CHEST_BOAT, Map.of(Items.ACACIA_CHEST_BOAT, 1)),
			Map.entry(EntityType.DARK_OAK_CHEST_BOAT, Map.of(Items.DARK_OAK_CHEST_BOAT, 1)),
			Map.entry(EntityType.MANGROVE_CHEST_BOAT, Map.of(Items.MANGROVE_CHEST_BOAT, 1)),
			Map.entry(EntityType.CHERRY_CHEST_BOAT, Map.of(Items.CHERRY_CHEST_BOAT, 1)),
			Map.entry(EntityType.PALE_OAK_CHEST_BOAT, Map.of(Items.PALE_OAK_CHEST_BOAT, 1)),
			Map.entry(EntityType.BAMBOO_CHEST_RAFT, Map.of(Items.BAMBOO_CHEST_RAFT, 1)),
			// —— 生物实体:按原版生成原料消耗 ——
			Map.entry(EntityType.WITHER, Map.of(Items.SOUL_SAND, 4, Items.WITHER_SKELETON_SKULL, 3)),
			Map.entry(EntityType.IRON_GOLEM, Map.of(Items.IRON_BLOCK, 4, Items.CARVED_PUMPKIN, 1)),
			Map.entry(EntityType.SNOW_GOLEM, Map.of(Items.SNOW_BLOCK, 2, Items.CARVED_PUMPKIN, 1)),
			Map.entry(EntityType.ENDERMITE, Map.of(Items.ENDER_PEARL, 1)));

	/** 可能承载"物品堆"的 NBT 字段(内容物)。 */
	private static final String[] CONTENT_FIELDS = {"Items", "HandItems", "ArmorItems"};

	private EntityPlacementHandler() {
	}

	/** 注册 payload 类型(客户端与服务端都调用,幂等)。 */
	public static void registerPayloadType() {
		if (payloadRegistered) {
			return;
		}
		payloadRegistered = true;
		PayloadTypeRegistry.serverboundPlay().register(EntityPlacementPayload.ID, EntityPlacementPayload.CODEC);
	}

	/** 注册服务端接收端(幂等)。 */
	public static void registerServerReceiver() {
		registerPayloadType();
		if (receiverRegistered) {
			return;
		}
		receiverRegistered = true;
		ServerPlayNetworking.registerGlobalReceiver(EntityPlacementPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			System.out.println(TAG + " 收到包: player=" + player.getName().getString()
					+ " 实体数=" + payload.spawns().size() + " count=" + payload.count());
			context.server().execute(() -> spawnEntity(player, payload));
		});
	}

	/** 逐条放出实体;每条都先确认这一格没有同类型实体。 */
	private static void spawnEntity(ServerPlayer player, EntityPlacementPayload payload) {
		if (!EasyPlaceEntitySettings.isEnabled()) {
			System.out.println(TAG + " 忽略: 规则未开");
			return;
		}
		if (!(player.level() instanceof ServerLevel world)) {
			return;
		}
		for (EntitySpawn spawn : payload.spawns()) {
			spawnOne(player, world, spawn, payload.count());
		}
	}

	/** 放出一条:实体/乘客必需,内容物能付则一并放出并消耗。 */
	private static void spawnOne(ServerPlayer player, ServerLevel world, EntitySpawn spawn, int rawCount) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(spawn.type());
		if (type == null) {
			System.out.println(TAG + " 跳过: 未知实体类型 " + spawn.type());
			return;
		}
		int count = Math.max(1, Math.min(EasyPlaceEntitySettings.MAX_COUNT, rawCount));
		CompoundTag nbt = spawn.nbt().copy();
		// 乘客(以及嵌套乘客)的 Pos 存的是**投影局部坐标**,必须改成载具当前的世界坐标,
		// 否则它们会被放到离载具几千格之外(表现就是"看不见乘客,打掉载具它才掉出来")。
		fixPassengerPositions(nbt, spawn.x(), spawn.y(), spawn.z());
		Map<Item, Integer> need = new LinkedHashMap<>();
		// ① 实体本身:计入消耗(不拦,以客户端判定为准 —— 客户端只把缺料标红,仍然允许点)。
		Map<Item, Integer> rootCost = COST.get(type);
		if (rootCost != null) {
			rootCost.forEach((item, amount) -> need.merge(item, amount * count, Integer::sum));
		}
		// ② 乘客:付得起的保留,付不起的**只去掉这一位** —— 例如"竹筏驮着雪傀儡",
		//    背包只有竹筏就只放竹筏;有竹筏 + 雪块 + 雕刻南瓜才连雪傀儡一起放。
		prunePassengers(player, nbt, count, need);
		// ③ 内容物:付得起的保留并计入消耗,付不起的整段剔除(只放实体本身)。
		pruneContents(player, nbt, count, need);
		for (Map.Entry<Item, Integer> entry : need.entrySet()) {
			consume(player, entry.getKey(), entry.getValue());
		}
		for (int i = 0; i < count; i++) {
			// 用(可能被修剪过的)NBT 还原:loadEntityRecursive 会连同 Passengers 一起创建。
			Entity entity = EntityType.loadEntityRecursive(nbt, world,
					EntitySpawnReason.COMMAND, EntityProcessor.NOP);
			if (entity == null) {
				System.out.println(TAG + " 还原失败 " + spawn.type() + "(NBT 无法创建实体)");
				break;
			}
			entity.snapTo(spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
			boolean added = world.tryAddFreshEntityWithPassengers(entity);
			System.out.println(TAG + " 生成 " + entity.getType() + " @ "
					+ String.format("%.3f,%.3f,%.3f", entity.getX(), entity.getY(), entity.getZ())
					+ " 投影要求=" + String.format("%.3f,%.3f,%.3f", spawn.x(), spawn.y(), spawn.z())
					+ " yaw=" + String.format("%.1f", spawn.yaw())
					+ " 加进世界=" + added + " 乘客数=" + entity.getPassengers().size());
		}
		System.out.println(TAG + " 完成 " + spawn.type() + " x" + count + " 消耗=" + need);
	}

	/**
	 * 乘客(含嵌套乘客):付得起就保留并计入消耗,付不起就把这一位**从 NBT 里去掉**,
	 * 这样能"有多少材料放多少"。
	 */
	private static void prunePassengers(ServerPlayer player, CompoundTag nbt, int count, Map<Item, Integer> need) {
		ListTag passengers = nbt.getList("Passengers").orElse(null);
		if (passengers == null) {
			return;
		}
		ListTag kept = new ListTag();
		for (int i = 0; i < passengers.size(); i++) {
			if (!(passengers.get(i) instanceof CompoundTag passenger)) {
				continue;
			}
			EntityType<?> passengerType = typeOf(passenger);
			// 先"试算"这一位(连同它自己驮着的)需要什么,够才真的保留
			Map<Item, Integer> sub = new LinkedHashMap<>();
			if (passengerType != null && collectEntities(passengerType, passenger, count, player, sub)) {
				prunePassengers(player, passenger, count, sub);
				sub.forEach((item, amount) -> need.merge(item, amount, Integer::sum));
				kept.add(passenger);
			} else {
				String passengerId = passenger.getString("id").orElse("?");
				System.out.println(TAG + " 乘客材料不足,已剔除 " + passengerId);
				player.sendSystemMessage(Component.translatable(
						"carpet-hao-addition.easyplaceentity.missingPassenger", passengerId));
			}
		}
		if (kept.isEmpty()) {
			nbt.remove("Passengers");
		} else {
			nbt.put("Passengers", kept);
		}
	}

	/** 统计实体与乘客需要的物品;任一不够返回 false。 */
	private static boolean collectEntities(EntityType<?> type, CompoundTag nbt, int count,
			ServerPlayer player, Map<Item, Integer> need) {
		Map<Item, Integer> cost = COST.get(type);
		if (cost != null) {
			cost.forEach((item, amount) -> need.merge(item, amount * count, Integer::sum));
		}
		ListTag passengers = nbt.getList("Passengers").orElse(null);
		if (passengers != null) {
			for (int i = 0; i < passengers.size(); i++) {
				if (passengers.get(i) instanceof CompoundTag passenger) {
					EntityType<?> passengerType = typeOf(passenger);
					if (passengerType != null && !collectEntities(passengerType, passenger, count, player, need)) {
						return false;
					}
				}
			}
		}
		for (Map.Entry<Item, Integer> entry : need.entrySet()) {
			if (count(player, entry.getKey()) < entry.getValue()) {
				return false;
			}
		}
		return true;
	}

	/** 内容物:付得起的保留(计入 need),付不起的把整个字段从 NBT 里删掉。 */
	private static void pruneContents(ServerPlayer player, CompoundTag nbt, int count, Map<Item, Integer> need) {
		CompoundTag single = nbt.getCompound("Item").orElse(null);
		if (single != null && !payable(player, single, count, need)) {
			nbt.remove("Item");
			System.out.println(TAG + " 内容物不足,已剔除 Item");
		}
		for (String field : CONTENT_FIELDS) {
			ListTag list = nbt.getList(field).orElse(null);
			if (list == null) {
				continue;
			}
			Map<Item, Integer> sub = new LinkedHashMap<>();
			boolean payableValue = true;
			for (int i = 0; i < list.size() && payableValue; i++) {
				if (list.get(i) instanceof CompoundTag stack) {
					payableValue = payable(player, stack, count, sub);
				}
			}
			if (payableValue) {
				sub.forEach((item, amount) -> need.merge(item, amount, Integer::sum));
			} else {
				nbt.remove(field);
				System.out.println(TAG + " 内容物不足,已剔除 " + field);
				player.sendSystemMessage(Component.translatable(
						"carpet-hao-addition.easyplaceentity.missingContents", field));
			}
		}
	}

	/** 单个物品堆能否支付(按 count 倍计算);能付则把需求记进 {@code need}。 */
	private static boolean payable(ServerPlayer player, CompoundTag stackNbt, int count, Map<Item, Integer> need) {
		String id = stackNbt.getString("id").orElse("");
		Identifier identifier = id.isEmpty() ? null : Identifier.tryParse(id);
		Item item = identifier == null ? null : BuiltInRegistries.ITEM.getValue(identifier);
		if (item == null || item == Items.AIR) {
			return true; // 认不出来就不拦:留着,也不计消耗
		}
		int amount = Math.max(1, stackNbt.getInt("count").orElse(1)) * count;
		if (count(player, item) < amount) {
			return false;
		}
		need.merge(item, amount, Integer::sum);
		return true;
	}

	/** 递归把乘客(及嵌套乘客)的 Pos 改写成载具的世界坐标:它们存的是投影局部坐标。 */
	private static void fixPassengerPositions(CompoundTag vehicleNbt, double x, double y, double z) {
		ListTag passengers = vehicleNbt.getList("Passengers").orElse(null);
		if (passengers == null) {
			return;
		}
		for (int i = 0; i < passengers.size(); i++) {
			if (!(passengers.get(i) instanceof CompoundTag passenger)) {
				continue;
			}
			ListTag pos = new ListTag();
			pos.add(DoubleTag.valueOf(x));
			pos.add(DoubleTag.valueOf(y));
			pos.add(DoubleTag.valueOf(z));
			passenger.put("Pos", pos);
			fixPassengerPositions(passenger, x, y, z);
		}
	}

	/** 读 NBT 里的实体类型。 */
	private static EntityType<?> typeOf(CompoundTag nbt) {
		String id = nbt.getString("id").orElse("");
		Identifier identifier = id.isEmpty() ? null : Identifier.tryParse(id);
		return identifier == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
	}

	/** 背包里某物品的总数。 */
	private static int count(ServerPlayer player, Item item) {
		int total = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** 从背包里扣掉 amount 个该物品(调用前已确认足够)。 */
	private static void consume(ServerPlayer player, Item item, int amount) {
		int remaining = amount;
		for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty() || !stack.is(item)) {
				continue;
			}
			int take = Math.min(remaining, stack.getCount());
			stack.shrink(take);
			remaining -= take;
		}
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
	}
}

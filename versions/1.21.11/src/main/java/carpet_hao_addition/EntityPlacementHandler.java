package carpet_hao_addition;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * haoProjectionEntity 的服务端实现:按客户端上报的实体 NBT,在投影对应的位置还原实体,并扣除对应物品。
 * <p>
 * 消耗规则:
 * <ul>
 *   <li><b>实体本身与乘客</b>(如"矿车镶在船里")—— **必需**:任一种不够就整条跳过,不放也不扣;</li>
 *   <li><b>实体携带的内容物</b>(展示框里的物品 {@code Item}、容器内容 {@code Items}、
 *       盔甲架装备 {@code HandItems}/{@code ArmorItems})—— **能付得起就一起放、一起扣**;
 *       付不起就把该字段从 NBT 里剔掉,只还原实体本身(不会只放一半、也不会白扣)。</li>
 * </ul>
 * 只有**有对应物品**的实体才计入消耗;没有对应物品的实体(村民等)忽略。
 */
public final class EntityPlacementHandler {
	private static boolean payloadRegistered;
	private static boolean receiverRegistered;

	/**
	 * 实体类型 → 需要的物品(每种的数量)。
	 * <p>
	 * "载具/装饰"类实体各要 1 个对应物品;生物类实体按**原版的生成原料**计:
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

	/**
	 * 每个玩家"已经放过"的投影实体身份({@code EntitySpawn.key = 子区域#序号})。
	 * <p>
	 * 去重必须按身份、不能按坐标:投影里常见几辆漏斗矿车叠在同一个点上,坐标与 NBT 都一样、
	 * 只有 UUID 不同 —— 按坐标判定会让第二辆永远放不出来。
	 */
	private static final Map<UUID, Map<String, Integer>> PLACED_KEYS = new HashMap<>();

	private EntityPlacementHandler() {
	}

	/** 注册 payload 类型(幂等)。 */
	public static void registerPayloadType() {
		if (payloadRegistered) {
			return;
		}
		payloadRegistered = true;
		PayloadTypeRegistry.playC2S().register(EntityPlacementPayload.ID, EntityPlacementPayload.CODEC);
	}

	/** 注册服务端接收端(幂等)。 */
	public static void registerServerReceiver() {
		registerPayloadType();
		if (receiverRegistered) {
			return;
		}
		receiverRegistered = true;
		ServerPlayNetworking.registerGlobalReceiver(EntityPlacementPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			context.server().execute(() -> hao$spawnEntity(player, payload));
		});
	}

	/** 逐条放出实体;每条都先确认这一格没有同类型实体。 */
	private static void hao$spawnEntity(ServerPlayerEntity player, EntityPlacementPayload payload) {
		if (!EasyPlaceEntitySettings.isEnabled()) {
			return;
		}
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		for (EntitySpawn spawn : payload.spawns()) {
			hao$spawnOne(player, world, spawn, payload.count());
		}
	}

	/** 放出一条:实体/乘客必需,内容物能付则一并放出并消耗。 */
	private static void hao$spawnOne(ServerPlayerEntity player, ServerWorld world, EntitySpawn spawn,
			int rawCount) {
		EntityType<?> type = Registries.ENTITY_TYPE.get(spawn.type());
		if (type == null) {
			return;
		}
		// 去重按**投影实体身份**(EntitySpawn.key = 子区域#序号),不按坐标 ——
		// 投影里可能有几个坐标完全重合的实体(两辆叠在一起的漏斗矿车),按坐标去重
		// 会让第二个永远放不出来。key 为空(旧版客户端)时才退化成"不检查"。
		// 记账:key → 上次生成的实体 ID。只有那个实体**还活着**才算"已经放过";
		// 一旦被你打掉,记录自动失效,可以重新放(否则会"放过一次就永久不能再放")。
		Map<String, Integer> placedKeys = PLACED_KEYS.computeIfAbsent(player.getUuid(),
				ignored -> new HashMap<>());
		String key = spawn.key();
		if (!key.isEmpty()) {
			Integer previous = placedKeys.get(key);
			if (previous != null && world.getEntityById(previous) != null) {
				System.out.println("[hao-entity]   跳过 " + spawn.type() + ":这个投影实体已经放过了");
				return;
			}
		}
		int count = Math.max(1, Math.min(EasyPlaceEntitySettings.MAX_COUNT, rawCount));
		NbtCompound nbt = spawn.nbt().copy();
		// 乘客(以及嵌套乘客)的 Pos 存的是**投影局部坐标**,必须改成载具当前的世界坐标,
		// 否则它们会被放到离载具几千格之外(表现就是"看不见乘客,打掉载具它才掉出来")。
		hao$fixPassengerPositions(nbt, spawn.x(), spawn.y(), spawn.z());
		Map<Item, Integer> need = new LinkedHashMap<>();
		// ① 实体本身:必需 —— 连它都付不起就整条跳过(剩下的都没意义)。
		Map<Item, Integer> rootCost = COST.get(type);
		if (rootCost != null) {
			rootCost.forEach((item, amount) -> need.merge(item, amount * count, Integer::sum));
		}
		for (Map.Entry<Item, Integer> entry : need.entrySet()) {
			if (hao$count(player, entry.getKey()) < entry.getValue()) {
				System.out.println("[hao-entity]   跳过 " + spawn.type() + ":实体本身的物品不足");
				return;
			}
		}
		// ② 乘客:付得起的保留,付不起的**只去掉这一位** —— 例如"竹筏驮着雪傀儡",
		//    背包只有竹筏就只放竹筏;有竹筏 + 雪块 + 雕刻南瓜才连雪傀儡一起放。
		hao$prunePassengers(player, nbt, count, need);
		// ③ 内容物:付得起的保留并计入消耗,付不起的整段剔除(只放实体本身)。
		hao$pruneContents(player, nbt, count, need);
		for (Map.Entry<Item, Integer> entry : need.entrySet()) {
			hao$consume(player, entry.getKey(), entry.getValue());
		}
		for (int i = 0; i < count; i++) {
			// 用(可能被修剪过的)NBT 还原:loadEntityWithPassengers 会把乘客一并创建,
			// 入世界必须用 spawnNewEntityAndPassengers,否则乘客不会真正加进世界。
			Entity entity = EntityType.loadEntityWithPassengers(nbt, world, SpawnReason.COMMAND, loaded -> loaded);
			if (entity == null) {
				System.out.println("[hao-entity]   还原失败 " + spawn.type() + "(NBT 无法创建实体)");
				break;
			}
			entity.refreshPositionAndAngles(spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
			boolean added = world.spawnNewEntityAndPassengers(entity);
			System.out.println("[hao-entity]   生成后 " + entity.getType() + " @ "
					+ String.format("%.3f,%.3f,%.3f", entity.getX(), entity.getY(), entity.getZ())
					+ " 投影要求=" + String.format("%.3f,%.3f,%.3f", spawn.x(), spawn.y(), spawn.z())
					+ " yaw=" + String.format("%.1f", spawn.yaw()));
			if (added && !key.isEmpty() && entity != null) {
				placedKeys.put(key, entity.getId()); // 生成了才记账;被打掉后自动失效
			}
			// 乘客保留**骑乘关系**(与投影完全一致:雪傀儡就是坐在竹筏上的乘客,有坐姿、会动),
			// 但要把它"已经在骑乘位"这件事**同步给客户端** —— 否则客户端会一直把它画在载具内部,
			// 表现就是"只看得见载具,打掉载具乘客才出现"。
			for (Entity passenger : new java.util.ArrayList<>(entity.getPassengerList())) {
				// 关键:乘客也必须**真正加入世界**(从而进入实体追踪、发出 spawn 包),
				// 否则客户端只会按载具的 Passengers "知道"它存在,却**不会渲染它** ——
				// 表现就是"只看得见载具,打掉载具乘客才出现"。骑乘关系保持不变(投影一致)。
				boolean addedPassenger = world.spawnNewEntityAndPassengers(passenger);
				System.out.println("[hao-entity]   乘客 " + passenger.getType() + " 加入世界="
						+ addedPassenger + " 位置=" + passenger.getX() + "," + passenger.getY()
						+ "," + passenger.getZ() + " 载具=" + entity.getX() + "," + entity.getY()
						+ "," + entity.getZ());
			}
			System.out.println("[hao-entity]   加进世界=" + added + " 乘客实况="
					+ entity.getPassengerList().size());
		}
		System.out.println("[hao-entity]   生成 " + spawn.type() + " x" + count + " 消耗=" + need);
	}

	/**
	 * 乘客(含嵌套乘客):付得起就保留并计入消耗,付不起就把这一位**从 NBT 里去掉**。
	 * <p>
	 * 与"整条跳过"相比,这样能做到"有多少材料放多少" —— 例如投影里竹筏上驮着雪傀儡,
	 * 背包只有竹筏时就只放竹筏;有竹筏 + 雪块 + 雕刻南瓜时才连雪傀儡一起放。
	 */
	private static void hao$prunePassengers(ServerPlayerEntity player, NbtCompound nbt, int count,
			Map<Item, Integer> need) {
		NbtList passengers = nbt.getList("Passengers").orElse(null);
		if (passengers == null) {
			return;
		}
		NbtList kept = new NbtList();
		for (int i = 0; i < passengers.size(); i++) {
			if (!(passengers.get(i) instanceof NbtCompound passenger)) {
				continue;
			}
			EntityType<?> passengerType = hao$typeOf(passenger);
			// 先"试算"这一位(连同它自己驮着的)需要什么,够才真的保留
			Map<Item, Integer> sub = new LinkedHashMap<>();
			if (passengerType != null
					&& hao$collectEntities(passengerType, passenger, count, player, sub)) {
				hao$prunePassengers(player, passenger, count, sub);
				sub.forEach((item, amount) -> need.merge(item, amount, Integer::sum));
				kept.add(passenger);
			} else {
				System.out.println("[hao-entity]   乘客材料不足,已剔除 "
						+ passenger.getString("id").orElse("?"));
			}
		}
		if (kept.isEmpty()) {
			nbt.remove("Passengers");
		} else {
			nbt.put("Passengers", kept);
		}
	}

	/** 统计实体与乘客需要的物品;任一不够返回 false。 */
	private static boolean hao$collectEntities(EntityType<?> type, NbtCompound nbt, int count,
			ServerPlayerEntity player, Map<Item, Integer> need) {
		Map<Item, Integer> cost = COST.get(type);
		if (cost != null) {
			cost.forEach((item, amount) -> need.merge(item, amount * count, Integer::sum));
		}
		NbtList passengers = nbt.getList("Passengers").orElseGet(NbtList::new);
		for (int i = 0; i < passengers.size(); i++) {
			if (passengers.get(i) instanceof NbtCompound passenger) {
				EntityType<?> passengerType = hao$typeOf(passenger);
				if (passengerType != null && !hao$collectEntities(passengerType, passenger, count, player, need)) {
					return false;
				}
			}
		}
		for (Map.Entry<Item, Integer> entry : need.entrySet()) {
			if (hao$count(player, entry.getKey()) < entry.getValue()) {
				return false;
			}
		}
		return true;
	}

	/** 内容物:付得起的保留(计入 need),付不起的把整个字段从 NBT 里删掉。 */
	private static void hao$pruneContents(ServerPlayerEntity player, NbtCompound nbt, int count,
			Map<Item, Integer> need) {
		NbtCompound single = nbt.getCompound("Item").orElse(null);
		if (single != null && !hao$payable(player, single, count, need)) {
			nbt.remove("Item");
			System.out.println("[hao-entity]   内容物不足,已剔除 Item");
		}
		for (String field : CONTENT_FIELDS) {
			NbtList list = nbt.getList(field).orElse(null);
			if (list == null) {
				continue;
			}
			Map<Item, Integer> sub = new LinkedHashMap<>();
			boolean payable = true;
			for (int i = 0; i < list.size() && payable; i++) {
				if (list.get(i) instanceof NbtCompound stack) {
					payable = hao$payable(player, stack, count, sub);
				}
			}
			if (payable) {
				sub.forEach((item, amount) -> need.merge(item, amount, Integer::sum));
			} else {
				nbt.remove(field);
				System.out.println("[hao-entity]   内容物不足,已剔除 " + field);
			}
		}
	}

	/** 单个物品堆能否支付(按 count 倍计算);能付则把需求记进 {@code need}。 */
	private static boolean hao$payable(ServerPlayerEntity player, NbtCompound stackNbt, int count,
			Map<Item, Integer> need) {
		String id = stackNbt.getString("id").orElse("");
		Identifier identifier = id.isEmpty() ? null : Identifier.tryParse(id);
		Item item = identifier == null ? null : Registries.ITEM.get(identifier);
		if (item == null || item == Items.AIR) {
			return true; // 认不出来就不拦:留着,也不计消耗
		}
		int amount = Math.max(1, stackNbt.getInt("count").orElse(1)) * count;
		if (hao$count(player, item) < amount) {
			return false;
		}
		need.merge(item, amount, Integer::sum);
		return true;
	}

	/** 递归把乘客(及嵌套乘客)的 Pos 改写成载具的世界坐标:它们存的是投影局部坐标。 */
	private static void hao$fixPassengerPositions(NbtCompound vehicleNbt, double x, double y, double z) {
		NbtList passengers = vehicleNbt.getList("Passengers").orElse(null);
		if (passengers == null) {
			return;
		}
		for (int i = 0; i < passengers.size(); i++) {
			if (!(passengers.get(i) instanceof NbtCompound passenger)) {
				continue;
			}
			NbtList pos = new NbtList();
			pos.add(net.minecraft.nbt.NbtDouble.of(x));
			pos.add(net.minecraft.nbt.NbtDouble.of(y));
			pos.add(net.minecraft.nbt.NbtDouble.of(z));
			passenger.put("Pos", pos);
			hao$fixPassengerPositions(passenger, x, y, z);
		}
	}

	/** 读 NBT 里的实体类型。 */
	private static EntityType<?> hao$typeOf(NbtCompound nbt) {
		String id = nbt.getString("id").orElse("");
		Identifier identifier = id.isEmpty() ? null : Identifier.tryParse(id);
		return identifier == null ? null : Registries.ENTITY_TYPE.get(identifier);
	}

	/** 背包里某物品的总数。 */
	private static int hao$count(ServerPlayerEntity player, Item item) {
		int total = 0;
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (!stack.isEmpty() && stack.isOf(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** 从背包里扣掉 amount 个该物品(调用前已确认足够)。 */
	private static void hao$consume(ServerPlayerEntity player, Item item, int amount) {
		int remaining = amount;
		for (int slot = 0; slot < player.getInventory().size() && remaining > 0; slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (stack.isEmpty() || !stack.isOf(item)) {
				continue;
			}
			int take = Math.min(remaining, stack.getCount());
			stack.decrement(take);
			remaining -= take;
		}
		player.getInventory().markDirty();
		player.currentScreenHandler.syncState();
	}
}

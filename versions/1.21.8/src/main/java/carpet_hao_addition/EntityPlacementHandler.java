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
		if (!EasyPlaceEntitySettings.isServerEnabled()) {
			return;
		}
		if (!(player.getWorld() instanceof ServerWorld world)) {
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
		// 去重**完全交给客户端**:客户端按「该位置世界已有实体数 >= 投影要求数」过滤候选,
		// 并对已发出的项标绿、不可重复点。服务端**不做**按 key 的拦截 —— 否则会出现
		// 「客户端能点、服务端却跳过」的不一致。
		// （这里曾经维护过一张 key → 上次生成实体 ID 的表,但既然服务端不拦就没人读它,
		//  只会在长时间游戏里无限攒条目,已删除。）
		int count = Math.max(1, Math.min(EasyPlaceEntitySettings.MAX_COUNT, rawCount));
		NbtCompound nbt = spawn.nbt().copy();
		// 乘客(以及嵌套乘客)的 Pos 存的是**投影局部坐标**,必须改成载具当前的世界坐标,
		// 否则它们会被放到离载具几千格之外(表现就是"看不见乘客,打掉载具它才掉出来")。
		hao$fixPassengerPositions(nbt, spawn.x(), spawn.y(), spawn.z());

		// 补齐路径**只在真的缺乘客时**才走。
		//
		// 为什么要有这个前提:投影里同一处可能有**两个独立的同类实体**(比如两辆漏斗矿车)。
		// 放完第 1 个之后,世界里已经有一个同类型实体了 —— 如果无脑走补齐路径,第 2 个就会被
		// 误当成"已存在的那个",结果是"客户端显示已发出、世界里没有新的"。
		//
		// 所以判据是:投影这个实体**带乘客**,而世界里那个同类型载具的乘客**还不够**。
		if (hao$wantedPassengerCount(nbt) > 0) {
			java.util.List<Entity> existingVehicles = hao$findExistingVehicles(world, spawn, type, nbt);
			if (!existingVehicles.isEmpty()) {
				HaoDebug.log("[hao-entity]   该位置已有载具 " + existingVehicles.size()
						+ " 辆缺乘客,走补齐路径(本次最多补 " + count + " 位)");
				hao$fillMissingPassengers(player, world, existingVehicles, spawn, count);
				return;
			}
			// 载具有了、乘客也齐了 —— 再放就会多出一份载具,直接不做
			if (hao$countVehiclesNear(world, spawn, type) > 0) {
				HaoDebug.log("[hao-entity]   该位置的载具乘客已齐,不再重建");
				return;
			}
		}

		Map<Item, Integer> need = new LinkedHashMap<>();
		// ① 实体本身:必需 —— 连它都付不起就整条跳过(剩下的都没意义)。
		Map<Item, Integer> rootCost = COST.get(type);
		if (rootCost != null) {
			rootCost.forEach((item, amount) -> need.merge(item, amount * count, Integer::sum));
		}
		// 实体本身的材料**不再拦**:以客户端的判定为准 —— 客户端只会把缺料标红(仍然允许点),
		// 所以这里照常生成,只是"有多少扣多少"(hao$consume 内部按实际数量取,不会扣成负数)。
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
				HaoDebug.log("[hao-entity]   还原失败 " + spawn.type() + "(NBT 无法创建实体)");
				break;
			}
			entity.refreshPositionAndAngles(spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
			boolean added = world.spawnNewEntityAndPassengers(entity);
			HaoDebug.log("[hao-entity]   生成后 " + entity.getType() + " @ "
					+ String.format("%.3f,%.3f,%.3f", entity.getX(), entity.getY(), entity.getZ())
					+ " 投影要求=" + String.format("%.3f,%.3f,%.3f", spawn.x(), spawn.y(), spawn.z())
					+ " yaw=" + String.format("%.1f", spawn.yaw()));
			// 乘客保留**骑乘关系**(与投影完全一致:雪傀儡就是坐在竹筏上的乘客,有坐姿、会动),
			// 但要把它"已经在骑乘位"这件事**同步给客户端** —— 否则客户端会一直把它画在载具内部,
			// 表现就是"只看得见载具,打掉载具乘客才出现"。
			for (Entity passenger : new java.util.ArrayList<>(entity.getPassengerList())) {
				// 关键:乘客也必须**真正加入世界**(从而进入实体追踪、发出 spawn 包),
				// 否则客户端只会按载具的 Passengers "知道"它存在,却**不会渲染它** ——
				// 表现就是"只看得见载具,打掉载具乘客才出现"。骑乘关系保持不变(投影一致)。
				boolean addedPassenger = world.spawnNewEntityAndPassengers(passenger);
				HaoDebug.log("[hao-entity]   乘客 " + passenger.getType() + " 加入世界="
						+ addedPassenger + " 位置=" + passenger.getX() + "," + passenger.getY()
						+ "," + passenger.getZ() + " 载具=" + entity.getX() + "," + entity.getY()
						+ "," + entity.getZ());
			}
			HaoDebug.log("[hao-entity]   加进世界=" + added + " 乘客实况="
					+ entity.getPassengerList().size());
		}
		HaoDebug.log("[hao-entity]   生成 " + spawn.type() + " x" + count + " 消耗=" + need);
	}

	/** 投影里这个实体要求几位乘客(递归数 NBT 的 {@code Passengers})。 */
	private static int hao$wantedPassengerCount(NbtCompound nbt) {
		NbtList passengers = nbt.getList("Passengers").orElse(null);
		if (passengers == null) {
			return 0;
		}
		int count = 0;
		for (int i = 0; i < passengers.size(); i++) {
			if (passengers.get(i) instanceof NbtCompound passenger) {
				count += 1 + hao$wantedPassengerCount(passenger);
			}
		}
		return count;
	}

	/**
	 * 世界里这个载具**还缺几位乘客**(投影要求的减去它身上现有同类型乘客)。
	 * <p>返回 0 表示乘客已经齐了 —— 这时不该走补齐路径。</p>
	 */
	private static int hao$missingPassengerCount(Entity vehicle, NbtCompound wantedNbt) {
		NbtList wanted = wantedNbt.getList("Passengers").orElse(null);
		if (wanted == null) {
			return 0;
		}
		// 投影要求:按类型记账
		Map<EntityType<?>, Integer> remaining = new LinkedHashMap<>();
		for (int i = 0; i < wanted.size(); i++) {
			if (wanted.get(i) instanceof NbtCompound passengerNbt) {
				EntityType<?> passengerType = hao$typeOf(passengerNbt);
				if (passengerType != null) {
					remaining.merge(passengerType, 1, Integer::sum);
				}
			}
		}
		// 现有:按类型扣掉
		for (Entity passenger : vehicle.getPassengerList()) {
			remaining.computeIfPresent(passenger.getType(), (ignored, amount) -> amount - 1);
		}
		int missing = 0;
		for (Integer left : remaining.values()) {
			if (left != null && left > 0) {
				missing += left;
			}
		}
		return missing;
	}

	/**
	 * 在这一处找一个**还缺乘客**的同类载具(没有就返回 null)。
	 *
	 * <p>判定:类型 + 位置 + NBT 特征(投影里给这个实体写了自定义名字,那世界里那个也必须同名;
	 * 投影里没写名字就只看类型和位置)。</p>
	 *
	 * <p><b>为什么接受范围放到 2 格:</b>载具会浮起来或落下去 —— 竹筏浮在水面上、
	 * 矿车落在铁轨上,实际位置和投影坐标常常差一格多。只认 1 格会找不到,
	 * 结果就是"又放了一个新的载具"。</p>
	 *
	 * <p><b>为什么优先选缺乘客的那个:</b>同一处可能已经有两个同类载具,
	 * 其中一个坐满了乘客、另一个还空着。该补的是空着的那个。</p>
	 */
	/**
	 * 这一处一共有几个同类型载具(不管乘客齐不齐)。
	 * <p>用来区分两种情况:「这里根本没载具」和「载具有了、乘客也齐了」——
	 * 前者该整组新建,后者再新建就会多出一份载具。</p>
	 */
	private static int hao$countVehiclesNear(ServerWorld world, EntitySpawn spawn, EntityType<?> type) {
		Box target = type.getSpawnBox(spawn.x(), spawn.y(), spawn.z());
		Box accepted = target.expand(2.0D);
		int count = 0;
		for (Entity candidate : world.getOtherEntities(null, target.expand(4.0D))) {
			if (candidate.getType() == type && candidate.isAlive()
					&& candidate.getBoundingBox().intersects(accepted)) {
				count++;
			}
		}
		return count;
	}

	private static java.util.List<Entity> hao$findExistingVehicles(ServerWorld world, EntitySpawn spawn,
			EntityType<?> type, NbtCompound wantedNbt) {
		Box target = type.getSpawnBox(spawn.x(), spawn.y(), spawn.z());
		Box accepted = target.expand(2.0D);
		String wantedName = spawn.nbt().getString("CustomName").orElse("");
		java.util.List<Entity> found = new java.util.ArrayList<>();
		int candidates = 0;
		for (Entity candidate : world.getOtherEntities(null, target.expand(4.0D))) {
			if (candidate.getType() != type || !candidate.isAlive()) {
				continue;
			}
			if (!candidate.getBoundingBox().intersects(accepted)) {
				continue;
			}
			String name = candidate.getCustomName() == null ? "" : candidate.getCustomName().getString();
			// 投影里指定了名字 → 必须同名;没指定 → 只要类型和位置对就算
			if (!wantedName.isEmpty() && !wantedName.equals(name)) {
				continue;
			}
			candidates++;
			// 这一位乘客已经齐了 → 不是它
			if (hao$missingPassengerCount(candidate, wantedNbt) <= 0) {
				continue;
			}
			found.add(candidate);
		}
		// 近的优先补:玩家通常就在最近的那一辆旁边
		found.sort(java.util.Comparator.comparingDouble(
				entity -> entity.squaredDistanceTo(spawn.x(), spawn.y(), spawn.z())));
		HaoDebug.log("[hao-entity]   找已有载具 " + type + " 位置候选=" + candidates
				+ " 缺乘客的=" + found.size());
		return found;
	}

	/**
	 * 把**缺失的乘客**补到已有载具上(不重建载具)。
	 *
	 * <p>做法:把这一处**所有**还缺乘客的同类载具找出来(近的优先),在上面一共补
	 * {@code count} 位 —— {@code count} 就是「一次放几个」,设 5 就补 5 条船。
	 * 每位按 1 份材料算,材料不够补几位算几位。</p>
	 */
	private static void hao$fillMissingPassengers(ServerPlayerEntity player, ServerWorld world,
			java.util.List<Entity> vehicles, EntitySpawn spawn, int count) {
		NbtList wanted = spawn.nbt().getList("Passengers").orElse(null);
		if (wanted == null || wanted.isEmpty()) {
			HaoDebug.log("[hao-entity]   已有载具且投影本来就没有乘客,无需补齐");
			return;
		}
		Map<Item, Integer> need = new LinkedHashMap<>();
		int filled = 0;
		// 还要补几位:一次放几个 = 最多补几位
		int budget = count;
		for (Entity vehicle : vehicles) {
			if (budget <= 0) {
				break;
			}
			// 这一辆上还缺哪几位乘客
			Map<EntityType<?>, Integer> remaining = new LinkedHashMap<>();
			for (int i = 0; i < wanted.size(); i++) {
				if (wanted.get(i) instanceof NbtCompound passengerNbt) {
					EntityType<?> passengerType = hao$typeOf(passengerNbt);
					if (passengerType != null) {
						remaining.merge(passengerType, 1, Integer::sum);
					}
				}
			}
			for (Entity passenger : vehicle.getPassengerList()) {
				remaining.computeIfPresent(passenger.getType(), (ignored, amount) -> amount - 1);
			}
			for (int i = 0; i < wanted.size() && budget > 0; i++) {
				if (!(wanted.get(i) instanceof NbtCompound passengerNbt)) {
					continue;
				}
				EntityType<?> passengerType = hao$typeOf(passengerNbt);
				if (passengerType == null) {
					continue;
				}
				Integer left = remaining.get(passengerType);
				if (left == null || left <= 0) {
					continue; // 这一辆上这一位已经有了
				}
				// 补 1 位就按 1 份材料算,不乘「一次放几个」——
				// 倍率由外面的 budget 控制(补几位),不是每位要几份
				Map<Item, Integer> one = new LinkedHashMap<>();
				if (!hao$collectEntities(passengerType, passengerNbt, 1, player, one)) {
					HaoDebug.log("[hao-entity]   补齐阶段乘客材料不足,跳过 " + passengerType);
					budget = 0; // 材料没了:外层也别再换下一辆重试(否则日志会刷屏 N 条)
					break;
				}
				// 还要和**本次已经决定要补的**合并后再校验:背包只有 1 份材料时不能补 2 位
				boolean affordable = true;
				for (Map.Entry<Item, Integer> entry : one.entrySet()) {
					int total = need.getOrDefault(entry.getKey(), 0) + entry.getValue();
					if (hao$count(player, entry.getKey()) < total) {
						affordable = false;
						break;
					}
				}
				if (!affordable) {
					HaoDebug.log("[hao-entity]   补齐阶段材料不够再多补一位,跳过 " + passengerType);
					budget = 0;
					break;
				}

				NbtCompound passengerCopy = passengerNbt.copy();
				hao$fixPassengerPositions(passengerCopy, spawn.x(), spawn.y(), spawn.z());
				Entity created = EntityType.loadEntityWithPassengers(passengerCopy, world,
						SpawnReason.COMMAND, loaded -> loaded);
				if (created == null) {
					// 材料还没记账,这里直接跳过,不会白扣
					continue;
				}
				created.refreshPositionAndAngles(vehicle.getX(), vehicle.getY(), vehicle.getZ(),
						created.getYaw(), created.getPitch());
				world.spawnNewEntityAndPassengers(created);
				// force=true:座位被占也换上去,保证一定补在原来那个载具上
				if (!created.startRiding(vehicle, true)) {
					// 没坐上去就不算补齐:不计 filled/budget,也不扣材料
					HaoDebug.log("[hao-entity]   补齐的乘客没能骑上去,视为失败 " + passengerType);
					continue;
				}
				// 真的补上了才记账 + 扣料
				one.forEach((item, amount) -> need.merge(item, amount, Integer::sum));
				remaining.merge(passengerType, -1, Integer::sum);
				filled++;
				budget--;
			}
		}
		for (Map.Entry<Item, Integer> entry : need.entrySet()) {
			hao$consume(player, entry.getKey(), entry.getValue());
		}
		HaoDebug.log("[hao-entity]   补齐乘客 " + filled + "/" + count + " 位 消耗=" + need
				+ " 涉及载具=" + vehicles.size());
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
					&& hao$collectEntities(passengerType, passenger, count, player, sub)
					&& hao$affordableWith(player, need, sub)) {
				hao$prunePassengers(player, passenger, count, sub);
				sub.forEach((item, amount) -> need.merge(item, amount, Integer::sum));
				kept.add(passenger);
			} else {
				HaoDebug.log("[hao-entity]   乘客材料不足,已剔除 "
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
			HaoDebug.log("[hao-entity]   内容物不足,已剔除 Item");
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
			// 还要和**本次已经决定消耗的**合并后再校验:背包只有 1 份料时不能同时留下两件同类内容物
			if (payable && hao$affordableWith(player, need, sub)) {
				sub.forEach((item, amount) -> need.merge(item, amount, Integer::sum));
			} else {
				nbt.remove(field);
				HaoDebug.log("[hao-entity]   内容物不足,已剔除 " + field);
			}
		}
	}

	/**
	 * 把「已经分配掉的量」也算进去再校验一次：背包能不能同时付得起 {@code allocated}
	 * 和 {@code extra}。
	 *
	 * <p>没有这一步会出现白拿：投影里两个乘客都要 1 个雕刻南瓜时，两遍试算看到的
	 * 都是「背包里有 1 个」而各自通过，最后 {@code need} 累加成 2、
	 * 扣料却只能扣到 0，于是白送一个。</p>
	 */
	private static boolean hao$affordableWith(ServerPlayerEntity player, Map<Item, Integer> allocated,
			Map<Item, Integer> extra) {
		for (Map.Entry<Item, Integer> entry : extra.entrySet()) {
			int already = allocated.getOrDefault(entry.getKey(), 0);
			if (hao$count(player, entry.getKey()) < already + entry.getValue()) {
				return false;
			}
		}
		return true;
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
		// 把 need 里**已经分配掉**的同类材料一起算进来 —— 否则两个内容物都要同一材料时，
		// 每一遍都只跟背包总量比，两次都通过，最后扣料扣不齐就白送。
		if (hao$count(player, item) < need.getOrDefault(item, 0) + amount) {
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

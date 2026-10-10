package carpet_hao_addition.mixin;

import carpet_hao_addition.EntityPlacementHandler;
import carpet_hao_addition.EntityPlacementPayload;
import carpet_hao_addition.EasyPlaceEntitySettings;
import carpet_hao_addition.EntitySpawn;
import carpet_hao_addition.client.EntityPlacementScreen;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.EntityInfo;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.LayerRange; // malilib 0.28.x:这个类还没挪进 util.position

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * easyPlaceEntity(客户端,26.3):照投影轻松放置时,把玩家**真正指着**的投影实体报给服务端生成。
 * <p>
 * 候选判定照 QuickCraft 的做法(而不是估算距离/夹角):
 * <ol>
 *   <li><b>视线射线 × 实体生成盒</b>:{@code start = 眼睛, end = 眼睛 + 视线 × 交互距离},
 *       与 {@code type.getSpawnAABB(x,y,z)} 求交 —— 用碰撞箱会漏掉大半个身位,用"命中方块附近"
 *       又会被相邻/上下叠着的实体骗到。</li>
 *   <li><b>渲染层范围</b>:{@code DataManager.getRenderLayerRange().isPositionWithinRange(...)},
 *       没渲染出来的实体不参与。</li>
 *   <li><b>位置变换</b>:投影的镜像/旋转再做一次,最后补「投影原点 + 子区域偏移」。</li>
 *   <li><b>已放数量</b>:按"位置 + 类型"计数,世界里已有数 ≥ 投影要求数才算这一处放满
 *       —— 叠在同一格的多个实体能一个个放,打掉后又能重新放。</li>
 * </ol>
 * <p>
 * 26.x 的差异:{@code EntityInfo} 是 record 且 {@code nbt()} 给的是 malilib 的 {@code CompoundData},
 * 必须过 {@link DataConverterNbt#toVanillaCompound} 才能拿到 vanilla NBT;
 * 实体盒用 {@code getSpawnAABB};射线求交用 {@code AABB.clip}。
 * <p>
 * 注入方法签名必须与目标一致:{@code doEasyPlaceAction(Minecraft)} 返回 {@code InteractionResult},
 * 所以用 {@code CallbackInfoReturnable}(写错编译期看不出来,运行时抛 InvalidInjectionException 崩游戏)。
 */
@Mixin(WorldUtils.class)
public abstract class WorldUtils_entityPlacementMixin {
	/** 上次发包所在的客户端 tick,用于每 tick 限一次。 */
	@Unique
	private static long hao$lastEntityTick = Long.MIN_VALUE;

	/**
	 * 最近请求过的目标格 → 请求时的 tick。
	 * <p>
	 * 按住轻松放置键时 {@code doEasyPlaceAction} 每个 tick 都会被调用,只挡"同一 tick 重复"不够 ——
	 * 跨 tick 会反复发包、反复生成。这里与 easyPlaceWaterlogged 一样给个 TTL,短时间内同一格只放一次。
	 */
	@Unique
	private static final Map<Long, Long> hao$recentEntityRequests = new HashMap<>();

	/** 同一格在此期间内只生成一次(2 秒,与 easyPlaceWaterlogged 的请求 TTL 一致)。 */
	@Unique
	private static final long HAO_ENTITY_REQUEST_TTL = 40L;

	@Inject(method = "doEasyPlaceAction", at = @At("HEAD"), cancellable = true)
	private static void hao$spawnEntityOnEasyPlace(Minecraft minecraft, CallbackInfoReturnable<InteractionResult> cir) {
		// 注入点在 HEAD:这里一旦抛异常,整个轻松放置(包括方块)都会失效。所以兜住一切意外。
		try {
			hao$handleEasyPlaceEntity(minecraft, cir);
		} catch (Throwable throwable) {
			System.out.println("[hao-entity] 出错(已忽略,不影响方块放置): " + throwable);
		}
	}

	@Unique
	private static void hao$handleEasyPlaceEntity(Minecraft minecraft, CallbackInfoReturnable<InteractionResult> cir) {
		LocalPlayer player = minecraft.player;
		if (player == null || !EasyPlaceEntitySettings.isEnabled()
				|| !(minecraft.hitResult instanceof BlockHitResult hit)) {
			return;
		}
		long tick = player.level().getGameTime();
		if (hao$lastEntityTick == tick) {
			return;
		}
		List<EntitySpawn> candidates = hao$collectCandidates(minecraft);
		if (candidates.isEmpty()) {
			return;
		}
		hao$lastEntityTick = tick;
		if (candidates.size() > 1 || EasyPlaceEntitySettings.uiEnabled(player.getUUID())) {
			// 多个候选时必须让玩家点(代码猜不出来"你指着谁");只命中一个时是否还弹界面,
			// 由 /easyPlaceEntityUi 决定 —— 开着就总是弹界面,关掉就直接放置(更快)。
			minecraft.setScreenAndShow(new EntityPlacementScreen(candidates));
			cir.setReturnValue(InteractionResult.SUCCESS);
			return;
		}
		// 单候选:同一格 2 秒内只放一次,否则按住右键会刷出一堆
		BlockPos target = hit.getBlockPos();
		long key = target.asLong();
		Long last = hao$recentEntityRequests.get(key);
		if (last != null && tick - last <= HAO_ENTITY_REQUEST_TTL) {
			return;
		}
		hao$recentEntityRequests.entrySet().removeIf(entry -> tick - entry.getValue() > HAO_ENTITY_REQUEST_TTL);
		hao$recentEntityRequests.put(key, tick);
		hao$dispatchPacket(candidates.getFirst(), player);
	}

	/** 视线射线命中的、还没放过的投影实体,由近到远。 */
	@Unique
	private static List<EntitySpawn> hao$collectCandidates(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			return List.of();
		}
		// 判定"指着哪个":视线射线。起点是眼睛,终点是眼睛 + 视线方向 × 交互距离。
		Vec3 start = player.getEyePosition(1.0F);
		double range = WorldUtils.getValidBlockRange(mc);
		Vec3 end = start.add(player.getViewVector(1.0F).scale(range));
		List<EntitySpawn> found = new ArrayList<>();
		for (SchematicPlacement placement
				: DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
			if (placement == null || !placement.isRenderingEnabled() || placement.ignoreEntities()) {
				continue;
			}
			hao$collectFrom(mc, placement, start, end, found);
		}
		found.sort(Comparator.comparingDouble(spawn ->
				new Vec3(spawn.x(), spawn.y(), spawn.z()).distanceToSqr(start)));
		// 按"位置 + 类型"计数过滤:同一处投影里有 N 个实体,世界里已有 M 个同类型实体,
		// 只有 M >= N 才算"这一处已经放满"。
		// 这样:① 叠在同一位置的多个实体能一个个放;② 把实体打掉之后 M 归零,候选会自动重新出现。
		Map<String, Integer> projected = new HashMap<>();
		for (EntitySpawn spawn : found) {
			projected.merge(hao$positionKey(spawn), 1, Integer::sum);
		}
		found.removeIf(spawn ->
				hao$countNearby(mc, spawn) >= projected.getOrDefault(hao$positionKey(spawn), 1));
		if (!found.isEmpty()) {
			System.out.println("[hao-entity] 候选 " + found.size() + " 个:" + found.stream()
					.map(spawn -> spawn.type() + "@" + String.format("%.1f,%.1f,%.1f",
							spawn.x(), spawn.y(), spawn.z()))
					.toList());
		}
		return found;
	}

	/** 从单个投影里收集候选。 */
	@Unique
	private static void hao$collectFrom(Minecraft mc, SchematicPlacement placement,
			Vec3 rayStart, Vec3 rayEnd, List<EntitySpawn> out) {
		LitematicaSchematic schematic = placement.getSchematic();
		if (schematic == null) {
			return;
		}
		BlockPos origin = placement.getOrigin();
		// 优先用"启用中的子区域";某些投影/操作流程下这个集合是空的,那就退回遍历投影里的全部区域。
		Set<String> regionNames = new LinkedHashSet<>(
				placement.getEnabledRelativeSubRegionPlacements().keySet());
		if (regionNames.isEmpty()) {
			regionNames.addAll(schematic.getAreaPositions().keySet());
		}
		for (String regionName : regionNames) {
			SubRegionPlacement sub = placement.getRelativeSubRegionPlacement(regionName);
			// 子区域自己也可以勾"忽略实体",和投影级是两回事
			if (sub != null && sub.ignoreEntities()) {
				continue;
			}
			List<EntityInfo> entities = schematic.getEntityListForRegion(regionName);
			if (entities == null || entities.isEmpty()) {
				continue;
			}
			for (int index = 0; index < entities.size(); index++) {
				EntitySpawn spawn = hao$candidateFor(mc, placement, sub, origin, regionName, index,
						entities.get(index), rayStart, rayEnd);
				if (spawn != null) {
					out.add(spawn);
				}
			}
		}
	}

	/** 把一个投影实体变成候选;不满足条件(没渲染/没命中/已放过)则返回 null。 */
	@Unique
	private static EntitySpawn hao$candidateFor(Minecraft mc, SchematicPlacement placement,
			SubRegionPlacement sub, BlockPos origin, String regionName, int index, EntityInfo info,
			Vec3 rayStart, Vec3 rayEnd) {
		// 投影实体的身份 —— 去重只能靠它,不能靠坐标(投影里可能有几个实体坐标完全重合)
		String key = regionName + "#" + index;
		// malilib 的 CompoundData 必须先转成 vanilla NBT
		CompoundTag nbt = DataConverterNbt.toVanillaCompound(info.nbt());
		String raw = nbt.getString("id").orElse("");
		Identifier identifier = raw.isEmpty() ? null : Identifier.tryParse(raw);
		if (identifier == null) {
			return null;
		}
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
		if (type == null) {
			return null;
		}
		// 位置:投影的镜像/旋转 → 投影原点 + **子区域偏移**。
		// 不要再把子区域的 mirror/rotation 叠一次:实体局部坐标虽是相对所属子区域的,
		// 但子区域自身的变换已经体现在它的位置偏移里。
		Vec3 pos = PositionUtils.getTransformedPosition(info.posVec(),
				placement.getMirror(), placement.getRotation());
		BlockPos regionPos = sub == null ? BlockPos.ZERO : sub.getPos();
		double x = origin.getX() + regionPos.getX() + pos.x;
		double y = origin.getY() + regionPos.getY() + pos.y;
		double z = origin.getZ() + regionPos.getZ() + pos.z;
		// "我渲染出来的才放":渲染层范围比投影级渲染开关细一档,没渲染的层/位置不参与。
		// 拿不到范围对象时不过滤(宁可多给候选,也别把能放的挡掉)。
		LayerRange renderLayerRange = DataManager.getRenderLayerRange();
		if (renderLayerRange != null
				&& !renderLayerRange.isPositionWithinRange((int) x, (int) y, (int) z)) {
			return null;
		}
		// 视线判定:**生成盒**求交,或者**实体所在的那一整格**求交。
		// 只判定生成盒太严 —— 矿车的盒子不到 1 格,站远一点、或从侧面/上方看就会擦过去。
		boolean hitBox = type.getSpawnAABB(x, y, z).clip(rayStart, rayEnd).isPresent();
		boolean hitCell = new AABB(BlockPos.containing(x, y, z)).clip(rayStart, rayEnd).isPresent();
		if (!hitBox && !hitCell) {
			return null;
		}
		// 朝向:按投影的镜像/旋转变换(与位置同一套变换),不叠子区域那一层
		float yaw = hao$rotateYaw(hao$rotation(nbt, 0),
				placement.getMirror(), placement.getRotation());
		float pitch = hao$rotation(nbt, 1);
		// 把投影实体的 NBT 一起带上(Pos/Rotation 改写成世界坐标与新朝向):
		// 这样服务端能**连同乘客一起**还原 —— "矿车镶在船里"就是船带矿车乘客。
		// 去掉投影里的实体 UUID(含乘客):它常与世界里已有实体撞车,导致实体加不进世界、凭空消失。
		hao$stripUuids(nbt);
		nbt.put("Pos", hao$nbtDoubles(x, y, z));
		nbt.put("Rotation", hao$nbtFloats(yaw, pitch));
		return new EntitySpawn(identifier, key, x, y, z, yaw, pitch, nbt);
	}

	@Unique
	private static void hao$dispatchPacket(EntitySpawn spawn, LocalPlayer player) {
		int count = EasyPlaceEntitySettings.count(player.getUUID());
		EntityPlacementHandler.registerPayloadType();
		ClientPlayNetworking.send(new EntityPlacementPayload(List.of(spawn), count));
		System.out.println("[hao-entity] 放置 " + spawn.type() + " @ "
				+ String.format("%.1f,%.1f,%.1f", spawn.x(), spawn.y(), spawn.z()) + " x" + count);
	}

	/** "位置 + 类型"的粗粒度键:同一处的多个同类实体算同一组。 */
	@Unique
	private static String hao$positionKey(EntitySpawn spawn) {
		return spawn.type() + "@" + (int) Math.floor(spawn.x()) + "," + (int) Math.floor(spawn.y())
				+ "," + (int) Math.floor(spawn.z());
	}

	/** 世界里"就在这个点上"已经有多少个同类型实体(用于判断这一处是否已经放满)。 */
	@Unique
	private static int hao$countNearby(Minecraft mc, EntitySpawn spawn) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(spawn.type());
		if (mc.level == null || type == null) {
			return 0;
		}
		int count = 0;
		AABB searchBox = type.getSpawnAABB(spawn.x(), spawn.y(), spawn.z()).inflate(1.0D);
		for (Entity existing : mc.level.getEntities((Entity) null, searchBox, entity -> entity.getType() == type)) {
			// 只数"几乎落在同一个点"的:并排/上下紧挨着的同类实体不算同一处
			if (existing.position().distanceToSqr(spawn.x(), spawn.y(), spawn.z()) <= 0.36D) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 把投影里记录的 yaw 按投影的镜像/旋转一起变换,得到世界里的朝向。
	 * <p>
	 * 做法是把 yaw 变成单位方向向量,用与位置**同一个** {@link PositionUtils#getTransformedPosition}
	 * 做变换,再用两次变换的差值消掉可能存在的平移,最后算回 yaw —— 这样不必自己推导
	 * "旋转/镜像 与 角度" 的对应关系。pitch(俯仰)不受水平旋转影响,保持原值。
	 */
	@Unique
	private static float hao$rotateYaw(float yaw, Mirror mirror, Rotation rotation) {
		Vec3 zero = PositionUtils.getTransformedPosition(Vec3.ZERO, mirror, rotation);
		Vec3 moved = PositionUtils.getTransformedPosition(Vec3.directionFromRotation(0.0F, yaw), mirror, rotation);
		Vec3 dir = moved.subtract(zero);
		if (dir.lengthSqr() < 1.0E-6D) {
			return yaw; // 退化情形:保持原朝向
		}
		return (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
	}

	/** 读投影实体 NBT 里 {@code Rotation[index]}(缺省 0)。 */
	@Unique
	private static float hao$rotation(CompoundTag nbt, int index) {
		ListTag rotation = nbt.getList("Rotation").orElse(null);
		return rotation != null && rotation.size() > index ? rotation.getFloat(index).orElse(0.0F) : 0.0F;
	}

	/** 递归去掉 NBT(含乘客)里的 UUID,避免与世界已有实体冲突。 */
	@Unique
	private static void hao$stripUuids(CompoundTag nbt) {
		nbt.remove("UUID");
		ListTag passengers = nbt.getList("Passengers").orElse(null);
		if (passengers == null) {
			return;
		}
		for (int i = 0; i < passengers.size(); i++) {
			if (passengers.get(i) instanceof CompoundTag passenger) {
				hao$stripUuids(passenger);
			}
		}
	}

	/** 构造 NBT 的双精度列表(用于 Pos)。 */
	@Unique
	private static ListTag hao$nbtDoubles(double... values) {
		ListTag list = new ListTag();
		for (double value : values) {
			list.add(DoubleTag.valueOf(value));
		}
		return list;
	}

	/** 构造 NBT 的浮点列表(用于 Rotation)。 */
	@Unique
	private static ListTag hao$nbtFloats(float... values) {
		ListTag list = new ListTag();
		for (float value : values) {
			list.add(FloatTag.valueOf(value));
		}
		return list;
	}
}

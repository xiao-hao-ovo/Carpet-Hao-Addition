package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceEntityHandler;
import carpet_hao_addition.EasyPlaceEntityPayload;
import carpet_hao_addition.EasyPlaceEntitySettings;
import carpet_hao_addition.EntitySpawn;
import carpet_hao_addition.client.EasyPlaceEntitySelectorScreen;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.EntityInfo;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.WorldUtils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * easyPlaceEntity(客户端):照投影轻松放置时,把玩家**真正指着**的投影实体报给服务端生成。
 * <p>
 * 候选判定照 QuickCraft 的做法(而不是估算距离/夹角):
 * <ol>
 *   <li><b>视线射线 × 实体生成盒</b>:{@code start = 眼睛, end = 眼睛 + 视线 × 交互距离},
 *       与 {@code type.getSpawnBox(x,y,z)} 求交 —— 用碰撞箱会漏掉大半个身位,用"命中方块附近"
 *       又会被相邻/上下叠着的实体骗到。</li>
 *   <li><b>渲染层范围</b>:{@code DataManager.getRenderLayerRange().isPositionWithinRange(...)},
 *       没渲染出来的实体不参与 —— 这才是"我渲染出来的才放"的正解(投影级开关粒度太粗)。</li>
 *   <li><b>位置变换</b>:投影的镜像/旋转 **再叠加子区域自己的镜像/旋转**,最后补
 *       「投影原点 + 子区域偏移(同样做投影变换)」,旋转过的投影里才不会偏。</li>
 *   <li><b>材料预检</b>:背包里没有对应物品就不产生候选(避免"点了之后被服务端默默拒绝")。</li>
 * </ol>
 * <p>
 * 命中多个时**弹出选择器让玩家点**(上下叠着的盔甲架/箱船就是这样),
 * 只有一个时直接放置,保留原来的右键手感。
 * <p>
 * 注入方法的签名必须和目标方法一致:{@code doEasyPlaceAction} 的第一个参数是 {@link MinecraftClient},
 * 且它有返回值 {@link ActionResult},所以要用 {@code CallbackInfoReturnable} 而不是 {@code CallbackInfo}
 * (写错的话编译期看不出来,只在运行时抛 InvalidInjectionException 把游戏崩掉)。
 */
@Mixin(WorldUtils.class)
public abstract class WorldUtils_easyPlaceEntityMixin {
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
	private static final Map<Long, Long> hao$recentEntityRequests = new java.util.HashMap<>();

	/** 同一格在此期间内只生成一次(2 秒,与 easyPlaceWaterlogged 的请求 TTL 一致)。 */
	@Unique
	private static final long HAO_ENTITY_REQUEST_TTL = 40L;

	@Inject(method = "doEasyPlaceAction", at = @At("HEAD"), cancellable = true)
	private static void hao$spawnEntityOnEasyPlace(MinecraftClient minecraft, CallbackInfoReturnable<ActionResult> cir) {
		// 注入点在 HEAD:这里一旦抛异常,整个轻松放置(包括方块)都会失效。所以兜住一切意外。
		try {
			hao$handleEasyPlaceEntity(minecraft, cir);
		} catch (Throwable throwable) {
			System.out.println("[hao-entity] 出错(已忽略,不影响方块放置): " + throwable);
			throwable.printStackTrace();
		}
	}

	@Unique
	private static void hao$handleEasyPlaceEntity(MinecraftClient minecraft, CallbackInfoReturnable<ActionResult> cir) {
		PlayerEntity player = minecraft.player;
		if (player == null || !EasyPlaceEntitySettings.isEnabled()
				|| !(minecraft.crosshairTarget instanceof BlockHitResult hit)) {
			return;
		}
		long tick = player.getEntityWorld().getTime();
		if (hao$lastEntityTick == tick) {
			return;
		}
		List<EntitySpawn> candidates = hao$collectCandidates(minecraft);
		if (candidates.isEmpty()) {
			return;
		}
		hao$lastEntityTick = tick;
		if (candidates.size() > 1 || EasyPlaceEntitySettings.uiEnabled(player.getUuid())) {
			// 多个候选时必须让玩家点(代码猜不出来"你指着谁");只命中一个时是否还弹界面,
			// 由 /easyPlaceEntityUi 决定 —— 开着就总是弹界面,关掉就直接放置(更快)。
			minecraft.setScreen(new EasyPlaceEntitySelectorScreen(candidates));
			cir.setReturnValue(ActionResult.SUCCESS);
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
		hao$send(candidates.getFirst(), player);
	}

	/** 视线射线命中的、还没放过的投影实体,由近到远。 */
	@Unique
	private static List<EntitySpawn> hao$collectCandidates(MinecraftClient mc) {
		PlayerEntity player = mc.player;
		if (player == null || mc.world == null) {
			return List.of();
		}
		// 判定"指着哪个":视线射线。起点是眼睛,终点是眼睛 + 视线方向 × 交互距离。
		Vec3d start = player.getCameraPosVec(1.0F);
		Vec3d end = start.add(player.getRotationVec(1.0F).multiply(WorldUtils.getValidBlockRange(mc)));
		List<EntitySpawn> found = new ArrayList<>();
		for (SchematicPlacement placement
				: DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
			if (placement == null || !placement.isRenderingEnabled() || placement.ignoreEntities()) {
				continue;
			}
			hao$collectFrom(mc, placement, start, end, found);
		}
		found.sort(Comparator.comparingDouble(spawn ->
				new Vec3d(spawn.x(), spawn.y(), spawn.z()).squaredDistanceTo(start)));
		// 按"位置 + 类型"计数过滤:同一处投影里有 N 个实体,世界里已有 M 个同类型实体,
		// 只有 M >= N 才算"这一处已经放满"。
		// 这样:① 叠在同一位置的多个实体能一个个放(而不是永远只认一个);
		//      ② 把实体打掉之后 M 归零,候选会自动重新出现,可以重新放。
		if (mc.world != null) {
			Map<String, Integer> projected = new java.util.HashMap<>();
			for (EntitySpawn spawn : found) {
				projected.merge(hao$positionKey(spawn), 1, Integer::sum);
			}
			found.removeIf(spawn -> {
				int nearby = hao$countNearby(mc, spawn);
				int need = projected.getOrDefault(hao$positionKey(spawn), 1);
				boolean drop = nearby >= need;
				System.out.println("[hao-entity]   过滤 " + hao$positionKey(spawn)
						+ " 世界已有=" + nearby + " 投影要求=" + need + (drop ? " -> 移除" : " -> 保留"));
				return drop;
			});
		}
		System.out.println("[hao-entity] 候选 " + found.size() + " 个");
		for (EntitySpawn spawn : found) {
			System.out.println("[hao-entity]   " + spawn.type() + " @ "
					+ String.format("%.1f,%.1f,%.1f", spawn.x(), spawn.y(), spawn.z()));
		}
		return found;
	}

	/** 从单个投影里收集候选。 */
	@Unique
	private static void hao$collectFrom(MinecraftClient mc, SchematicPlacement placement,
			Vec3d rayStart, Vec3d rayEnd, List<EntitySpawn> out) {
		LitematicaSchematic schematic = placement.getSchematic();
		if (schematic == null) {
			return;
		}
		BlockPos origin = placement.getOrigin();
		// 优先用"启用中的子区域";但有些投影/操作流程下这个集合是空的,那就退回遍历投影里的
		// 全部区域 —— 否则一个实体都收不到,表现就是"完全没有反应"。
		java.util.Set<String> regionNames = new java.util.LinkedHashSet<>(
				placement.getEnabledRelativeSubRegionPlacements().keySet());
		if (regionNames.isEmpty()) {
			regionNames.addAll(schematic.getAreaPositions().keySet());
		}
		System.out.println("[hao-entity] 投影「" + placement.getName() + "」待查区域 " + regionNames);
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

	/** 把一个投影实体变成候选;不满足条件(没渲染/没命中/已放过/没材料)则返回 null。 */
	@Unique
	private static EntitySpawn hao$candidateFor(MinecraftClient mc, SchematicPlacement placement,
			SubRegionPlacement sub, BlockPos origin, String regionName, int index, EntityInfo info,
			Vec3d rayStart, Vec3d rayEnd) {
		// 投影实体的身份 —— 去重只能靠它,不能靠坐标(投影里可能有几个实体坐标完全重合)
		String key = regionName + "#" + index;
		System.out.println("[hao-entity]    区域 " + regionName + " 实体 "
				+ info.nbt.getString("id").orElse("?") + " 局部=" + info.posVec);
		System.out.println("[hao-entity]      NBT=" + info.nbt);
		String raw = info.nbt.getString("id").orElse("");
		Identifier identifier = raw.isEmpty() ? null : Identifier.tryParse(raw);
		if (identifier == null) {
			return null;
		}
		EntityType<?> type = Registries.ENTITY_TYPE.get(identifier);
		if (type == null) {
			return null;
		}
		// 位置:投影的镜像/旋转 → 投影原点 + **子区域偏移**。
		// 不要再把子区域的 mirror/rotation 叠一次:实体局部坐标虽是相对所属子区域的,
		// 但子区域自身的变换已经体现在它的位置偏移里,再叠一次会让实体和投影错开
		// (表现就是"投影里明明卡在那个方块里,放出来却不在那儿")。
		Vec3d pos = PositionUtils.getTransformedPosition(info.posVec,
				placement.getMirror(), placement.getRotation());
		BlockPos regionPos = sub == null ? BlockPos.ORIGIN : sub.getPos();
		double x = origin.getX() + regionPos.getX() + pos.x;
		double y = origin.getY() + regionPos.getY() + pos.y;
		double z = origin.getZ() + regionPos.getZ() + pos.z;
		System.out.println("[hao-entity]    坐标 局部=" + info.posVec
				+ " 区域=" + regionName + " 区域偏移=" + regionPos + " 投影原点=" + origin
				+ " 变换后=" + String.format("%.3f,%.3f,%.3f", pos.x, pos.y, pos.z)
				+ " 世界=" + String.format("%.3f,%.3f,%.3f", x, y, z));
		// 诊断:这个位置在**投影世界**和**真实世界**分别是什么方块。
		// 若投影那边是空气,说明我们算的位置根本不是投影里凋灵所在的那一格(位置算偏了)。
		var schematicWorld = fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld();
		if (schematicWorld != null && mc.world != null) {
			BlockPos at = BlockPos.ofFloored(x, y, z);
			System.out.println("[hao-entity]    位置方块 " + at + " 投影=" + schematicWorld.getBlockState(at)
					+ " 世界=" + mc.world.getBlockState(at));
		}
		// "我渲染出来的才放":渲染层范围比投影级渲染开关细一档,没渲染的层/位置不参与。
		// 拿不到范围对象时不过滤(宁可多给候选,也别把能放的挡掉)。
		var renderLayerRange = DataManager.getRenderLayerRange();
		if (renderLayerRange != null
				&& !renderLayerRange.isPositionWithinRange((int) x, (int) y, (int) z)) {
			System.out.println("[hao-entity]     × 在渲染层范围外 " + identifier + " @ "
					+ String.format("%.1f,%.1f,%.1f", x, y, z));
			return null;
		}
		// 视线射线与**生成盒**求交(不是碰撞箱:碰撞箱比实体小,俯视/侧看时容易漏)
		if (type.getSpawnBox(x, y, z).raycast(rayStart, rayEnd).isEmpty()) {
			System.out.println("[hao-entity]     × 视线射线没命中 " + identifier + " @ "
					+ String.format("%.1f,%.1f,%.1f", x, y, z));
			return null;
		}
		// 材料不足**不再**把候选藏起来 —— 藏起来玩家就不知道"还差什么",连材料清单都看不到。
		// 改为照常出候选、界面上把缺的材料标红,点击时给出提示(见 EasyPlaceEntitySelectorScreen)。
		// 朝向:按投影的镜像/旋转变换(与位置同一套变换),不叠子区域那一层
		float yaw = hao$rotateYaw(hao$rotation(info.nbt, 0),
				placement.getMirror(), placement.getRotation());
		float pitch = hao$rotation(info.nbt, 1);
		// 把投影实体的 NBT 一起带上(Pos/Rotation 改写成世界坐标与新朝向):
		// 这样服务端能**连同乘客一起**还原 —— "矿车镶在船里"就是船带矿车乘客。
		NbtCompound nbt = info.nbt.copy();
		// 去掉投影里的实体 UUID(含乘客):它常与世界里已有实体(之前粘贴出来的)撞车,
		// 导致实体加不进世界、凭空消失。
		hao$stripUuids(nbt);
		nbt.put("Pos", hao$nbtDoubles(x, y, z));
		nbt.put("Rotation", hao$nbtFloats(yaw, pitch));
		if (info.nbt.contains("Item")) {
			System.out.println("[hao-entity]   框内物品=" + info.nbt.get("Item") + " @ " + identifier);
		}
		return new EntitySpawn(identifier, key, x, y, z, yaw, pitch, nbt);
	}

	@Unique
	private static void hao$send(EntitySpawn spawn, PlayerEntity player) {
		int count = EasyPlaceEntitySettings.count(player.getUuid());
		EasyPlaceEntityHandler.registerPayloadType();
		EasyPlaceEntityHandler.registerPayloadType();
		ClientPlayNetworking.send(new EasyPlaceEntityPayload(List.of(spawn), count));
		System.out.println("[hao-entity] 放置 " + spawn.type() + " @ "
				+ String.format("%.1f,%.1f,%.1f", spawn.x(), spawn.y(), spawn.z()));
	}


	/** "位置 + 类型"的粗粒度键:同一处的多个同类实体算同一组。 */
	@Unique
	private static String hao$positionKey(EntitySpawn spawn) {
		return spawn.type() + "@" + (int) Math.floor(spawn.x()) + "," + (int) Math.floor(spawn.y())
				+ "," + (int) Math.floor(spawn.z());
	}

	/** 世界里"就在这个点上"已经有多少个同类型实体(用于判断这一处是否已经放满)。 */
	@Unique
	private static int hao$countNearby(MinecraftClient mc, EntitySpawn spawn) {
		ClientWorld world = mc.world;
		EntityType<?> type = Registries.ENTITY_TYPE.get(spawn.type());
		if (world == null || type == null) {
			return 0;
		}
		int count = 0;
		for (Entity existing : world.getOtherEntities(null,
				type.getSpawnBox(spawn.x(), spawn.y(), spawn.z()).expand(1.0D))) {
			// 只数"几乎落在同一个点"的:并排/上下紧挨着的同类实体不算同一处
			if (existing.getType() == type
					&& existing.getEntityPos().squaredDistanceTo(spawn.x(), spawn.y(), spawn.z()) <= 0.36D) {
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
	private static float hao$rotateYaw(float yaw, BlockMirror mirror, BlockRotation rotation) {
		Vec3d zero = PositionUtils.getTransformedPosition(Vec3d.ZERO, mirror, rotation);
		Vec3d moved = PositionUtils.getTransformedPosition(Vec3d.fromPolar(0.0F, yaw), mirror, rotation);
		Vec3d dir = moved.subtract(zero);
		if (dir.lengthSquared() < 1.0E-6D) {
			return yaw; // 退化情形:保持原朝向
		}
		return (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
	}

	/** 读投影实体 NBT 里 {@code Rotation[index]}(缺省 0)。 */
	@Unique
	private static float hao$rotation(NbtCompound nbt, int index) {
		NbtList rotation = nbt.getList("Rotation").orElseGet(NbtList::new);
		return rotation.size() > index ? rotation.getFloat(index).orElse(0.0F) : 0.0F;
	}

	/** 递归去掉 NBT(含乘客)里的 UUID,避免与世界已有实体冲突。 */
	@Unique
	private static void hao$stripUuids(NbtCompound nbt) {
		nbt.remove("UUID");
		nbt.getList("Passengers").ifPresent(list -> {
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof NbtCompound passenger) {
					hao$stripUuids(passenger);
				}
			}
		});
	}

	/** 构造 NBT 的双精度列表(用于 Pos)。 */
	@Unique
	private static NbtList hao$nbtDoubles(double... values) {
		NbtList list = new NbtList();
		for (double value : values) {
			list.add(NbtDouble.of(value));
		}
		return list;
	}

	/** 构造 NBT 的浮点列表(用于 Rotation)。 */
	@Unique
	private static NbtList hao$nbtFloats(float... values) {
		NbtList list = new NbtList();
		for (float value : values) {
			list.add(NbtFloat.of(value));
		}
		return list;
	}
}

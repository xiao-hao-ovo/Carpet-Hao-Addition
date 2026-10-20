package carpet_hao_addition.mixin;

import carpet_hao_addition.EntityPlacementCounter;
import carpet_hao_addition.EntityPlacementHandler;
import carpet_hao_addition.EntityPlacementPayload;
import carpet_hao_addition.EasyPlaceEntitySettings;
import carpet_hao_addition.EntityPlacementTrigger;
import carpet_hao_addition.EntitySpawn;
import carpet_hao_addition.client.EntityPlacementScreen;

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

import carpet_hao_addition.HaoDebug;
/**
 * haoProjectionEntity(客户端):照投影轻松放置时,把玩家**真正指着**的投影实体报给服务端生成。
 * <p>
 * 候选判定用视线射线求交(而不是估算距离/夹角):
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
public abstract class WorldUtils_entityPlacementMixin {
	/** 上次发包所在的客户端 tick,用于每 tick 限一次。 */
	@Unique
	private static long hao$lastEntityTick = Long.MIN_VALUE;

	/**
	 * 最近请求过的目标格 → 请求时的 tick。
	 * <p>
	 * 按住轻松放置键时 {@code doEasyPlaceAction} 每个 tick 都会被调用,只挡"同一 tick 重复"不够 ——
	 * 跨 tick 会反复发包、反复生成。这里与 haoProjectionWaterlogged 一样给个 TTL,短时间内同一格只放一次。
	 */
	@Unique
	private static final Map<Long, Long> hao$recentEntityRequests = new java.util.HashMap<>();

	/** 同一格在此期间内只生成一次(2 秒,与 haoProjectionWaterlogged 的请求 TTL 一致)。 */
	@Unique
	private static final long HAO_ENTITY_REQUEST_TTL = 40L;

	/**
	 * 每个候选的「投影里几个 / 世界里已有几个」,键是 {@code EntitySpawn#key()}。
	 * <p>
	 * 刻意**不**放进 {@link EntitySpawn}:那是个带 {@code PacketCodec} 的 record,加字段会改动
	 * 跨 mod 协议(HaoMaSa 与 26.x 都按同一个 codec 收发)。这两个数只给客户端的选择器界面用,
	 * 不需要发给服务端,所以单独放一份。
	 * <p>
	 * 用途有两个:① 界面能一直打开,玩家可以**继续加放**;② 界面上能看出**还缺几个**,
	 * 用来核对投影是否放全了。
	 */
	@Unique
	private static final java.util.Map<String, int[]> HAO_COUNTS = new java.util.HashMap<>();

	@Inject(method = "doEasyPlaceAction", at = @At("HEAD"), cancellable = true)
	private static void hao$spawnEntityOnEasyPlace(MinecraftClient minecraft, CallbackInfoReturnable<ActionResult> cir) {
		// 注入点在 HEAD:这里一旦抛异常,整个轻松放置(包括方块)都会失效。所以兜住一切意外。
		try {
			hao$handleEasyPlaceEntity(minecraft, cir);
		} catch (Throwable throwable) {
			HaoDebug.log("[hao-entity] 出错(已忽略,不影响方块放置): " + throwable);
			HaoDebug.stackTrace(throwable);
		}
	}

	@Unique
	private static void hao$handleEasyPlaceEntity(MinecraftClient minecraft, CallbackInfoReturnable<ActionResult> cir) {
		PlayerEntity player = minecraft.player;
		// 触发来源只对「这一次调用」有效：先取走，免得上一次快捷键影响后面的右键。
		boolean fromHotkey = EntityPlacementTrigger.consumeHotkey();
		if (!fromHotkey) {
			// 右键路径：配套客户端上一次按快捷键带来的「一次放置数量」也要清掉，
			// 否则它会影响这次右键用的数量。
			EntityPlacementTrigger.clearHotkeyCount();
		}
		if (player == null || !EasyPlaceEntitySettings.isTriggerEnabled(fromHotkey)) {
			// 诊断(临时):区分"客户端读不到规则值"与"准星没对着方块"。
			// 规则关着时每 tick 都会走到这里,所以先看开关再拼接。
			if (HaoDebug.ENABLED) {
				HaoDebug.log("[hao-entity] 未进入收集: player=" + (player != null)
						+ " 取值=" + EasyPlaceEntitySettings.value() + " 快捷键=" + fromHotkey
						+ " 准星是方块=" + (minecraft.crosshairTarget instanceof BlockHitResult));
			}
			// 快捷键是玩家主动按键,不给提示就完全看不出发生了什么(以前这里是静默返回,
			// 表现就是"快捷键时灵时不灵")。右键路径保持安静,否则照着投影施工会刷屏。
			if (fromHotkey && player != null) {
				hao$notify(player, EasyPlaceEntitySettings.value() == EasyPlaceEntitySettings.Trigger.FALSE
						? "carpet-hao-addition.entity.ruleOff"
						: "carpet-hao-addition.entity.noBlock");
			}
			return;
		}
		// ⚠️ 右键路径才要求"准星对着方块"(右键本来就是对屏幕中央那个方块操作);
		// 快捷键路径**不能**这样要求 —— 快捷键的判定在 hao$collectCandidates 里用视线射线自己做,
		// 而 crosshairTarget 会命中**实体**:第一次放出的那个实体往往正好挡在准星上,
		// 于是"在同一个地方再按一次"就会被误判成"准星没有指着方块"。
		if (!fromHotkey && !(minecraft.crosshairTarget instanceof BlockHitResult)) {
			HaoDebug.log("[hao-entity] 右键路径:准星没对着方块");
			return;
		}
		long tick = player.getWorld().getTime();
		// 同 tick 去抖只对右键长按有意义:快捷键是人工单击,再抖就会"按第二下没反应"。
		if (!fromHotkey && hao$lastEntityTick == tick) {
			return;
		}
		List<EntitySpawn> candidates = hao$collectCandidates(minecraft);
		if (candidates.isEmpty()) {
			HaoDebug.log("[hao-entity] 候选为空(视线内没有可放的投影实体)");
			if (fromHotkey) {
				hao$notify(player, "carpet-hao-addition.entity.noTarget");
			}
			return;
		}
		hao$lastEntityTick = tick;
		if (candidates.size() > 1 || EasyPlaceEntitySettings.uiEnabled(player.getUuid())) {
			// 多个候选时必须让玩家点(代码猜不出来"你指着谁");只命中一个时是否还弹界面,
			// 由 /easyPlaceEntityUi 决定 —— 开着就总是弹界面,关掉就直接放置(更快)。
			// 界面会一直弹(即使这一处已经放满),玩家据此核对缺口、或继续加放。
			minecraft.setScreen(new EntityPlacementScreen(candidates, HAO_COUNTS));
			cir.setReturnValue(ActionResult.SUCCESS);
			return;
		}
		// 单候选:同一格 2 秒内只放一次,否则按住右键会刷出一堆。快捷键不受此限。
		// TTL 的键统一用"视线终点所在格":快捷键路径没有可靠的 crosshairTarget
		// (可能被实体挡住),而这条射线两条路径都能拿到。
		Vec3d eye = player.getCameraPosVec(1.0F);
		Vec3d lookEnd = eye.add(player.getRotationVec(1.0F).multiply(WorldUtils.getValidBlockRange(minecraft)));
		BlockPos target = BlockPos.ofFloored(lookEnd);
		long key = target.asLong();
		Long last = hao$recentEntityRequests.get(key);
		if (!fromHotkey && last != null && tick - last <= HAO_ENTITY_REQUEST_TTL) {
			HaoDebug.log("[hao-entity] 同格 2 秒内已放过,跳过");
			return;
		}
		hao$recentEntityRequests.entrySet().removeIf(entry -> tick - entry.getValue() > HAO_ENTITY_REQUEST_TTL);
		hao$recentEntityRequests.put(key, tick);
		hao$dispatchPacket(candidates.getFirst(), player);
	}

	/** 给玩家一行动作栏提示(快捷键路径专用,右键路径保持安静)。 */
	@Unique
	private static void hao$notify(PlayerEntity player, String translationKey) {
		player.sendMessage(net.minecraft.text.Text.translatable(translationKey), true);
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
		double range = WorldUtils.getValidBlockRange(mc);
		Vec3d end = start.add(player.getRotationVec(1.0F).multiply(range));
		// 诊断(临时):玩家眼睛位置 + 视线可达距离。
		// 用来判断"没命中"到底是"离得太远"还是"litematica 给的有效距离是 0"。
		// 这里每 tick 都会走到,而 String.format 是所有字符串操作里最慢的一档 ——
		// 必须先看开关再拼,HaoDebug 的 ENABLED 是 static final,JIT 会把整块消掉。
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-entity] 玩家眼睛=" + String.format("%.2f,%.2f,%.2f", start.x, start.y, start.z)
					+ " 视线距离=" + String.format("%.2f", range));
		}
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
		// 按"位置 + 类型"计数:同一处投影里有 N 个实体,世界里已有 M 个。
		// ⚠️ 这里**不再**把"已放满"的候选从列表里去掉 —— 选择器界面要能随时打开,玩家才能
		// ①继续加放(比如投影 2 个只放了 1 个,或想多放几个)②核对投影到底缺不缺。
		// 数量另存一份带进界面(见 HAO_COUNTS),候选本身原样保留。
		HAO_COUNTS.clear();
		if (mc.world != null) {
			// 整处统计:同一位置有几个投影实体、世界里已有几个(与"一次放几个"无关)
			HAO_COUNTS.putAll(EntityPlacementCounter.totals(mc.world, found));
		}
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-entity] 候选 " + found.size() + " 个");
			for (EntitySpawn spawn : found) {
				int[] counts = HAO_COUNTS.get(spawn.key());
				HaoDebug.log("[hao-entity]   " + spawn.type() + " @ "
						+ String.format("%.1f,%.1f,%.1f", spawn.x(), spawn.y(), spawn.z())
						+ (counts == null ? "" : "  投影=" + counts[0] + " 已有=" + counts[1]));
			}
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
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-entity] 投影「" + placement.getName() + "」待查区域 " + regionNames);
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

	/** 把一个投影实体变成候选;不满足条件(没渲染/没命中/已放过/没材料)则返回 null。 */
	@Unique
	private static EntitySpawn hao$candidateFor(MinecraftClient mc, SchematicPlacement placement,
			SubRegionPlacement sub, BlockPos origin, String regionName, int index, EntityInfo info,
			Vec3d rayStart, Vec3d rayEnd) {
		// 投影实体的身份 —— 去重只能靠它,不能靠坐标(投影里可能有几个实体坐标完全重合)。
		// key 里必须带**这个投影自己的标识**:只用「子区域#序号」的话,同时显示两个投影、
		// 而它们恰好有同名子区域(单区域投影常常都叫 main)且实体序号相同时,两边的 key 会
		// 撞在一起 —— HAO_COUNTS 与 EntityPlacementCounter.totals 都拿 key 当分组键,
		// 后写的覆盖先写的,界面上的「需要/已放」就串到另一个投影上去了。
		// 用投影原点当标识:两个投影叠在同一位置时确实分不开,但那种情况玩家自己也分不清。
		// 注意 key 会进网络包(buf.writeString(spawn.key(), 256)),子区域名过长必须截断,
		// 否则序列化时直接抛异常。
		String regionKey = regionName.length() > 96 ? regionName.substring(0, 96) : regionName;
		String key = origin.asLong() + "/" + regionKey + "#" + index;
		// ⚠️ 下面这段诊断里 info.nbt 的 toString 会把整棵 NBT 树序列化一遍,
		// 而 hao$candidateFor 是"每个投影实体 × 每 tick"都走的路径 —— 必须靠 ENABLED 短路,
		// 否则玩家按住轻松放置键时这里会持续空转。
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-entity]    区域 " + regionName + " 实体 "
					+ info.nbt.getString("id").orElse("?") + " 局部=" + info.posVec);
			HaoDebug.log("[hao-entity]      NBT=" + info.nbt);
		}
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
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-entity]    坐标 局部=" + info.posVec
					+ " 区域=" + regionName + " 区域偏移=" + regionPos + " 投影原点=" + origin
					+ " 变换后=" + String.format("%.3f,%.3f,%.3f", pos.x, pos.y, pos.z)
					+ " 世界=" + String.format("%.3f,%.3f,%.3f", x, y, z));
		}
		// 诊断:这个位置在**投影世界**和**真实世界**分别是什么方块。
		// 若投影那边是空气,说明我们算的位置根本不是投影里凋灵所在的那一格(位置算偏了)。
		// getBlockState 是真实的世界查询,不在调试时一律不跑。
		if (HaoDebug.ENABLED) {
			var schematicWorld = fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld();
			if (schematicWorld != null && mc.world != null) {
				BlockPos at = BlockPos.ofFloored(x, y, z);
				HaoDebug.log("[hao-entity]    位置方块 " + at + " 投影=" + schematicWorld.getBlockState(at)
						+ " 世界=" + mc.world.getBlockState(at));
			}
		}
		// "我渲染出来的才放":渲染层范围比投影级渲染开关细一档,没渲染的层/位置不参与。
		// 拿不到范围对象时不过滤(宁可多给候选,也别把能放的挡掉)。
		var renderLayerRange = DataManager.getRenderLayerRange();
		if (renderLayerRange != null
				&& !renderLayerRange.isPositionWithinRange((int) x, (int) y, (int) z)) {
			if (HaoDebug.ENABLED) {
				HaoDebug.log("[hao-entity]     × 在渲染层范围外 " + identifier + " @ "
						+ String.format("%.1f,%.1f,%.1f", x, y, z));
			}
			return null;
		}
		// 视线判定:**生成盒**求交,或者**实体所在的那一整格**求交。
		// 只判定生成盒太严 —— 矿车的盒子不到 1 格,站远一点、或从侧面/上方看就会擦过去,
		// 表现就是"候选 0 个、点了没反应"。放宽成"准星指到实体所在格也算选中"。
		boolean hitBox = !type.getSpawnBox(x, y, z).raycast(rayStart, rayEnd).isEmpty();
		boolean hitCell = !new Box(BlockPos.ofFloored(x, y, z)).raycast(rayStart, rayEnd).isEmpty();
		if (!hitBox && !hitCell) {
			if (HaoDebug.ENABLED) {
				HaoDebug.log("[hao-entity]     × 视线射线没命中 " + identifier + " @ "
						+ String.format("%.1f,%.1f,%.1f", x, y, z));
			}
			return null;
		}
		// 材料不足**不再**把候选藏起来 —— 藏起来玩家就不知道"还差什么",连材料清单都看不到。
		// 改为照常出候选、界面上把缺的材料标红,点击时给出提示(见 EntityPlacementScreen)。
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
		if (HaoDebug.ENABLED && info.nbt.contains("Item")) {
			HaoDebug.log("[hao-entity]   框内物品=" + info.nbt.get("Item") + " @ " + identifier);
		}
		return new EntitySpawn(identifier, key, x, y, z, yaw, pitch, nbt);
	}

	@Unique
	private static void hao$dispatchPacket(EntitySpawn spawn, PlayerEntity player) {
		int count = EasyPlaceEntitySettings.count(player.getUuid());
		EntityPlacementHandler.registerPayloadType();
		ClientPlayNetworking.send(new EntityPlacementPayload(List.of(spawn), count));
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-entity] 放置 " + spawn.type() + " @ "
					+ String.format("%.1f,%.1f,%.1f", spawn.x(), spawn.y(), spawn.z()));
		}
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

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
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.position.LayerRange;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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

import carpet_hao_addition.HaoDebug;
/**
 * haoProjectionEntity(客户端,26.x):照投影轻松放置时,把玩家**真正指着**的投影实体报给服务端生成。
 * <p>
 * 严格对照 1.21.8 的同名 mixin 移植,规则语义完全一致:
 * <ol>
 *   <li><b>右键路径</b>要求准星对着方块;<b>快捷键路径不要求</b> —— 快捷键的判定在
 *       {@code hao$collectCandidates} 里用视线射线自己做,而 {@code hitResult} 会命中**实体**
 *       (第一次放出的那个往往正好挡在准星上)。</li>
 *   <li><b>候选判定</b>用视线射线求交:先按准星命中的方块位置挑最近的一处,再按"射线穿过生成盒",
 *       最后退回"离射线最近";都不满足才淘汰。</li>
 *   <li><b>渲染层范围</b>:{@code DataManager.getRenderLayerRange().isPositionWithinRange(...)},
 *       没渲染出来的实体不参与。</li>
 *   <li><b>位置变换</b>:投影的镜像/旋转再做一次,最后补「投影原点 + 子区域偏移」。</li>
 *   <li><b>不做材料预检</b>:材料不足照样出候选,界面标红并在点击时提示。</li>
 *   <li><b>候选为空也弹选择器</b>,让玩家看到"确实是 0 个",而不是按了没反应。</li>
 * </ol>
 * <p>
 * 26.x 的映射差异:{@code EntityInfo} 是 record 且 {@code nbt()} 给的是 malilib 的 {@code CompoundData},
 * 必须过 {@link DataConverterNbt#toVanillaCompound} 才能拿到 vanilla NBT;
 * 实体盒用 {@code getSpawnAABB};射线求交用 {@code AABB.clip};准星命中是 {@code Minecraft#hitResult};
 * 当前界面是 {@code Minecraft#gui().screen()};动作栏提示是 {@code Player#sendOverlayMessage}。
 * <p>
 * 注入方法签名必须与目标一致:{@code doEasyPlaceAction(Minecraft)} 返回 {@code InteractionResult},
 * 所以用 {@code CallbackInfoReturnable}(写错编译期看不出来,运行时抛 InvalidInjectionException 崩游戏)。
 */
@Mixin(value = WorldUtils.class, priority = 850)
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
	private static final Map<Long, Long> hao$recentEntityRequests = new HashMap<>();

	/** 同一格在此期间内只生成一次(2 秒,与 haoProjectionWaterlogged 的请求 TTL 一致)。 */
	@Unique
	private static final long HAO_ENTITY_REQUEST_TTL = 40L;

	/**
	 * 射线到投影坐标的最短距离在多少以内算「瞄准了这一处」(单位:格的平方)。
	 * <p>取 1.2 格:实体出生盒本身不到一格宽,要求射线精确穿过太苛刻 —— 玩家看到的是投影渲染
	 * 出来的虚影,鼠标压在虚影上时人眼误差折算到十来格外的射线末端会差一大截,差一点就判不中,
	 * 表现就是"同一处有时能开界面、有时开不了"。放宽到 1.2 格后准星大致对着就能选中;
	 * 再大就会把并排的相邻那一处也算进来。</p>
	 */
	@Unique
	private static final double AIM_SQUARED = 1.44D;

	/**
	 * 每个候选的「投影里几个 / 世界里已有几个」,键是 {@code EntitySpawn#key()}。
	 * <p>
	 * 刻意**不**放进 {@link EntitySpawn}:那是个带 {@code StreamCodec} 的 record,加字段会改动
	 * 跨 mod 协议(HaoMaSa 与 26.x 都按同一个 codec 收发)。这两个数只给客户端的选择器界面用,
	 * 不需要发给服务端,所以单独放一份。
	 */
	@Unique
	private static final Map<String, int[]> HAO_COUNTS = new HashMap<>();

	@Inject(method = "doEasyPlaceAction", at = @At("HEAD"), cancellable = true)
	private static void hao$spawnEntityOnEasyPlace(Minecraft minecraft, CallbackInfoReturnable<InteractionResult> cir) {
		// 注入点在 HEAD:这里一旦抛异常,整个轻松放置(包括方块)都会失效。所以兜住一切意外。
		try {
			hao$handleEasyPlaceEntity(minecraft, cir);
		} catch (Throwable throwable) {
			HaoDebug.log("[hao-entity] 出错(已忽略,不影响方块放置): " + throwable);
			HaoDebug.stackTrace(throwable);
		}
	}

	@Unique
	private static void hao$handleEasyPlaceEntity(Minecraft minecraft, CallbackInfoReturnable<InteractionResult> cir) {
		LocalPlayer player = minecraft.player;
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
						+ " 准星是方块=" + (minecraft.hitResult instanceof BlockHitResult));
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
		// 而 hitResult 会命中**实体**:第一次放出的那个实体往往正好挡在准星上,
		// 于是"在同一个地方再按一次"就会被误判成"准星没有指着方块"。
		if (!fromHotkey && !(minecraft.hitResult instanceof BlockHitResult)) {
			HaoDebug.log("[hao-entity] 右键路径:准星没对着方块");
			return;
		}
		long tick = player.level().getGameTime();
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
			// 没收集到候选也把选择器弹出来:让玩家看到"确实是 0 个",
			// 而不是按了没反应、也分不清是没瞄准还是功能没生效。
			// 已经有别的界面开着时不抢。
			if (minecraft.gui.screen() == null) {
				minecraft.setScreenAndShow(new EntityPlacementScreen(candidates, HAO_COUNTS));
				cir.setReturnValue(InteractionResult.SUCCESS);
			}
			return;
		}
		hao$lastEntityTick = tick;
		if (candidates.size() > 1 || EasyPlaceEntitySettings.uiEnabled(player.getUUID())) {
			// 多个候选时必须让玩家点(代码猜不出来"你指着谁");只命中一个时是否还弹界面,
			// 由 /easyPlaceEntityUi 决定 —— 开着就总是弹界面,关掉就直接放置(更快)。
			minecraft.setScreenAndShow(new EntityPlacementScreen(candidates, HAO_COUNTS));
			cir.setReturnValue(InteractionResult.SUCCESS);
			return;
		}
		// 单候选:同一格 2 秒内只放一次,否则按住右键会刷出一堆。快捷键不受此限。
		// TTL 的键统一用"视线终点所在格":快捷键路径没有可靠的 hitResult
		// (可能被实体挡住),而这条射线两条路径都能拿到。
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 lookEnd = eye.add(player.getViewVector(1.0F).scale(Math.max(4.5D, Math.max(
				WorldUtils.getValidBlockRange(minecraft), player.blockInteractionRange()))));
		BlockPos target = BlockPos.containing(lookEnd);
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
	private static void hao$notify(LocalPlayer player, String translationKey) {
		player.sendOverlayMessage(Component.translatable(translationKey));
	}

	/** 点到视线线段的最短距离,用来判断"准星更接近哪一处"。 */
	@Unique
	private static double hao$distanceToRay(Vec3 start, Vec3 end, double x, double y, double z) {
		final Vec3 direction = end.subtract(start);
		final double length = direction.length();

		if (length <= 1.0E-4D) {
			return Math.sqrt(start.distanceToSqr(x, y, z));
		}

		final Vec3 unit = direction.scale(1.0D / length);
		final Vec3 toTarget = new Vec3(x - start.x, y - start.y, z - start.z);
		final double along = Math.max(0.0D, Math.min(length, toTarget.dot(unit)));
		final Vec3 closest = start.add(unit.scale(along));

		return Math.sqrt(closest.distanceToSqr(x, y, z));
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
		// 交互距离取三者的最大值:Litematica 给的有效放置距离、玩家自身的方块交互距离、
		// 以及原版 4.5 的下限。只取 Litematica 那个值时,某些设置下它只有两三格,
		// 射线够不到稍远处那台实体 —— 玩家明明瞄准了却判成"没命中",界面打不开。
		double range = Math.max(4.5D, Math.max(WorldUtils.getValidBlockRange(mc),
				player.blockInteractionRange()));
		Vec3 end = start.add(player.getViewVector(1.0F).scale(range));
		// 诊断(临时):玩家眼睛位置 + 视线可达距离。
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
				new Vec3(spawn.x(), spawn.y(), spawn.z()).distanceToSqr(start)));
		// 只留一处:并排的两处投影常常只差一格,射线是一整条线,会**连续穿过一串格子** ——
		// 站在一处旁边往另一处看时,两处都会被判成"穿过",再比距离就会选回近的那一处,
		// 表现就是"对着 17032 却弹出 17033"。所以判据按精确度分三档:
		// ① 准星命中的那个方块位置(只有一个点,最接近"我盯着哪一格")
		// ② 射线穿过实体出生盒的那些
		// ③ 都没有才退回"离射线最近"
		if (found.size() > 1) {
			String keep = null;

			if (mc.hitResult instanceof BlockHitResult blockHit) {
				final Vec3 hitPos = blockHit.getLocation();
				double best = Double.MAX_VALUE;

				for (EntitySpawn spawn : found) {
					final double distance = hitPos.distanceToSqr(spawn.x(), spawn.y(), spawn.z());

					if (distance < best) {
						best = distance;
						keep = EntityPlacementCounter.positionKey(spawn);
					}
				}
			}

			if (keep == null) {
				final List<EntitySpawn> exactHits = new ArrayList<>();

				for (EntitySpawn spawn : found) {
					final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(spawn.type());

					if (type == null) {
						continue;
					}

					if (type.getSpawnAABB(spawn.x(), spawn.y(), spawn.z())
							.clip(start, end).isPresent()) {
						exactHits.add(spawn);
					}
				}

				final List<EntitySpawn> pool = exactHits.isEmpty() ? found : exactHits;
				double nearestAim = Double.MAX_VALUE;

				for (EntitySpawn spawn : pool) {
					final double aim = hao$distanceToRay(start, end, spawn.x(), spawn.y(), spawn.z());

					if (aim < nearestAim) {
						nearestAim = aim;
						keep = EntityPlacementCounter.positionKey(spawn);
					}
				}
			}

			if (keep != null) {
				final String chosen = keep;
				found.removeIf(spawn -> !EntityPlacementCounter.positionKey(spawn).equals(chosen));
			}
		}
		// 按"位置 + 类型"计数:同一处投影里有 N 个实体,世界里已有 M 个。
		// ⚠️ 这里**不再**把"已放满"的候选从列表里去掉 —— 选择器界面要能随时打开,玩家才能
		// ①继续加放(比如投影 2 个只放了 1 个,或想多放几个)②核对投影到底缺不缺。
		// 数量另存一份带进界面(见 HAO_COUNTS),候选本身原样保留。
		HAO_COUNTS.clear();
		if (mc.level != null) {
			HAO_COUNTS.putAll(EntityPlacementCounter.totals(mc.level, found));
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
	private static void hao$collectFrom(Minecraft mc, SchematicPlacement placement,
			Vec3 rayStart, Vec3 rayEnd, List<EntitySpawn> out) {
		LitematicaSchematic schematic = placement.getSchematic();
		if (schematic == null) {
			return;
		}
		BlockPos origin = placement.getOrigin();
		// 优先用"启用中的子区域";但有些投影/操作流程下这个集合是空的,那就退回遍历投影里的
		// 全部区域 —— 否则一个实体都收不到,表现就是"完全没有反应"。
		Set<String> regionNames = new LinkedHashSet<>(
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

	/** 把一个投影实体变成候选;不满足条件(没渲染/没命中)则返回 null。 */
	@Unique
	private static EntitySpawn hao$candidateFor(Minecraft mc, SchematicPlacement placement,
			SubRegionPlacement sub, BlockPos origin, String regionName, int index, EntityInfo info,
			Vec3 rayStart, Vec3 rayEnd) {
		// 投影实体的身份 —— 去重只能靠它,不能靠坐标(投影里可能有几个实体坐标完全重合)。
		// key 里必须带**这个投影自己的标识**:只用「子区域#序号」的话,同时显示两个投影、
		// 而它们恰好有同名子区域(单区域投影常常都叫 main)且实体序号相同时,两边的 key 会
		// 撞在一起 —— HAO_COUNTS 与 EntityPlacementCounter.totals 都拿 key 当分组键,
		// 后写的覆盖先写的,界面上的「需要/已放」就串到另一个投影上去了。
		// 用投影原点当标识:两个投影叠在同一位置时确实分不开,但那种情况玩家自己也分不清。
		// 注意 key 会进网络包(buf.writeUtf(spawn.key(), 256)),子区域名过长必须截断,
		// 否则序列化时直接抛异常。
		String regionKey = regionName.length() > 96 ? regionName.substring(0, 96) : regionName;
		String key = origin.asLong() + "/" + regionKey + "#" + index;
		// ⚠️ 下面这段诊断里 NBT 的 toString 会把整棵 NBT 树序列化一遍,
		// 而 hao$candidateFor 是"每个投影实体 × 每 tick"都走的路径 —— 必须靠 ENABLED 短路。
		if (HaoDebug.ENABLED) {
			// malilib 的 CompoundData#getString 直接返回 String(可能为 null),不是 Optional。
			String debugId = info.nbt().getString("id");
			HaoDebug.log("[hao-entity]    区域 " + regionName + " 实体 "
					+ (debugId == null || debugId.isEmpty() ? "?" : debugId) + " 局部=" + info.posVec());
		}
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
		// 但子区域自身的变换已经体现在它的位置偏移里,再叠一次会让实体和投影错开
		// (表现就是"投影里明明卡在那个方块里,放出来却不在那儿")。
		Vec3 pos = PositionUtils.getTransformedPosition(info.posVec(),
				placement.getMirror(), placement.getRotation());
		BlockPos regionPos = sub == null ? BlockPos.ZERO : sub.getPos();
		double x = origin.getX() + regionPos.getX() + pos.x;
		double y = origin.getY() + regionPos.getY() + pos.y;
		double z = origin.getZ() + regionPos.getZ() + pos.z;
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-entity]    坐标 局部=" + info.posVec()
					+ " 区域=" + regionName + " 区域偏移=" + regionPos + " 投影原点=" + origin
					+ " 变换后=" + String.format("%.3f,%.3f,%.3f", pos.x, pos.y, pos.z)
					+ " 世界=" + String.format("%.3f,%.3f,%.3f", x, y, z));
		}
		// "我渲染出来的才放":渲染层范围比投影级渲染开关细一档,没渲染的层/位置不参与。
		// 拿不到范围对象时不过滤(宁可多给候选,也别把能放的挡掉)。
		LayerRange renderLayerRange = DataManager.getRenderLayerRange();
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
		boolean hitBox = type.getSpawnAABB(x, y, z).clip(rayStart, rayEnd).isPresent();
		boolean hitCell = new AABB(BlockPos.containing(x, y, z)).clip(rayStart, rayEnd).isPresent();
		// 兜底:准星命中了世界里某个同类型实体。矿车会顺铁轨滚、船会漂、盔甲架会落地,
		// 放出来之后常常离投影坐标一两格 —— 这时射线穿到的是**实体**,不是投影位置,
		// 上面两条都判不中,玩家明明指着那台矿车却打不开界面。所以再看一眼准星命中物。
		if (!hitBox && !hitCell
				&& mc.hitResult instanceof EntityHitResult entityHit
				&& entityHit.getEntity().getType() == type
				&& entityHit.getEntity().position().distanceToSqr(x, y, z) <= 4.0D) {
			hitBox = true;
		}
		// 再兜一层:射线到投影坐标的**最短距离**够近就算瞄准。
		// 上面两条都要求射线精确穿过出生盒(不到一格宽),而玩家看到的是投影渲染出来的虚影 ——
		// 鼠标压在虚影上时,人眼误差折算到十格外的射线末端就是一大截,差一点就判不中。
		// 表现就是"同一个位置有时能开、有时开不了"。
		if (!hitBox && !hitCell) {
			final Vec3 direction = rayEnd.subtract(rayStart);
			final double length = direction.length();

			if (length > 1.0E-4D) {
				final Vec3 unit = direction.scale(1.0D / length);
				final Vec3 toTarget = new Vec3(x - rayStart.x, y - rayStart.y, z - rayStart.z);
				final double along = Math.max(0.0D, Math.min(length, toTarget.dot(unit)));
				final Vec3 closest = rayStart.add(unit.scale(along));

				if (closest.distanceToSqr(x, y, z) <= AIM_SQUARED) {
					hitBox = true;
				}
			}
		}
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
		float yaw = hao$rotateYaw(hao$rotation(nbt, 0),
				placement.getMirror(), placement.getRotation());
		float pitch = hao$rotation(nbt, 1);
		// 把投影实体的 NBT 一起带上(Pos/Rotation 改写成世界坐标与新朝向):
		// 这样服务端能**连同乘客一起**还原 —— "矿车镶在船里"就是船带矿车乘客。
		// 去掉投影里的实体 UUID(含乘客):它常与世界里已有实体(之前粘贴出来的)撞车,
		// 导致实体加不进世界、凭空消失。
		hao$stripUuids(nbt);
		nbt.put("Pos", hao$nbtDoubles(x, y, z));
		nbt.put("Rotation", hao$nbtFloats(yaw, pitch));
		if (HaoDebug.ENABLED && nbt.contains("Item")) {
			HaoDebug.log("[hao-entity]   框内物品=" + nbt.get("Item") + " @ " + identifier);
		}
		return new EntitySpawn(identifier, key, x, y, z, yaw, pitch, nbt);
	}

	@Unique
	private static void hao$dispatchPacket(EntitySpawn spawn, LocalPlayer player) {
		int count = EasyPlaceEntitySettings.count(player.getUUID());
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
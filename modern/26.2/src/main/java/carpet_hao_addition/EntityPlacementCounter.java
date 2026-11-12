package carpet_hao_addition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 统计「这一处投影实体，世界里现在已经有几个」。
 *
 * <p>移植自 1.21.8 的 {@code entity.EntityPlacementCounter}，只是换成 26.x 的 Mojang 映射：
 * {@code ClientLevel} / {@code BuiltInRegistries} / {@code AABB} / {@code entityTags()}。</p>
 *
 * <p><b>为什么单独抽成一个类：</b>界面开着的时候玩家会当场放置，格子上「已放几个」必须跟着变；
 * 但选择器界面和触发入口（mixin）不在同一个类里，总不能各写一份判定 —— 两边判定一旦不一致，
 * 就会变成"界面说已放完、实际还能放"这种更难查的问题。所以判定只留这一份。</p>
 */
public final class EntityPlacementCounter {
	/** 查找范围：以投影坐标为底面的实体盒，往外放宽 4 格。 */
	private static final double SEARCH_EXPAND = 4.0D;

	/** 认定范围：实体中心到投影坐标的距离在多少格内，才算这一处放的。 */
	private static final double ACCEPT_DISTANCE = 0.6D;

	private EntityPlacementCounter() {
	}

	private static Map<String, Integer> assignNearest(ClientLevel world, Map<String, List<EntitySpawn>> groups) {
		final Map<String, Integer> have = new HashMap<>();

		if (world == null || groups.isEmpty()) {
			return have;
		}

		final Map<String, EntitySpawn> representatives = new LinkedHashMap<>();
		final Map<String, EntityType<?>> types = new HashMap<>();
		final Map<String, AABB> searches = new HashMap<>();

		for (Map.Entry<String, List<EntitySpawn>> entry : groups.entrySet()) {
			final EntitySpawn spawn = entry.getValue().get(0);
			final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(spawn.type()).map(net.minecraft.core.Holder.Reference::value).orElse(null);

			if (type == null) {
				continue;
			}

			final AABB target = type.getSpawnAABB(spawn.x(), spawn.y(), spawn.z());
			representatives.put(entry.getKey(), spawn);
			types.put(entry.getKey(), type);
			searches.put(entry.getKey(), target.inflate(SEARCH_EXPAND));
		}

		if (representatives.isEmpty()) {
			return have;
		}

		final Map<Entity, Boolean> candidates = new LinkedHashMap<>();

		for (AABB search : searches.values()) {
			for (Entity existing : world.getEntities((Entity) null, search, entity -> true)) {
				candidates.put(existing, Boolean.TRUE);
			}
		}

		for (Entity existing : candidates.keySet()) {
			// 乘客不单独计数：它已经被驮着它的那个载具一起算进去了。
			if (existing.getVehicle() != null) {
				continue;
			}

			String nearest = null;

			// ① 先按放置时打下的归属标记认。
			final Set<String> commandTags = existing.entityTags();

			for (Map.Entry<String, EntitySpawn> entry : representatives.entrySet()) {
				if (types.get(entry.getKey()) != existing.getType()) {
					continue;
				}

				if (commandTags.contains(EntitySpawn.placementTag(entry.getValue()))) {
					nearest = entry.getKey();
					break;
				}
			}

			// ② 没有标记的实体退回按距离挑最近的一处。
			double nearestDistance = Double.MAX_VALUE;

			if (nearest == null) {
				for (Map.Entry<String, EntitySpawn> entry : representatives.entrySet()) {
					if (types.get(entry.getKey()) != existing.getType()) {
						continue;
					}

					final EntitySpawn spawn = entry.getValue();
					final double distance = existing.position()
							.distanceToSqr(spawn.x(), spawn.y(), spawn.z());

					if (distance > ACCEPT_DISTANCE * ACCEPT_DISTANCE) {
						continue;
					}

					if (distance < nearestDistance) {
						nearestDistance = distance;
						nearest = entry.getKey();
					}
				}
			}

			if (nearest != null) {
				have.merge(nearest, 1 + countPassengers(existing), Integer::sum);
			}
		}

		return have;
	}

	/** 递归数一个实体身上骑着的乘客（含乘客的乘客）。 */
	private static int countPassengers(Entity entity) {
		int count = 0;

		for (Entity passenger : entity.getPassengers()) {
			count += 1 + countPassengers(passenger);
		}

		return count;
	}

	/** 数一段实体 NBT 里挂着几个乘客（{@code Passengers} 列表，可递归）。 */
	public static int countPassengers(CompoundTag nbt) {
		if (nbt == null) {
			return 0;
		}

		final Optional<ListTag> list = nbt.getList("Passengers");

		if (list.isEmpty()) {
			return 0;
		}

		int count = 0;

		for (int i = 0; i < list.get().size(); i++) {
			if (list.get().get(i) instanceof CompoundTag passenger) {
				count += 1 + countPassengers(passenger);
			}
		}

		return count;
	}

	/** 「位置 + 类型」的粗粒度键：同一处的多个同类实体算同一组。 */
	public static String positionKey(EntitySpawn spawn) {
		return spawn.type() + "@" + (int) Math.floor(spawn.x()) + "," + (int) Math.floor(spawn.y())
				+ "," + (int) Math.floor(spawn.z());
	}

	/**
	 * 算出每个候选所属位置的「投影总共需要几个 / 世界里已经有几个」。
	 *
	 * @return 键为 {@code EntitySpawn#key()}，值为 {@code {需要, 已放}}
	 */
	public static Map<String, int[]> totals(ClientLevel world, List<EntitySpawn> spawns) {
		final Map<String, int[]> result = new HashMap<>();
		final Map<String, List<EntitySpawn>> groups = new LinkedHashMap<>();

		for (EntitySpawn spawn : spawns) {
			groups.computeIfAbsent(positionKey(spawn), key -> new ArrayList<>()).add(spawn);
		}

		final Map<String, Integer> projectedByPos = new HashMap<>();

		for (Map.Entry<String, List<EntitySpawn>> entry : groups.entrySet()) {
			int need = 0;

			for (EntitySpawn spawn : entry.getValue()) {
				need += 1 + countPassengers(spawn.nbt());
			}

			projectedByPos.put(entry.getKey(), need);
		}

		final Map<String, Integer> existingByPos = assignNearest(world, groups);

		for (EntitySpawn spawn : spawns) {
			final String posKey = positionKey(spawn);
			final int need = projectedByPos.getOrDefault(posKey, 1);
			final int have = Math.min(existingByPos.getOrDefault(posKey, 0), need);
			result.put(spawn.key(), new int[]{need, have});
		}

		return result;
	}

	/** 便捷重载：用客户端的当前世界。 */
	public static Map<String, int[]> totals(List<EntitySpawn> spawns) {
		return totals(Minecraft.getInstance().level, spawns);
	}
}
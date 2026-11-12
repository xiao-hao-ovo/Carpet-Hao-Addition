package carpet_hao_addition.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Box;

/**
 * 统计「这一处投影实体,世界里现在已经有几个」。
 *
 * <p><b>为什么单独抽成一个类：</b>界面开着的时候玩家会当场放置,格子上「已放几个」必须跟着变;
 * 但选择器界面和触发入口(mixin)不在同一个类里,总不能各写一份判定 —— 两边判定一旦不一致,
 * 就会变成"界面说已放完、实际还能放"这种更难查的问题。所以判定只留这一份。
 * </p>
 *
 * <p><b>为什么用碰撞箱相交、而不是"与投影坐标的距离"：</b>投影里的坐标是实体<b>应该在</b>的位置,
 * 而放出来的盔甲架/矿车/船会受重力与碰撞箱修正<b>落地或轻微偏移</b>,经常差半格到一格。
 * 早先版本用 {@code squaredDistanceTo <= 0.36}(= 0.6 格)判定,结果实体明明在那儿却数不到,
 * 界面就一直显示"还缺几个"。
 * </p>
 */
public final class EntityPlacementCounter
{
	/** 查找范围:以投影坐标为底面的实体盒,往外放宽 4 格。
	 *  放宽得多一点是因为矿车会顺铁轨滑、船会漂 —— 有归属标记时按标记认,滑远也能认回来;
	 *  没有标记的旧实体仍受下面的认定范围限制,不会因为搜得宽就误算。 */
	private static final double SEARCH_EXPAND = 4.0D;

	/** 认定范围:实体**中心**到投影坐标的距离在多少格内,才算这一处放的。
	 *  不能用"碰撞箱相交"来判断:矿车的碰撞箱宽 0.98 格,而并排的两处投影常常正好差 1 格 ——
	 *  实体一侧几乎贴着隔壁那处的坐标,放宽多少都还会相交,于是这处没放却显示已放满。
	 *  改用中心点距离:同一处放的实体中心就在坐标上(距离 0),隔壁那处至少差接近 1 格,分得开。
	 *  落地/漂移的偏移由放置时打下的归属标记兜住。 */
	private static final double ACCEPT_DISTANCE = 0.6D;

	private EntityPlacementCounter()
	{
	}

	/**
	 * 把世界里的实体按「离哪个投影位置最近」分给各位置,每个实体只归一组。
	 *
	 * <p>不能每组各自数一遍:认定范围是"投影坐标的出生盒再放宽一格",
	 * 并排摆着、只差一格的两处会让同一个实体同时落进彼此的范围内 ——
	 * 于是两边都把对方放的那几个数进自己的「已放」,没放的那处直接显示完成。
	 * </p>
	 *
	 * <p>改成唯一分配:先把各处范围内的实体汇总去重,再对每个实体在所有位置里
	 * 挑最近的一个(且必须落进它的认定范围),只累加给这一组。</p>
	 *
	 * @param groups 键为 {@link #positionKey},值为该位置的全部候选
	 * @return 键为 {@link #positionKey},值为「世界里已经有几个」(含乘客)
	 */
	private static java.util.Map<String, Integer> assignNearest(ClientWorld world,
			java.util.Map<String, java.util.List<EntitySpawn>> groups)
	{
		final java.util.Map<String, Integer> have = new java.util.HashMap<>();

		if (world == null || groups.isEmpty())
		{
			return have;
		}

		final java.util.Map<String, EntitySpawn> representatives = new java.util.LinkedHashMap<>();
		final java.util.Map<String, EntityType<?>> types = new java.util.HashMap<>();
		final java.util.Map<String, Box> searches = new java.util.HashMap<>();

		for (java.util.Map.Entry<String, java.util.List<EntitySpawn>> entry : groups.entrySet())
		{
			final EntitySpawn spawn = entry.getValue().get(0);
			final EntityType<?> type = Registries.ENTITY_TYPE.get(spawn.type());

			if (type == null)
			{
				continue;
			}

			final Box target = type.getSpawnBox(spawn.x(), spawn.y(), spawn.z());
			representatives.put(entry.getKey(), spawn);
			types.put(entry.getKey(), type);

			searches.put(entry.getKey(), target.expand(SEARCH_EXPAND));
		}

		if (representatives.isEmpty())
		{
			return have;
		}

		// 逐处搜自己的范围再去重,而不是把各处范围并成一个大盒子:
		// 同时加载两个相距很远的投影时,并集会把中间整片区域也扫进来。
		final java.util.Map<Entity, Boolean> candidates = new java.util.LinkedHashMap<>();

		for (Box search : searches.values())
		{
			for (Entity existing : world.getOtherEntities(null, search))
			{
				candidates.put(existing, Boolean.TRUE);
			}
		}

		for (Entity existing : candidates.keySet())
		{
			// 乘客不单独计数:它已经被驮着它的那个载具一起算进去了。
			// 不跳过的话,"矿车驮着矿车"这种同类型乘客会被计两次。
			if (existing.getVehicle() != null)
			{
				continue;
			}

			String nearest = null;

			// ① 先按放置时打下的归属标记认。并排的两处投影只差一格,放出来的矿车又会顺铁轨滑,
			// 光比世界坐标分不清某一台属于哪一处 —— 标记是唯一精确的依据,对上了就直接归这一处,
			// 不再看距离(滑出去一两格也算这一处的)。
			final java.util.Set<String> commandTags = existing.getCommandTags();

			for (java.util.Map.Entry<String, EntitySpawn> entry : representatives.entrySet())
			{
				if (types.get(entry.getKey()) != existing.getType())
				{
					continue;
				}

				if (commandTags.contains(EntitySpawn.placementTag(entry.getValue())))
				{
					nearest = entry.getKey();
					break;
				}
			}

			// ② 没有标记的实体(旧版本放出来的、或别的途径生成的)退回按距离挑最近的一处。
			double nearestDistance = Double.MAX_VALUE;

			if (nearest == null)
			{
				for (java.util.Map.Entry<String, EntitySpawn> entry : representatives.entrySet())
				{
					if (types.get(entry.getKey()) != existing.getType())
					{
						continue;
					}

					final EntitySpawn spawn = entry.getValue();
					final double distance = existing.getPos().squaredDistanceTo(spawn.x(), spawn.y(), spawn.z());

					// 只认中心点落在这一处附近的:用碰撞箱相交判断不行 —— 矿车碰撞箱宽 0.98 格,
					// 并排的两处又常差 1 格,实体一侧几乎贴着隔壁的坐标,怎么放宽都会相交。
					if (distance > ACCEPT_DISTANCE * ACCEPT_DISTANCE)
					{
						continue;
					}

					if (distance < nearestDistance)
					{
						nearestDistance = distance;
						nearest = entry.getKey();
					}
				}
			}

			if (nearest != null)
			{
				// 载具 + 它驮着的乘客算一整组。投影里"船带着矿车""矿车带着漏斗"就是
				// 一个 NBT 的 Passengers;只数载具会让"材料不够、乘客被跳过"看起来像已完成。
				have.merge(nearest, 1 + countPassengers(existing), Integer::sum);
			}
		}

		return have;
	}

	/** 递归数一个实体身上骑着的乘客(含乘客的乘客)。 */
	private static int countPassengers(Entity entity)
	{
		int count = 0;

		for (Entity passenger : entity.getPassengerList())
		{
			count += 1 + countPassengers(passenger);
		}

		return count;
	}

	/**
	 * 数一段实体 NBT 里挂着几个乘客({@code Passengers} 列表,可递归)。
	 *
	 * <p>投影侧的「需要几个」要靠它:一个载具的 NBT 里可能还挂着几层乘客,
	 * 材料不足时服务端会把这些乘客整批跳过,所以「需要」必须把它们算进去,
	 * 否则载具一放就显示"完成"。</p>
	 */
	public static int countPassengers(net.minecraft.nbt.NbtCompound nbt)
	{
		if (nbt == null)
		{
			return 0;
		}

		final java.util.Optional<net.minecraft.nbt.NbtList> list = nbt.getList("Passengers");

		if (list.isEmpty())
		{
			return 0;
		}

		int count = 0;

		for (int i = 0; i < list.get().size(); i++)
		{
			if (list.get().get(i) instanceof net.minecraft.nbt.NbtCompound passenger)
			{
				count += 1 + countPassengers(passenger);
			}
		}

		return count;
	}

	/**
	 * 「位置 + 类型」的粗粒度键:同一处的多个同类实体算同一组。
	 * <p>
	 * 分组判定必须和 {@link #countNearby} 的「同一处」一致,否则会出现
	 * "按位置算有 2 个、按格子分却分不出组"的矛盾。所以这两件事都留在这一个类里。
	 */
	public static String positionKey(EntitySpawn spawn)
	{
		return spawn.type() + "@" + (int) Math.floor(spawn.x()) + "," + (int) Math.floor(spawn.y())
				+ "," + (int) Math.floor(spawn.z());
	}

	/**
	 * 算出每个候选所属位置的「投影总共需要几个 / 世界里已经有几个」。
	 *
	 * <p><b>这里是整处统计,不是每格各算一个:</b>投影里同一处叠了几个实体时(比如两辆漏斗矿车),
	 * 该位置的每个格子都拿到<b>同一份</b> {@code {需要, 已放}} —— 界面要回答的是
	 * "这一整处一共要几个、现在有几个、还缺几个",而不是"这一格代表哪一个"。
	 * 和玩家设置的「一次放几个」完全无关。
	 * </p>
	 *
	 * @param spawns 界面上的全部候选(或一次收集到的候选)
	 * @return 键为 {@code EntitySpawn#key()},值为 {@code {需要, 已放}}
	 */
	public static java.util.Map<String, int[]> totals(ClientWorld world, java.util.List<EntitySpawn> spawns)
	{
		final java.util.Map<String, int[]> result = new java.util.HashMap<>();
		final java.util.Map<String, java.util.List<EntitySpawn>> groups = new java.util.LinkedHashMap<>();

		for (EntitySpawn spawn : spawns)
		{
			groups.computeIfAbsent(positionKey(spawn), key -> new java.util.ArrayList<>()).add(spawn);
		}

		final java.util.Map<String, Integer> projectedByPos = new java.util.HashMap<>();

		for (java.util.Map.Entry<String, java.util.List<EntitySpawn>> entry : groups.entrySet())
		{
			int need = 0;

			for (EntitySpawn spawn : entry.getValue())
			{
				// 一个候选可能是"载具 + 车上的乘客"整组(投影里就是一个 NBT 的 Passengers),
				// 所以要连乘客一起算「需要几个」,否则载具一放就显示"完成"。
				need += 1 + countPassengers(spawn.nbt());
			}

			projectedByPos.put(entry.getKey(), need);
		}

		// 每个已放置的实体只算给离它最近的那一处,不再重复计入相邻位置
		final java.util.Map<String, Integer> existingByPos = assignNearest(world, groups);


		for (EntitySpawn spawn : spawns)
		{
			final String posKey = positionKey(spawn);
			final int need = projectedByPos.getOrDefault(posKey, 1);
			// 已放按需要数封顶:一处最多就是这 N 个,数出 20 来只说明世界里那些实体
			// (矿车会顺铁轨滚、船会漂)滚到了这一处附近,不能算成"这一处多放了 10 个"。
			// 不封顶的话,打开并排的第一处会看到两处的合计,像是数量串到了别处。
			final int have = Math.min(existingByPos.getOrDefault(posKey, 0), need);
			result.put(spawn.key(), new int[] {need, have});
		}

		return result;
	}

	/** 便捷重载:用客户端的当前世界。 */
	public static java.util.Map<String, int[]> totals(java.util.List<EntitySpawn> spawns)
	{
		return totals(MinecraftClient.getInstance().world, spawns);
	}
}

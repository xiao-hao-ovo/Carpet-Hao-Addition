package carpet_hao_addition;

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
	/** 查找范围:以投影坐标为底面的实体盒,往外放宽 2 格。 */
	private static final double SEARCH_EXPAND = 2.0D;

	/** 认定范围:实体的包围盒碰到"放宽 1 格"的这个盒子,就算同一处。 */
	private static final double ACCEPT_EXPAND = 1.0D;

	private EntityPlacementCounter()
	{
	}

	/** 世界里「就在这一处」已经有多少个同类型实体 —— **连乘客一起算**。 */
	public static int countNearby(ClientWorld world, EntitySpawn spawn)
	{
		if (world == null)
		{
			return 0;
		}

		final EntityType<?> type = Registries.ENTITY_TYPE.get(spawn.type());

		if (type == null)
		{
			return 0;
		}

		final Box target = type.getSpawnBox(spawn.x(), spawn.y(), spawn.z());
		final Box accepted = target.expand(ACCEPT_EXPAND);
		int count = 0;

		for (Entity existing : world.getOtherEntities(null, target.expand(SEARCH_EXPAND)))
		{
			// 乘客不单独计数:它已经被上面载具那一支的 countPassengers 算进去了。
			// 不跳过的话,"矿车驮着矿车"这种同类型乘客会被计两次。
			if (existing.getVehicle() != null)
			{
				continue;
			}

			// 只认"这一处"的同类型实体:并排/上下紧挨着的同类实体不算同一处
			if (existing.getType() == type && existing.getBoundingBox().intersects(accepted))
			{
				// 载具 + 它驮着的乘客算一整组。投影里"船带着矿车""矿车带着漏斗"就是
				// 一个 NBT 的 Passengers;只数载具会让"材料不够、乘客被跳过"看起来像已完成。
				count += 1 + countPassengers(existing);
			}
		}

		return count;
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
		final java.util.Map<String, Integer> projectedByPos = new java.util.HashMap<>();
		final java.util.Map<String, Integer> existingByPos = new java.util.HashMap<>();

		for (EntitySpawn spawn : spawns)
		{
			final String posKey = positionKey(spawn);
			// 一个候选可能是"载具 + 车上的乘客"整组(投影里就是一个 NBT 的 Passengers),
			// 所以要连乘客一起算「需要几个」,否则载具一放就显示"完成"。
			projectedByPos.merge(posKey, 1 + countPassengers(spawn.nbt()), Integer::sum);
			// 每个位置只数一次世界里的实体
			existingByPos.computeIfAbsent(posKey, key -> countNearby(world, spawn));
		}

		for (EntitySpawn spawn : spawns)
		{
			final String posKey = positionKey(spawn);
			result.put(spawn.key(), new int[] {
					projectedByPos.getOrDefault(posKey, 1),
					existingByPos.getOrDefault(posKey, 0)});
		}

		return result;
	}

	/** 便捷重载:用客户端的当前世界。 */
	public static java.util.Map<String, int[]> totals(java.util.List<EntitySpawn> spawns)
	{
		return totals(MinecraftClient.getInstance().world, spawns);
	}
}

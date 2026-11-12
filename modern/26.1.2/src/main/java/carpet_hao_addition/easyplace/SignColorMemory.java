package carpet_hao_addition.easyplace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * easyplace 放标牌时"我给这个位置设过什么颜色"的记录表。
 * <p>
 * 26.x 的标牌方块实体会被**多份 NBT 先后加载**:客户端本地预测、服务端 BE 包、方块更新导致的
 * BE 重建……其中有的那份**文字在、颜色却是黑的**,会把刚设好的颜色冲掉(实测日志里同一个 pos
 * 会走成 lime → black → black)。原来在 {@code SignBlockEntity_colorProbeMixin} 里"读到非黑就记、
 * 读到黑就补回来并删记录"的写法只救得回一次,而"空 BE 重建"那一下就把记录消耗掉了,所以颜色
 * 回不来。
 * <p>
 * 现在改成:由 {@link carpet_hao_addition.mixin.SignBlockEntity_colorProbeMixin} 每次加载后查这里,
 * **只要读到黑、而我们记过该位置的颜色,就补回去**(记录不删,因此多少份黑 NBT 都挡得住)。
 * 玩家手动把标牌染成别的颜色时颜色不是黑,不会被动;只有染黑才会被补回原色(再染一次即可)。
 */
public final class SignColorMemory {
	/** 记录上限,防止长期累积。 */
	private static final int MAX_ENTRIES = 8192;

	/** key = 侧别 + 位置 + 面,客户端与服务端各一份。 */
	private static final Map<String, DyeColor> COLORS = new ConcurrentHashMap<>();

	private SignColorMemory() {
	}

	private static String cacheKey(boolean clientSide, BlockPos pos, String slot) {
		return (clientSide ? "C" : "S") + pos.asLong() + ":" + slot;
	}

	/** 记下"我们在该位置设过这个颜色";黑色是默认色,不记。 */
	public static void remember(boolean clientSide, BlockPos pos, String slot, DyeColor color) {
		if (color == DyeColor.BLACK) {
			return;
		}
		if (COLORS.size() < MAX_ENTRIES) {
			COLORS.put(cacheKey(clientSide, pos, slot), color);
		}
	}

	/** 取该位置记过的颜色;没记过返回 null。 */
	public static @Nullable DyeColor recall(boolean clientSide, BlockPos pos, String slot) {
		return COLORS.get(cacheKey(clientSide, pos, slot));
	}
}

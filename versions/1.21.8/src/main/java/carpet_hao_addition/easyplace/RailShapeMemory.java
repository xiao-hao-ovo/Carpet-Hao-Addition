package carpet_hao_addition.easyplace;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 记住「投影放下的铁轨该是什么形状」。
 *
 * <p>投影铺弯道、坡道是一格一格放的。每放下一节，原版都会给四周邻居跑一遍连接重算
 * （{@code AbstractRailBlock.updateCurves}），把已经铺好的那几节按「连向新轨」重新排形状，
 * 弯轨被拉直、坡轨被压平。协议里的「别自动改形状」只护得住刚放下的那一格，
 * 邻居身上没有这个标记，照样被改。铺一段弯道最后只剩最后一节是弯的，就是这个原因。
 *
 * <p>所以这里把投影放过的每一格连同它该有的<b>形状</b>记下来，邻居重算时命中就还原。
 * 表里只有投影放过的格，玩家自己摆的铁轨完全不受影响。
 *
 * <p><b>只记形状，不记整份状态。</b>铁轨还有别的属性会随时变，最典型的是动力铁轨的
 * {@code POWERED}：放下那一刻它往往还是 {@code false}，由 {@code updateBlockState}
 * 随后读红石改成 {@code true}。要是把整份状态存下来再还原，就会把已经充上的电抹掉，
 * 表现为「铁轨放下来了，但没被激活，得再给一次红石」。
 *
 * <p>表按世界区分：换维度就清空。过了 {@code TTL} 也清，
 * 免得玩家之后手动改铁轨时被几秒前的一次投影放置牵着走。
 */
public final class RailShapeMemory {

	/** 记忆保留的 tick 数。投影铺铁轨是连续的，200 tick 足够盖住一整段。 */
	private static final int TTL = 200;

	/** 表的上限。长时间连续放置时直接清空重来，不做 LRU。 */
	private static final int MAX_ENTRIES = 1024;

	private static final Map<BlockPos, RailShape> EXPECTED = new HashMap<>();
	private static World rememberedWorld;
	private static long expireAt;

	private RailShapeMemory() {
	}

	/** 记下这一格投影要求的形状。只取形状，同一格的其他属性不归这里管。 */
	public static void remember(World world, BlockPos pos, BlockState state) {
		RailShape shape = shapeOf(state);
		if (shape == null) {
			return;
		}
		if (rememberedWorld != world) {
			EXPECTED.clear();
			rememberedWorld = world;
		}
		if (EXPECTED.size() >= MAX_ENTRIES) {
			EXPECTED.clear();
		}
		EXPECTED.put(pos.toImmutable(), shape);
		expireAt = world.getTime() + TTL;
	}

	/**
	 * 这一格投影要求的形状。
	 *
	 * @param block 这一格当前是哪种铁轨，用来确认方块种类没被换掉
	 * @return 没有记录、已过期、或者方块已经不是同一种时返回 {@code null}
	 */
	public static RailShape expected(World world, BlockPos pos, Block block) {
		if (rememberedWorld != world) {
			return null;
		}
		if (world.getTime() > expireAt) {
			EXPECTED.clear();
			return null;
		}
		if (!(block instanceof AbstractRailBlock)) {
			return null;
		}
		return EXPECTED.get(pos);
	}

	private static RailShape shapeOf(BlockState state) {
		if (!(state.getBlock() instanceof AbstractRailBlock rail)) {
			return null;
		}
		Property<RailShape> property = rail.getShapeProperty();
		return state.contains(property) ? state.get(property) : null;
	}
}

package carpet_hao_addition.directdrops;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;

/**
 * 「掉落归属」上下文:记录当前正在破坏方块 / 击杀实体的玩家。
 * <p>
 * 原版逻辑可能在同一次操作里嵌套产生掉落(例如破坏一个方块时,相邻方块因失去支撑也在
 * 同一调用栈内掉落),因此用栈保存:最内层操作完成后,外层操作的归属自动恢复。
 * <p>
 * 这里用 {@link ArrayList} 而不是 {@code ArrayDeque},是为了<b>允许 null 元素</b>:
 * 外层属于玩家 A、内层因规则关闭而没有归属时,压入 null 能保证 HEAD/RETURN 严格配对,
 * 不会误弹外层的 A。栈顶为 null 表示「当前掉落不归属任何玩家」,按原版掉落在世界中。
 */
public final class DirectDropContext {
	private static final ThreadLocal<ArrayList<ServerPlayerEntity>> STACK = new ThreadLocal<>();

	private DirectDropContext() {}

	/**
	 * 压入一次「本次掉落归属」。
	 *
	 * @param owner 归属玩家;为 null 表示本次不接管掉落(原版行为)
	 */
	public static void push(ServerPlayerEntity owner) {
		ArrayList<ServerPlayerEntity> stack = STACK.get();
		if (stack == null) {
			stack = new ArrayList<>(4);
			STACK.set(stack);
		}
		stack.add(owner);
	}

	/** 与 {@link #push} 严格配对;必须无条件调用,否则会破坏外层归属。 */
	public static void pop() {
		ArrayList<ServerPlayerEntity> stack = STACK.get();
		if (stack == null || stack.isEmpty()) {
			return;
		}
		stack.remove(stack.size() - 1);
		// 栈空了**不清 ThreadLocal**:留着这个空表复用。
		// ServerWorld.tickBlock 的 HEAD/RETURN 各调一次 push(null)/pop(),而大型刷石机/农场
		// 每秒有几千个方块 tick —— 每次都 remove 掉,下次 push 又得 new 一个 ArrayList,
		// 等于每秒几千次分配。空表不持有任何实体引用,不存在泄漏。
	}

	/** 当前掉落应归属的玩家;无归属(栈空或栈顶为 null)时返回 null。 */
	public static ServerPlayerEntity currentPlayer() {
		ArrayList<ServerPlayerEntity> stack = STACK.get();
		return stack == null || stack.isEmpty() ? null : stack.get(stack.size() - 1);
	}

	public static void clearList() {
		STACK.remove();
	}
}

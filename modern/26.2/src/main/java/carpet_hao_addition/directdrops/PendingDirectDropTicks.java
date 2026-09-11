package carpet_hao_addition.directdrops;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 「跨 tick 连锁掉落」的归属登记表。
 * <p>
 * 玩家破坏方块时,原版会为受影响的方块(包括失去支撑的相邻方块)调用
 * {@code scheduleTick};而这些调用都发生在玩家操作的同一个调用栈内,所以能在这里记下
 * 「这个坐标稍后的掉落归谁」。等到该坐标真正被 tick 到时,再由 {@link #consume} 取出归属,
 * 使后续 tick 才产生的掉落也能进入背包。
 * <p>
 * 用 {@link WeakHashMap} 以世界为键,避免关闭世界后仍被强引用;条目本身只保留玩家 UUID。
 */
public final class PendingDirectDropTicks {
	/** 清理过期条目的间隔(游戏刻)。 */
	private static final long CLEANUP_INTERVAL_TICKS = 20L;
	/** 归属多保留的宽限刻数,避免因调度延迟抖动而丢归属。 */
	private static final long GRACE_TICKS = 20L;

	private static final Map<ServerLevel, WorldEntries> PENDING = new WeakHashMap<>();

	private PendingDirectDropTicks() {}

	/**
	 * 登记一次「该坐标的方块 tick 应归属于该玩家」。
	 *
	 * @param delay 该方块 tick 被调度的延迟(刻);用于推算过期时间
	 */
	public static void record(ServerLevel level, BlockPos pos, Block block, int delay, ServerPlayer player) {
		if (level == null || pos == null || block == null || player == null) {
			return;
		}
		WorldEntries entries = PENDING.get(level);
		if (entries == null) {
			entries = new WorldEntries();
			PENDING.put(level, entries);
		}
		long now = level.getGameTime();
		entries.cleanupIfDue(now);
		Key key = new Key(pos, block);
		// 原版对相同的方块 tick 会去重,因此第一次登记的归属也应优先保留。
		if (entries.entries.containsKey(key)) {
			return;
		}
		entries.entries.put(key, new Entry(player.getUUID(), now + Math.max(delay, 0) + GRACE_TICKS));
	}

	/** 取出并移除该坐标的归属;无归属或已过期时返回 null。 */
	public static ServerPlayer consume(ServerLevel level, BlockPos pos, Block block) {
		if (level == null || pos == null || block == null) {
			return null;
		}
		WorldEntries entries = PENDING.get(level);
		if (entries == null) {
			return null;
		}
		long now = level.getGameTime();
		entries.cleanupIfDue(now);
		Entry entry = entries.entries.remove(new Key(pos, block));
		if (entries.entries.isEmpty()) {
			PENDING.remove(level);
		}
		if (entry == null || entry.expiresAt < now) {
			return null;
		}
		return level.getServer().getPlayerList().getPlayer(entry.playerId);
	}

	/** 规则关闭或服务器切换时清空全部登记。 */
	public static void clearAll() {
		PENDING.clear();
	}

	private static final class WorldEntries {
		private final Map<Key, Entry> entries = new HashMap<>();
		private long nextCleanupTick = Long.MIN_VALUE;

		private void cleanupIfDue(long now) {
			if (now < nextCleanupTick) {
				return;
			}
			nextCleanupTick = now + CLEANUP_INTERVAL_TICKS;
			Iterator<Map.Entry<Key, Entry>> iterator = entries.entrySet().iterator();
			while (iterator.hasNext()) {
				if (iterator.next().getValue().expiresAt < now) {
					iterator.remove();
				}
			}
		}
	}

	private static final class Entry {
		private final UUID playerId;
		private final long expiresAt;

		private Entry(UUID playerId, long expiresAt) {
			this.playerId = playerId;
			this.expiresAt = expiresAt;
		}
	}

	private static final class Key {
		private final BlockPos pos;
		private final Block block;

		private Key(BlockPos pos, Block block) {
			this.pos = pos.immutable();
			this.block = block;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof Key other)) {
				return false;
			}
			return this.block == other.block && this.pos.equals(other.pos);
		}

		@Override
		public int hashCode() {
			return 31 * this.pos.hashCode() + System.identityHashCode(this.block);
		}
	}
}

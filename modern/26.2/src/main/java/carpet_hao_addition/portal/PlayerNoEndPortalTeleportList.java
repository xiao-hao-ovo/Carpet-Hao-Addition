package carpet_hao_addition.portal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 末地门“不传送玩家”名单(仿 Carpet-AMS-Addition 的 PlayerNoNetherPortalTeleportRegistry)。
 * <p>
 * - globalMode=true(默认):规则开启时所有玩家都不被末地传送门传送;
 * - globalMode=false:仅名单内的玩家不被传送(黑名单语义);
 * - 名单仅保存在内存(与 AMS 一致,服务器重启后清空);
 * - 名单存 UUID→玩家名,便于 /list 显示。
 * <p>
 * 共享层(纯 JDK,不依赖 Minecraft 类型),判定入口 {@link #shouldBlock(UUID)} 供版本层 mixin 使用。
 */
public final class PlayerNoEndPortalTeleportList {
	private PlayerNoEndPortalTeleportList() {
	}

	/**
	 * 全局模式:默认 false(规则开启本身不禁止任何传送);
	 * true=所有玩家都不被末地传送门传送;false=仅名单内玩家不被传送(黑名单)。
	 */
	public static boolean globalMode = false;

	private static final Map<UUID, String> BLOCKED = new LinkedHashMap<>();

	public static Map<UUID, String> entries() {
		return BLOCKED;
	}

	public static boolean contains(UUID uuid) {
		return uuid != null && BLOCKED.containsKey(uuid);
	}

	/** @return 是否成功新增(之前不在名单) */
	public static boolean add(UUID uuid, String name) {
		if (uuid == null) {
			return false;
		}
		String previous = BLOCKED.put(uuid, name);
		return previous == null;
	}

	/** @return 是否成功移除(之前确实在名单) */
	public static boolean remove(UUID uuid) {
		return uuid != null && BLOCKED.remove(uuid) != null;
	}

	/** @return 清空的人数 */
	public static int clear() {
		int size = BLOCKED.size();
		BLOCKED.clear();
		return size;
	}

	/**
	 * 判定某玩家当前是否应被禁止传送(供 EndPortalBlock mixin 调用):
	 * 全局模式下全部禁止;否则仅名单内玩家禁止。
	 */
	public static boolean shouldBlock(UUID uuid) {
		if (globalMode) {
			return true;
		}
		return contains(uuid);
	}
}

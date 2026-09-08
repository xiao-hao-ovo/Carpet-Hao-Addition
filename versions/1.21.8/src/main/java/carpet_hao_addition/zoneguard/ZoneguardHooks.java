package carpet_hao_addition.zoneguard;

import carpet_hao_addition.zoneguard.region.DetectorRegion;
import carpet_hao_addition.zoneguard.region.RegionObserverRefresh;
import carpet_hao_addition.zoneguard.region.ZoneguardSavedData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 版本层钩子:共享层(CarpetHaoAdditionExtension 的规则观察者)通过它调用
 * 依赖 Minecraft 版本 API 的逻辑,保持共享层不直接引用版本层实现。
 */
public final class ZoneguardHooks {
	private ZoneguardHooks() {
	}

	/**
	 * 对存档内所有已配置区域执行"面对面侦测器对"重启。
	 * 用于规则由开启切换为关闭时,恢复被禁用期间卡住的侦测器电路。
	 *
	 * @return 成功重启的侦测器对数
	 */
	public static int refreshAllRegions(MinecraftServer server) {
		ZoneguardSavedData data = ZoneguardSavedData.get(server);
		int refreshed = 0;
		for (DetectorRegion region : data.regions().values()) {
			ServerWorld world = server.getWorld(region.dimension());
			if (world == null) {
				continue;
			}
			refreshed += RegionObserverRefresh.startLoadedFaceToFacePairs(world, region);
		}
		return refreshed;
	}

	/**
	 * 向所有在线玩家重新下发命令树。
	 * <p>
	 * 客户端登录时缓存命令树,之后 requires 不再满足的节点(例如 zoneguard
	 * 规则被关闭)不会自动从补全里消失;重推后按玩家权限过滤,未开启规则时
	 * /zoneguard 会立即从客户端补全中移除(开启时则立即出现)。
	 */
	public static void refreshCommandTree(MinecraftServer server) {
		CommandManager commandManager = server.getCommandManager();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			commandManager.sendCommandTree(player);
		}
	}
}

package carpet_hao_addition.zoneguard;

import carpet_hao_addition.zoneguard.region.DetectorRegion;
import carpet_hao_addition.zoneguard.region.RegionObserverRefresh;
import carpet_hao_addition.zoneguard.region.ZoneguardSavedData;

import net.minecraft.server.MinecraftServer;
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
}

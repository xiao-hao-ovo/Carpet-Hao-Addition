package carpet_hao_addition.zoneguard.region;

import carpet_hao_addition.zoneguard.ZoneguardSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 服务端判定入口:某位置的侦测器是否应被禁用。
 * <p>
 * 规则门控放在这里(而非每个 mixin 注入点),与参考实现的判定点一致;
 * 规则默认关闭,关闭时直接返回 false,保持原版行为。
 */
public final class ZoneguardState {
	private ZoneguardState() {
	}

	public static boolean disablesObserver(Level world, BlockPos pos) {
		if (world.isClientSide() || world.getServer() == null) {
			return false;
		}

		// Rule gate: /carpet zoneguard true|false (authoritative Carpet rule value)
		if (!ZoneguardSettings.isEnabled()) {
			return false;
		}

		return ZoneguardSavedData.get(world.getServer()).disablesObserver(world, pos);
	}
}

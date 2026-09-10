package carpet_hao_addition;

import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * snowyCalcite 规则的共享判定(版本层实现,依赖 Minecraft 类型)。
 * <p>
 * “雪地群系”判定:该位置的生物群系降水类型为 {@link Biome.Precipitation#SNOW}
 * (即该处会下雪/降水为雪的区域,与玩家直觉的雪地一致)。
 */
public final class SnowyCalciteHooks {
	private SnowyCalciteHooks() {
	}

	public static boolean shouldReplace(Level world, BlockPos pos) {
		if (!SnowyCalciteSettings.isEnabled()) {
			return false;
		}

		Holder<Biome> biome = world.getBiome(pos);
		return biome.value().getPrecipitationAt(pos, world.getSeaLevel()) == Biome.Precipitation.SNOW;
	}
}

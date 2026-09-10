package carpet_hao_addition;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * snowyCalcite 规则的共享判定(版本层实现,依赖 Minecraft 类型)。
 * <p>
 * “雪地群系”判定:该位置的生物群系降水类型为 {@link Biome.Precipitation#SNOW}
 * (即该处会下雪/降水为雪的区域,与玩家直觉的雪地一致)。
 */
public final class SnowyCalciteHooks {
	private SnowyCalciteHooks() {
	}

	public static boolean shouldReplace(World world, BlockPos pos) {
		if (!SnowyCalciteSettings.isEnabled()) {
			return false;
		}

		RegistryEntry<Biome> biome = world.getBiomeAccess().getBiome(pos);
		return biome.value().getPrecipitation(pos, world.getSeaLevel()) == Biome.Precipitation.SNOW;
	}
}

package carpet_hao_addition.zoneguard.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 一个立方的侦测器禁用区域(维度 + 最小/最大角)。
 * <p>
 * 版本相关代码(依赖 Minecraft 类型与 codec),位于 versions 层。
 * 语义与参考实现(MC 26.2 zoneguard)等价:contains/volume/fromCorners 行为一致。
 */
public record DetectorRegion(ResourceKey<Level> dimension, BlockPos min, BlockPos max) {
	public static final Codec<DetectorRegion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(DetectorRegion::dimension),
			BlockPos.CODEC.fieldOf("min").forGetter(DetectorRegion::min),
			BlockPos.CODEC.fieldOf("max").forGetter(DetectorRegion::max)
	).apply(instance, DetectorRegion::new));

	// 1.21.8 yarn 的 BlockPos 是不可变值对象,record 组件无需额外拷贝。
	// (参考实现的 immutable() 调用在 yarn 1.21.8 中不存在,语义上等效。)

	public static DetectorRegion fromCorners(ResourceKey<Level> dimension, BlockPos first, BlockPos second) {
		return new DetectorRegion(dimension, BlockPos.min(first, second), BlockPos.max(first, second));
	}

	public boolean contains(ResourceKey<Level> levelDimension, BlockPos pos) {
		return this.dimension.equals(levelDimension)
				&& pos.getX() >= this.min.getX()
				&& pos.getX() <= this.max.getX()
				&& pos.getY() >= this.min.getY()
				&& pos.getY() <= this.max.getY()
				&& pos.getZ() >= this.min.getZ()
				&& pos.getZ() <= this.max.getZ();
	}

	public long volume() {
		return (long) (this.max.getX() - this.min.getX() + 1)
				* (this.max.getY() - this.min.getY() + 1)
				* (this.max.getZ() - this.min.getZ() + 1);
	}
}

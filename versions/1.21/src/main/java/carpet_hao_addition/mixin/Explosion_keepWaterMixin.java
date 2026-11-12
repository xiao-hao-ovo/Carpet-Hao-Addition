package carpet_hao_addition.mixin;

import carpet_hao_addition.EnhancedWorldEaterSettings;
import carpet_hao_addition.WorldEaterProMaxSettings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * ProMax 单独开启时保留含水方块里的水：爆炸把方块炸掉后，把原本含水的格重新放回水源。
 *
 * <p>1.21 / 1.21.1 没有 {@code ExplosionImpl}，爆炸实现就是 {@code Explosion} 本身；
 * 语义与 1.21.8 的 {@code ExplosionImpl_keepWaterMixin} 一致：只有 ProMax 开、
 * enhancedWorldEater 关才保留水。</p>
 */
@Mixin(Explosion.class)
public abstract class Explosion_keepWaterMixin {
	@Shadow
	private World world;

	@Shadow
	private ObjectArrayList<BlockPos> affectedBlocks;

	@Unique
	private final ThreadLocal<List<BlockPos>> hao$waterloggedStack = ThreadLocal.withInitial(ArrayList::new);

	@Inject(method = "collectBlocksAndDamageEntities", at = @At("HEAD"))
	private void hao$recordWaterlogged(CallbackInfo ci) {
		List<BlockPos> saved = new ArrayList<>();
		for (BlockPos pos : affectedBlocks) {
			BlockState state = world.getBlockState(pos);
			boolean water = state.isOf(Blocks.WATER)
					|| (state.contains(Properties.WATERLOGGED)
					&& state.get(Properties.WATERLOGGED));
			if (water) {
				saved.add(pos.toImmutable());
			}
		}
		hao$waterloggedStack.set(saved);
	}

	@Inject(method = "collectBlocksAndDamageEntities", at = @At("RETURN"))
	private void hao$restoreWater(CallbackInfo ci) {
		if (!WorldEaterProMaxSettings.isEnabled() || EnhancedWorldEaterSettings.isEnabled()) {
			hao$waterloggedStack.remove();
			return;
		}
		for (BlockPos pos : hao$waterloggedStack.get()) {
			if (world.getBlockState(pos).isAir()) {
				world.setBlockState(pos, Blocks.WATER.getDefaultState());
			}
		}
		hao$waterloggedStack.remove();
	}
}
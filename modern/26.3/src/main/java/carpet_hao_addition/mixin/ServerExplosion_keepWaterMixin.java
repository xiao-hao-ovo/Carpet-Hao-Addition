package carpet_hao_addition.mixin;

import carpet_hao_addition.EnhancedWorldEaterSettings;
import carpet_hao_addition.WorldEaterProMaxSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
 * <p>只有「ProMax 开、enhancedWorldEater 关」才保留；两条同开时水随方块一起消失，
 * 与 1.21.8 的 {@code ExplosionImpl_keepWaterMixin} 语义一致。</p>
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosion_keepWaterMixin {
	@Shadow
	private ServerLevel level;

	@Unique
	private final ThreadLocal<List<BlockPos>> hao$waterloggedStack = ThreadLocal.withInitial(ArrayList::new);

	@Inject(method = "interactWithBlocks", at = @At("HEAD"))
	private void hao$recordWaterlogged(List<BlockPos> positions, CallbackInfo ci) {
		List<BlockPos> saved = new ArrayList<>();
		for (BlockPos pos : positions) {
			BlockState state = level.getBlockState(pos);
			boolean water = state.is(Blocks.WATER)
					|| (state.hasProperty(BlockStateProperties.WATERLOGGED)
					&& state.getValue(BlockStateProperties.WATERLOGGED));
			if (water) {
				saved.add(pos.immutable());
			}
		}
		hao$waterloggedStack.set(saved);
	}

	@Inject(method = "interactWithBlocks", at = @At("RETURN"))
	private void hao$restoreWater(List<BlockPos> positions, CallbackInfo ci) {
		if (!WorldEaterProMaxSettings.isEnabled() || EnhancedWorldEaterSettings.isEnabled()) {
			hao$waterloggedStack.remove();
			return;
		}
		for (BlockPos pos : hao$waterloggedStack.get()) {
			if (level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
			}
		}
		hao$waterloggedStack.remove();
	}
}
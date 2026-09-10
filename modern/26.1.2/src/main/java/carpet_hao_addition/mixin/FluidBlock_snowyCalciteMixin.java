package carpet_hao_addition.mixin;

import carpet_hao_addition.SnowyCalciteHooks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * snowyCalcite 规则:流体方块接收邻居流体并“生成圆石/石头”的生成点
 * (LiquidBlock.receiveNeighborFluids 内的两处 getDefaultState 调用;仿 fabric-carpet
 * renewableDeepslate 的 LiquidBlock 注入)。雪地生物群系且规则开启时替换为方解石。
 */
@Mixin(LiquidBlock.class)
public abstract class FluidBlock_snowyCalciteMixin {
	@Inject(method = "shouldSpreadLiquid", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;",
			ordinal = 0), cancellable = true)
	private void hao$snowyCalciteReplaceFirst(Level world, BlockPos pos, BlockState state,
			CallbackInfoReturnable<Boolean> cir) {
		replaceWithCalcite(world, pos, cir);
	}

	@Inject(method = "shouldSpreadLiquid", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;",
			ordinal = 1), cancellable = true)
	private void hao$snowyCalciteReplaceSecond(Level world, BlockPos pos, BlockState state,
			CallbackInfoReturnable<Boolean> cir) {
		replaceWithCalcite(world, pos, cir);
	}

	private static void replaceWithCalcite(Level world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!SnowyCalciteHooks.shouldReplace(world, pos)) {
			return;
		}

		world.setBlockAndUpdate(pos, Blocks.CALCITE.defaultBlockState());
		world.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
		cir.setReturnValue(false);
	}
}

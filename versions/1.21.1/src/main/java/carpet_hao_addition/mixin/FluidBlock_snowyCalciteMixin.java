package carpet_hao_addition.mixin;

import carpet_hao_addition.SnowyCalciteHooks;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * snowyCalcite 规则:流体方块接收邻居流体并“生成圆石/石头”的生成点
 * (FluidBlock.receiveNeighborFluids 内的两处 getDefaultState 调用;仿 fabric-carpet
 * renewableDeepslate 的 LiquidBlock 注入)。雪地生物群系且规则开启时替换为方解石。
 */
@Mixin(FluidBlock.class)
public abstract class FluidBlock_snowyCalciteMixin {
	@Inject(method = "receiveNeighborFluids", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/Block;getDefaultState()Lnet/minecraft/block/BlockState;",
			ordinal = 0), cancellable = true)
	private void hao$snowyCalciteReplaceFirst(World world, BlockPos pos, BlockState state,
			CallbackInfoReturnable<Boolean> cir) {
		replaceWithCalcite(world, pos, cir);
	}

	@Inject(method = "receiveNeighborFluids", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/Block;getDefaultState()Lnet/minecraft/block/BlockState;",
			ordinal = 1), cancellable = true)
	private void hao$snowyCalciteReplaceSecond(World world, BlockPos pos, BlockState state,
			CallbackInfoReturnable<Boolean> cir) {
		replaceWithCalcite(world, pos, cir);
	}

	private static void replaceWithCalcite(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!SnowyCalciteHooks.shouldReplace(world, pos)) {
			return;
		}

		world.setBlockState(pos, Blocks.CALCITE.getDefaultState());
		world.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, pos, 0);
		cir.setReturnValue(false);
	}
}

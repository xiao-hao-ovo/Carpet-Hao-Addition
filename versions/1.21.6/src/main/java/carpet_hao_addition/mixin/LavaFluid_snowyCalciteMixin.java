package carpet_hao_addition.mixin;

import carpet_hao_addition.SnowyCalciteHooks;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * snowyCalcite 规则:岩浆流动中“遇水生成石头”的生成点(对应 yarn 的
 * LavaFluid.flow;仿 fabric-carpet renewableDeepslate 的 LavaFluid 注入)。
 * 在雪地生物群系且规则开启时,把即将生成的默认方块替换为方解石并播放熄灭事件。
 */
@Mixin(LavaFluid.class)
public abstract class LavaFluid_snowyCalciteMixin {
	@Inject(method = "flow", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/Block;getDefaultState()Lnet/minecraft/block/BlockState;"),
			cancellable = true)
	private void hao$snowyCalciteReplace(WorldAccess worldAccess, BlockPos pos, BlockState state,
			Direction direction, FluidState fluidState, CallbackInfo ci) {
		if (!(worldAccess instanceof World world)) {
			return;
		}
		if (!SnowyCalciteHooks.shouldReplace(world, pos)) {
			return;
		}

		world.setBlockState(pos, Blocks.CALCITE.getDefaultState(), 3);
		world.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, pos, 0);
		ci.cancel();
	}
}

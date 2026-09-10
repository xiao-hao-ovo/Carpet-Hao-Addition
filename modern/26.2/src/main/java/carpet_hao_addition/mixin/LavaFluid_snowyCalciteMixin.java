package carpet_hao_addition.mixin;

import carpet_hao_addition.SnowyCalciteHooks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LevelEvent;

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
	@Inject(method = "spreadTo", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"),
			cancellable = true)
	private void hao$snowyCalciteReplace(LevelAccessor worldAccess, BlockPos pos, BlockState state,
			Direction direction, FluidState fluidState, CallbackInfo ci) {
		if (!(worldAccess instanceof Level world)) {
			return;
		}
		if (!SnowyCalciteHooks.shouldReplace(world, pos)) {
			return;
		}

		world.setBlock(pos, Blocks.CALCITE.defaultBlockState(), 3);
		world.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
		ci.cancel();
	}
}

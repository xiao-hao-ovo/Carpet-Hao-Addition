package carpet_hao_addition.mixin;

import carpet_hao_addition.zoneguard.region.ZoneguardState;

import net.minecraft.block.BlockState;
import net.minecraft.block.ObserverBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 zoneguard 规则开启且方块位于已配置区域时,取消侦测器(观察者)行为。
 * <p>
 * 注入点按 1.21.8 yarn 的 {@link ObserverBlock} 实际方法逐一适配:
 * 参考实现(MC 26.2)的 tick/updateShape/updateNeighborsInFront/ownSignal/
 * getSignal/getDirectSignal/onPlace/affectNeighborsAfterRemoval 对应 yarn 的
 * scheduledTick/getStateForNeighborUpdate/updateNeighbors/getWeakRedstonePower/
 * getStrongRedstonePower/onBlockAdded/onStateReplaced;26.2 新增的 ownSignal
 * 在 1.21.8 不存在,其职责已由强弱红石信号两个注入点覆盖,故功能等价剔除。
 */
@Mixin(ObserverBlock.class)
public class ObserverBlockMixin {
	@Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelScheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
		if (ZoneguardState.disablesObserver(world, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "getStateForNeighborUpdate", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelStateForNeighborUpdate(BlockState state, WorldView worldView, ScheduledTickView scheduledTickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random, CallbackInfoReturnable<BlockState> cir) {
		if (worldView instanceof World world && ZoneguardState.disablesObserver(world, pos)) {
			cir.setReturnValue(state);
		}
	}

	@Inject(method = "updateNeighbors", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelUpdateNeighbors(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (ZoneguardState.disablesObserver(world, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "getWeakRedstonePower", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelWeakRedstonePower(BlockState state, BlockView blockView, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (blockView instanceof World world && ZoneguardState.disablesObserver(world, pos)) {
			cir.setReturnValue(0);
		}
	}

	@Inject(method = "getStrongRedstonePower", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelStrongRedstonePower(BlockState state, BlockView blockView, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (blockView instanceof World world && ZoneguardState.disablesObserver(world, pos)) {
			cir.setReturnValue(0);
		}
	}

	@Inject(method = "onBlockAdded", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelOnBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved, CallbackInfo ci) {
		if (ZoneguardState.disablesObserver(world, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "onStateReplaced", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelOnStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved, CallbackInfo ci) {
		if (ZoneguardState.disablesObserver(world, pos)) {
			ci.cancel();
		}
	}
}

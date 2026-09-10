package carpet_hao_addition.mixin;

import carpet_hao_addition.zoneguard.region.ZoneguardState;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;

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
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelScheduledTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (ZoneguardState.disablesObserver(world, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelStateForNeighborUpdate(BlockState state, LevelReader worldView, ScheduledTickAccess scheduledTickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
		if (worldView instanceof Level world && ZoneguardState.disablesObserver(world, pos)) {
			cir.setReturnValue(state);
		}
	}

	@Inject(method = "updateNeighborsInFront", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelUpdateNeighbors(Level world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (ZoneguardState.disablesObserver(world, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelWeakRedstonePower(BlockState state, BlockGetter blockView, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (blockView instanceof Level world && ZoneguardState.disablesObserver(world, pos)) {
			cir.setReturnValue(0);
		}
	}

	@Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelStrongRedstonePower(BlockState state, BlockGetter blockView, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (blockView instanceof Level world && ZoneguardState.disablesObserver(world, pos)) {
			cir.setReturnValue(0);
		}
	}

	@Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelOnBlockAdded(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved, CallbackInfo ci) {
		if (ZoneguardState.disablesObserver(world, pos)) {
			ci.cancel();
		}
	}

	@Inject(method = "affectNeighborsAfterRemoval", at = @At("HEAD"), cancellable = true)
	private void zoneguard$cancelOnStateReplaced(BlockState state, ServerLevel world, BlockPos pos, boolean moved, CallbackInfo ci) {
		if (ZoneguardState.disablesObserver(world, pos)) {
			ci.cancel();
		}
	}
}

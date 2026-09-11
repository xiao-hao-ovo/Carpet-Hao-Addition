package carpet_hao_addition.mixin;

import carpet_hao_addition.directdrops.DirectDropContext;
import carpet_hao_addition.directdrops.PendingDirectDropTicks;

import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.TickPriority;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * directBlockDrops(跨 tick 部分):登记方块 tick 的掉落归属。
 * <p>
 * 玩家破坏方块时,原版会为受影响的方块(含失去支撑的相邻方块)排队一个方块 tick,这些排队
 * 调用都发生在玩家操作的同一调用栈内 —— 此时 {@link DirectDropContext} 上仍有玩家,于是把
 * 「坐标 + 方块 → 玩家」记进 {@link PendingDirectDropTicks},供后续 tick 取用。
 * <p>
 * 世界直接取自 mixin 实例本身(被调度的就是它),不依赖任何随版本改名的取世界方法。
 * <p>
 * 目标类 1.21.2 起为 {@code ScheduledTickView};1.21 / 1.21.1 上该方法是
 * {@code WorldAccess.scheduleBlockTick},各版本层使用不同的 mixin 类名。
 */
@Mixin(WorldAccess.class)
public interface WorldAccess_directDropsMixin {
	@Inject(
			method = "scheduleBlockTick(" +
					"Lnet/minecraft/util/math/BlockPos;" +
					"Lnet/minecraft/block/Block;" +
					"I" +
					")V",
			at = @At("HEAD")
	)
	private void hao$recordScheduledBlockTick(BlockPos pos, Block block, int delay, CallbackInfo ci) {
		hao$attribute(pos, block, delay);
	}

	@Inject(
			method = "scheduleBlockTick(" +
					"Lnet/minecraft/util/math/BlockPos;" +
					"Lnet/minecraft/block/Block;" +
					"I" +
					"Lnet/minecraft/world/tick/TickPriority;" +
					")V",
			at = @At("HEAD")
	)
	private void hao$recordScheduledBlockTickWithPriority(BlockPos pos, Block block, int delay, TickPriority priority, CallbackInfo ci) {
		hao$attribute(pos, block, delay);
	}

	private void hao$attribute(BlockPos pos, Block block, int delay) {
		ServerPlayerEntity player = DirectDropContext.currentPlayer();
		if (player == null || !((Object) this instanceof ServerWorld world)) {
			return;
		}
		PendingDirectDropTicks.record(world, pos, block, delay, player);
	}
}

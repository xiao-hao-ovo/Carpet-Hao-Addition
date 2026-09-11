package carpet_hao_addition.mixin;

import carpet_hao_addition.directdrops.DirectDropContext;
import carpet_hao_addition.directdrops.PendingDirectDropTicks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.ticks.TickPriority;

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
 */
@Mixin(ScheduledTickAccess.class)
public interface ScheduledTickAccess_directDropsMixin {
	@Inject(
			method = "scheduleTick(" +
					"Lnet/minecraft/core/BlockPos;" +
					"Lnet/minecraft/world/level/block/Block;" +
					"I" +
					")V",
			at = @At("HEAD")
	)
	private void hao$recordScheduledTick(BlockPos pos, Block block, int delay, CallbackInfo ci) {
		hao$attribute(pos, block, delay);
	}

	@Inject(
			method = "scheduleTick(" +
					"Lnet/minecraft/core/BlockPos;" +
					"Lnet/minecraft/world/level/block/Block;" +
					"I" +
					"Lnet/minecraft/world/ticks/TickPriority;" +
					")V",
			at = @At("HEAD")
	)
	private void hao$recordScheduledTickWithPriority(BlockPos pos, Block block, int delay, TickPriority priority, CallbackInfo ci) {
		hao$attribute(pos, block, delay);
	}

	private void hao$attribute(BlockPos pos, Block block, int delay) {
		ServerPlayer player = DirectDropContext.currentPlayer();
		if (player == null || !((Object) this instanceof ServerLevel level)) {
			return;
		}
		PendingDirectDropTicks.record(level, pos, block, delay, player);
	}
}

package carpet_hao_addition.mixin;

import carpet_hao_addition.directdrops.DirectDropContext;
import carpet_hao_addition.directdrops.PendingDirectDropTicks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * directBlockDrops(跨 tick 部分):执行被赋予归属的方块 tick。
 * <p>
 * 在 {@code ServerLevel.tickBlock} 的 HEAD 取出归属并压入 {@link DirectDropContext},
 * 整个 tick 期间产生的掉落都归到当初破坏方块的玩家名下,RETURN 时弹栈。
 * <p>
 * 之所以不需要再校验「销毁的是不是这个坐标」:一次方块 tick 只会处理一个方块,它同步引发的
 * 连锁销毁(竹子、仙人掌、脚手架失去支撑等)本就属于同一次玩家操作。
 * <p>
 * HEAD 无条件压栈(没有归属时压 null),保证与 RETURN 的弹栈严格配对。
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevel_tickBlockMixin {
	@Inject(method = "tickBlock", at = @At("HEAD"))
	private void hao$beginAttributedBlockTick(BlockPos pos, Block block, CallbackInfo ci) {
		ServerLevel level = (ServerLevel) (Object) this;
		ServerPlayer player = PendingDirectDropTicks.consume(level, pos, block);
		DirectDropContext.push(player);
	}

	// 无条件弹栈:与 HEAD 严格配对,避免上下文泄漏到下一个方块 tick。
	@Inject(method = "tickBlock", at = @At("RETURN"))
	private void hao$endAttributedBlockTick(BlockPos pos, Block block, CallbackInfo ci) {
		DirectDropContext.pop();
	}
}

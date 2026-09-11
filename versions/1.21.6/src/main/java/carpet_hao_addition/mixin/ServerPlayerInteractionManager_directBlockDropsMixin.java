package carpet_hao_addition.mixin;

import carpet_hao_addition.DirectDropsSettings;
import carpet_hao_addition.directdrops.DirectDropContext;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * directBlockDrops:在玩家破坏方块期间标记掉落归属。
 * <p>
 * 只在 {@code tryBreakBlock} 的调用栈内标记,所以只有这次操作同步产生的掉落(方块本体、
 * 连锁破坏、失去支撑的相邻方块、被破坏容器内释放的物品)会进背包;世界后续 tick 里
 * 独立产生的掉落不受影响。
 * <p>
 * HEAD 压栈 / RETURN 无条件弹栈,保证异常与提前返回时上下文都能正确恢复到外层。
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManager_directBlockDropsMixin {
	@Shadow
	protected ServerPlayerEntity player;

	@Inject(method = "tryBreakBlock", at = @At("HEAD"))
	private void hao$beginDirectBlockDrops(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		DirectDropContext.push(DirectDropsSettings.blockDropsEnabled() ? this.player : null);
	}

	// 无条件弹栈:HEAD 压入的内容必须弹掉,即使规则在调用中途被改。
	@Inject(method = "tryBreakBlock", at = @At("RETURN"))
	private void hao$endDirectBlockDrops(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		DirectDropContext.pop();
	}
}

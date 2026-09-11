package carpet_hao_addition.mixin;

import carpet_hao_addition.directdrops.DirectDropContext;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 两条规则的共同落点:拦截世界生成实体,把「有归属」的掉落物直接塞进玩家背包。
 * <p>
 * 所有掉落物最终都以 {@link ItemEntity} 的形式经这里进入世界,所以只需在入口处判断归属,
 * 方块掉落与实体掉落两条规则共用同一段逻辑。
 * <p>
 * 关键细节:{@code insertStack} 会<b>就地修改</b>传入的 ItemStack,只有它被完全装下
 * (变空)时才取消世界生成;装不下的部分保留在 stack 上,继续按原版掉落在地上。
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorld_directDropsMixin {
	@Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
	private void hao$redirectDirectDrop(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		ServerPlayerEntity player = DirectDropContext.currentPlayer();
		if (player == null || !(entity instanceof ItemEntity)) {
			return;
		}
		ItemStack stack = ((ItemEntity) entity).getStack();
		if (stack.isEmpty()) {
			return;
		}
		player.getInventory().insertStack(stack);
		if (stack.isEmpty()) {
			cir.setReturnValue(true);
		}
	}
}

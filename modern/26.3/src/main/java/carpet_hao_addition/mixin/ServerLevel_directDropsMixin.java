package carpet_hao_addition.mixin;

import carpet_hao_addition.directdrops.DirectDropContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

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
 * 关键细节:{@code Inventory.add} 会<b>就地修改</b>传入的 ItemStack,只有它被完全装下
 * (变空)时才取消世界生成;装不下的部分保留在 stack 上,继续按原版掉落在地上。
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevel_directDropsMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void hao$redirectDirectDrop(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		ServerPlayer player = DirectDropContext.currentPlayer();
		if (player == null || !(entity instanceof ItemEntity itemEntity)) {
			return;
		}
		ItemStack stack = itemEntity.getItem();
		if (stack.isEmpty()) {
			return;
		}
		player.getInventory().add(stack);
		if (stack.isEmpty()) {
			cir.setReturnValue(true);
		}
	}
}

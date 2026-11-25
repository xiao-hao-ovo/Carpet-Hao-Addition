package carpet_hao_addition.mixin.pickup;

import carpet_hao_addition.pickup.PickupFilter;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 兜底：掉落物「直接进背包」时也要受黑白名单约束。
 * <p>
 * carpet 的 {@code blockDropsDirectlyEnterInventory} 会在 {@code ServerWorld#spawnEntity} 里调用
 * {@code player.getInventory().insertStack(itemEntity.getStack())}，这里在 insertStack 被调用时
 * 判断这个 ItemStack 是否就是刚被加进世界的那个掉落物、并且被名单拦下。
 * 返回 false 时堆栈不会被清空，物品随后照常作为掉落物留在地上。
 * <p>
 * 另一条路径是方块掉落表（见 {@link Block_pickupFilterDropsMixin}）：
 * 那里没有物品实体可追踪，只能按 ItemStack 对象身份认出来。
 */
@Mixin(PlayerInventory.class)
public abstract class PlayerInventory_pickupFilterMixin {
	@Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void hao$filterDirectInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (PickupFilter.blocksDirectInsert(stack)) {
			cir.setReturnValue(false);
		}
	}
}
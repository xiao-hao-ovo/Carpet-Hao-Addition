package carpet_hao_addition.mixin.pickup;

import carpet_hao_addition.pickup.PickupFilter;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 普通拾取（走过去踩到掉落物）。
 * <p>
 * {@code ItemEntity#onPlayerCollision} 一进来就拦掉，后面的 {@code insertStack}、
 * 堆栈扣减、拾取统计和 {@code onItemPickup} 都不会执行，物品继续躺在地上。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntity_pickupFilterMixin {
	@Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
	private void hao$filterPickup(PlayerEntity player, CallbackInfo ci) {
		// 拾取只在服务端发生；用 instanceof 而不是 getWorld().isClient()，
		// 因为 1.21.6+ 的 Yarn 把 Entity#getWorld() 改名了，跨版本写法在旧层编不过。
		if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity)) {
			return;
		}
		ItemEntity self = (ItemEntity) (Object) this;
		if (PickupFilter.denies(self.getStack())) {
			ci.cancel();
		}
	}
}
package carpet_hao_addition.mixin.pickup;

import carpet_hao_addition.pickup.PickupFilter;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 普通拾取（走过去踩到掉落物）。
 * <p>
 * {@code ItemEntity#playerTouch} 一进来就拦掉，后面的 {@code Inventory#add}、
 * 堆栈扣减、拾取统计和 {@code onItemPickup} 都不会执行，物品继续躺在地上。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntity_pickupFilterMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void hao$filterPickup(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) {
            return;
        }
        ItemEntity self = (ItemEntity) (Object) this;
        if (PickupFilter.denies(self.getItem())) {
            ci.cancel();
        }
    }
}
package carpet_hao_addition.mixin.pickup;

import carpet_hao_addition.pickup.PickupFilter;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 标记「正在往世界里加一个物品实体」。
 * <p>
 * carpet 的 {@code blockDropsDirectlyEnterInventory} 在 {@code ServerWorld#spawnEntity}
 * 调 {@code addNewEntity} 的那一刻直接把掉落物塞进玩家背包，此时还没有任何拾取事件。
 * 这里把当前实体记到 ThreadLocal，{@code PlayerInventory#insertStack} 才能认出
 * 这一次入包是哪个掉落物。
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorld_pickupFilterMixin {
	@Inject(method = "spawnEntity", at = @At("HEAD"))
	private void hao$enterItemAdd(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof ItemEntity item) {
			PickupFilter.enterAddEntity(item);
		}
	}

	@Inject(method = "spawnEntity", at = @At("RETURN"))
	private void hao$exitItemAdd(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof ItemEntity) {
			PickupFilter.exitAddEntity();
		}
	}
}
package carpet_hao_addition.mixin;

import carpet_hao_addition.WitherSkeletonDropReductionSettings;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * witherSkeletonDropReduction:去除凋零骷髅死亡时掉落的手持石剑装备。
 * <p>
 * 装备掉落位于 MobEntity.dropEquipment,其逐槽调用 getEquippedStack;用 @WrapOperation
 * (而非 @Redirect,兼容旧 mixinextras 0.5.4)在返回石剑时替换为空栈。
 */
@Mixin(MobEntity.class)
public abstract class MobEntity_witherSkeletonDropMixin {
	@WrapOperation(method = "dropEquipment",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/entity/mob/MobEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"))
	private ItemStack hao$removeWitherSkeletonSword(MobEntity self, EquipmentSlot slot, Operation<ItemStack> original) {
		ItemStack stack = original.call(self, slot);
		if (self instanceof WitherSkeletonEntity && WitherSkeletonDropReductionSettings.remove("sword")
				&& stack.isOf(Items.STONE_SWORD)) {
			return ItemStack.EMPTY;
		}
		return stack;
	}
}

package carpet_hao_addition.mixin;

import carpet_hao_addition.WitherSkeletonDropReductionSettings;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * witherSkeletonDropReduction:去除凋零骷髅死亡时掉落的手持石剑装备。
 * <p>
 * 装备掉落位于 MobEntity.dropEquipment,其对每个装备槽调用
 * getEquippedStack(EquipmentSlot);命中 sword 选项时把石剑返回为空,使其不掉落。
 */
@Mixin(MobEntity.class)
public abstract class MobEntity_witherSkeletonDropMixin {
	@Redirect(method = "dropEquipment",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/entity/mob/MobEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"))
	private ItemStack hao$removeWitherSkeletonSword(MobEntity self, EquipmentSlot slot) {
		ItemStack stack = self.getEquippedStack(slot);
		if (self instanceof WitherSkeletonEntity && WitherSkeletonDropReductionSettings.remove("sword")
				&& stack.isOf(Items.STONE_SWORD)) {
			return ItemStack.EMPTY;
		}
		return stack;
	}
}

package carpet_hao_addition.mixin;

import carpet_hao_addition.WitherSkeletonDropReductionSettings;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * witherSkeletonDropReduction:去除凋零骷髅死亡时掉落的手持石剑装备。
 * <p>
 * 26.2 的装备掉落不再逐槽调用 getItemBySlot,故改为在 Mob.dropCustomDeathLoot(装备掉落入口)
 * 的 HEAD 处:命中 sword 选项时先把主手石剑清空,使其不参与后续掉落计算。
 */
@Mixin(Mob.class)
public abstract class MobEntity_witherSkeletonDropMixin {
	@Inject(method = "dropCustomDeathLoot", at = @At("HEAD"))
	private void hao$removeWitherSkeletonSword(ServerLevel level, DamageSource source, boolean hitByPlayer,
			CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (!(self instanceof WitherSkeleton) || !WitherSkeletonDropReductionSettings.remove("sword")) {
			return;
		}
		ItemStack mainHand = self.getItemBySlot(EquipmentSlot.MAINHAND);
		if (mainHand.is(Items.STONE_SWORD)) {
			self.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
	}
}

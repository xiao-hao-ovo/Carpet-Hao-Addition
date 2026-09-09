package carpet_hao_addition.mixin;

import carpet_hao_addition.WitherSkeletonDropReductionSettings;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * witherSkeletonDropReduction:过滤凋零骷髅从 loot 表(骨头/煤炭/头颅)掉落的物品。
 * <p>
 * 1.21.8/1.21.10 死亡掉落经 LivingEntity.forEachGeneratedItem,最后把生成物品列表交给
 * List.forEach;用 @WrapOperation 包住该调用(而非 @Redirect,兼容旧 mixinextras 0.5.4),
 * 命中选项时改为只消费保留的物品。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_witherSkeletonDropMixin {
	@WrapOperation(method = "forEachGeneratedItem",
			at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
	private void hao$filterWitherSkeletonLoot(List<ItemStack> stacks, Consumer<ItemStack> consumer,
			Operation<Void> original) {
		if (!((Object) this instanceof WitherSkeletonEntity)
				|| WitherSkeletonDropReductionSettings.value().equals("false")) {
			original.call(stacks, consumer);
			return;
		}

		List<ItemStack> kept = new ArrayList<>(stacks.size());
		for (ItemStack stack : stacks) {
			if (!shouldRemove(stack.getItem())) {
				kept.add(stack);
			}
		}
		kept.forEach(consumer);
	}

	private static boolean shouldRemove(Item item) {
		if (WitherSkeletonDropReductionSettings.remove("bone") && item == Items.BONE) {
			return true;
		}
		if (WitherSkeletonDropReductionSettings.remove("coal") && item == Items.COAL) {
			return true;
		}
		return WitherSkeletonDropReductionSettings.remove("skull") && item == Items.WITHER_SKELETON_SKULL;
	}
}

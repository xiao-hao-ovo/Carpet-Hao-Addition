package carpet_hao_addition.mixin;

import carpet_hao_addition.WitherSkeletonDropReductionSettings;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * witherSkeletonDropReduction:过滤凋零骷髅从 loot 表(骨头/煤炭/头颅)掉落的物品。
 * <p>
 * 1.21.8/1.21.10 的死亡掉落经 LivingEntity.forEachGeneratedItem,最终把生成的
 * 物品列表交给 List.forEach;在凋零骷髅且规则命中时,先滤掉指定物品再交给原 consumer。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_witherSkeletonDropMixin {
	@Redirect(method = "forEachGeneratedItem",
			at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
	private void hao$filterWitherSkeletonLoot(List<ItemStack> stacks, Consumer<ItemStack> consumer) {
		if (!((Object) this instanceof WitherSkeletonEntity)
				|| (WitherSkeletonDropReductionSettings.value().equals("false"))) {
			stacks.forEach(consumer);
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

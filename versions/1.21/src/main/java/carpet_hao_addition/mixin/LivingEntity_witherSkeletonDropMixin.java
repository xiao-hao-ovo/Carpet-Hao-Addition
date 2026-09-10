package carpet_hao_addition.mixin;

import carpet_hao_addition.WitherSkeletonDropReductionSettings;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

/**
 * witherSkeletonDropReduction:过滤凋零骷髅死亡掉落(loot 表:骨头/煤炭/头颅)。
 * <p>
 * 1.21.8/1.21.10 的实体死亡掉落由 LivingEntity.dropLoot 调
 * LootTable.generateLoot(context, seed, consumer) 完成;用 @ModifyArg 把该 consumer
 * 包一层(vanilla 注入,不依赖 mixinextras),命中选项时丢弃指定物品。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_witherSkeletonDropMixin {
	@ModifyArg(method = "dropLoot",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/loot/LootTable;generateLoot(Lnet/minecraft/loot/context/LootContextParameterSet;JLjava/util/function/Consumer;)V"),
			index = 2)
	private Consumer<ItemStack> hao$filterWitherSkeletonLoot(Consumer<ItemStack> consumer) {
		if (!((Object) this instanceof WitherSkeletonEntity)
				|| WitherSkeletonDropReductionSettings.value().equals("false")) {
			return consumer;
		}

		return stack -> {
			if (!shouldRemove(stack.getItem())) {
				consumer.accept(stack);
			}
		};
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

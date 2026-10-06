package carpet_hao_addition.mixin;

import carpet_hao_addition.WitherSkeletonDropReductionSettings;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

/**
 * witherSkeletonDropReduction:过滤凋零骷髅死亡掉落(loot 表:骨头/煤炭/头颅)。
 * <p>
 * 1.21.10 的实体死亡掉落由 dropLoot 委托到 LivingEntity.generateLoot(ServerLevel,
 * DamageSource, boolean, ResourceKey, Consumer),其内部调
 * LootTable.generateLoot(context, seed, consumer);用 @ModifyArg 把该 consumer 包一层
 * (vanilla 注入,不依赖 mixinextras),命中选项时丢弃指定物品。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_witherSkeletonDropMixin {
	@ModifyArg(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"),
			index = 2)
	private Consumer<ItemStack> hao$filterWitherSkeletonLoot(Consumer<ItemStack> consumer) {
		if (!((Object) this instanceof WitherSkeleton)
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

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
 * haoWitherSkeletonDrop:过滤凋零骷髅死亡掉落(loot 表:骨头/煤炭/头颅)。
 * <p>
 * 1.21.8/1.21.10 的实体死亡掉落由 LivingEntity.dropLoot 调
 * LootTable.generateLoot(context, seed, consumer) 完成;用 @ModifyArg 把该 consumer
 * 包一层(vanilla 注入,不依赖 mixinextras),命中选项时丢弃指定物品。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_witherSkeletonDropMixin {
	@ModifyArg(method = "dropLoot",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/loot/LootTable;generateLoot(Lnet/minecraft/loot/context/LootWorldContext;JLjava/util/function/Consumer;)V"),
			index = 2)
	private Consumer<ItemStack> hao$filterWitherSkeletonLoot(Consumer<ItemStack> consumer) {
		// instanceof 放前面:dropLoot 是所有生物死亡都走的路径,这里绝大多数情况不是凋零骷髅。
		if (!((Object) this instanceof WitherSkeletonEntity)) {
			return consumer;
		}
		// 选项只读一次。原来每个掉落物品都要问三遍(removeEntry 内部各查一次 carpet 规则),
		// 而 generateLoot 会对掉落表里每一项都回调这个 consumer。
		String option = WitherSkeletonDropReductionSettings.storedValue();
		if ("false".equals(option)) {
			return consumer;
		}
		boolean all = "all".equals(option);
		boolean noBone = all || "bone".equals(option);
		boolean noCoal = all || "coal".equals(option);
		boolean noSkull = all || "skull".equals(option);

		return stack -> {
			Item item = stack.getItem();
			if ((noBone && item == Items.BONE)
					|| (noCoal && item == Items.COAL)
					|| (noSkull && item == Items.WITHER_SKELETON_SKULL)) {
				return;
			}
			consumer.accept(stack);
		};
	}
}

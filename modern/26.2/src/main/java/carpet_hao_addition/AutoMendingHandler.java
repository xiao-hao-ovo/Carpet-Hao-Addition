package carpet_hao_addition;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 「自动经验修补」的实际逻辑 —— **移植于 Carpet WuHu Addition**(上游类 {@code AutoMending})。
 * <p>
 * 放在 26.3 层而不是共享层:用到 {@code EnchantmentHelper.modifyDurabilityToRepairFromXp}
 * 这类版本相关 API(1.21 才有"附魔效果"这套机制,26.x 又改过一次名字)。
 */
public final class AutoMendingHandler {
	/** 检查间隔:tick 数,20 = 1 秒(与上游一致)。 */
	private static final int INTERVAL = 20;

	private AutoMendingHandler() {
	}

	/** 由 {@code ServerPlayer.tick} 的 mixin 在每 tick 末尾调用。 */
	public static void tick(ServerPlayer player) {
		if (!AutoMendingSettings.isEnabled()) {
			return;
		}
		if (player.level().getGameTime() % INTERVAL != 0) {
			return;
		}
		mending(player);
	}

	/** 把等级进度里已攒下的经验拿去修补一件带「经验修补」的受损装备。 */
	private static void mending(ServerPlayer player) {
		// 等级进度条里已经攒下、尚未消费掉的经验(点数)。
		int experience = (int) Math.floor(player.experienceProgress * player.getXpNeededForNextLevel());
		// 进度为 0 但已经有等级时至少给 1 点,否则经验凑不满一级就永远修不动。
		int amount = experience == 0 ? (player.experienceLevel > 0 ? 1 : 0) : experience;
		if (amount == 0) {
			return;
		}
		ItemStack target = findDamagedMendingItem(player);
		if (target == null) {
			return;
		}
		ServerLevel level = player.level();
		// 这些经验最多能换成多少点耐久(受附魔等级与物品本身的换算影响)。
		int repairable = EnchantmentHelper.modifyDurabilityToRepairFromXp(level, target, amount);
		int repaired = Math.min(repairable, target.getDamageValue());
		if (repaired <= 0) {
			return;
		}
		target.setDamageValue(target.getDamageValue() - repaired);
		// 扣掉与修复量等量的经验点,保证"经验换耐久"守恒。
		player.giveExperiencePoints(-repaired);
	}

	/** 找一件"带经验修补附魔 + 已损坏"的装备(主手 / 副手 / 各盔甲槽)。 */
	private static ItemStack findDamagedMendingItem(ServerPlayer player) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isDamaged() && hasMending(stack)) {
				return stack;
			}
		}
		return null;
	}

	/**
	 * 物品上是否有「经验修补」附魔。
	 * <p>
	 * 26.x 里附魔是物品栈上的 {@code DataComponents.ENCHANTMENTS} 组件(不是方块实体 / NBT 层),
	 * 拿到 {@code ItemEnchantments} 后按 key 遍历判断 —— {@code Holder.is(ResourceKey)} 最省事。
	 */
	private static boolean hasMending(ItemStack stack) {
		ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
		if (enchantments == null) {
			return false;
		}
		for (Holder<Enchantment> entry : enchantments.keySet()) {
			if (entry.is(Enchantments.MENDING)) {
				return true;
			}
		}
		return false;
	}
}

package carpet_hao_addition;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 「自动经验修补」的实际逻辑 —— **移植于 Carpet WuHu Addition**(上游类 {@code AutoMending})。
 * <p>
 * 放在 1.21.8 层而不是共享层:用到 {@code EnchantmentHelper.getRepairWithExperience} 这类
 * 版本相关 API(1.21 才有"附魔效果"这套机制,与旧版签名完全不同)。
 */
public final class AutoMendingHandler {
	/** 检查间隔:tick 数,20 = 1 秒(与上游一致)。 */
	private static final int INTERVAL = 20;

	private AutoMendingHandler() {
	}

	/** 由 {@code ServerPlayerEntity.tick} 的 mixin 在每 tick 末尾调用。 */
	public static void tick(ServerPlayerEntity player) {
		if (!AutoMendingSettings.isEnabled()) {
			return;
		}
		if (player.getEntityWorld().getTime() % INTERVAL != 0) {
			return;
		}
		mending(player);
	}

	/** 把等级进度里已攒下的经验拿去修补一件带「经验修补」的受损装备。 */
	private static void mending(ServerPlayerEntity player) {
		// 等级进度条里已经攒下、尚未消费掉的经验(点数)。
		int experience = (int) Math.floor(player.experienceProgress * player.getNextLevelExperience());
		// 进度为 0 但已经有等级时至少给 1 点,否则经验凑不满一级就永远修不动。
		int amount = experience == 0 ? (player.experienceLevel > 0 ? 1 : 0) : experience;
		if (amount == 0) {
			return;
		}
		ItemStack target = findDamagedMendingItem(player);
		if (target == null) {
			return;
		}
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		// 这些经验最多能换成多少点耐久(受附魔等级与物品本身的换算影响)。
		int repairable = EnchantmentHelper.getRepairWithExperience(world, target, amount);
		int repaired = Math.min(repairable, target.getDamage());
		if (repaired <= 0) {
			return;
		}
		target.setDamage(target.getDamage() - repaired);
		// 扣掉与修复量等量的经验点,保证"经验换耐久"守恒。
		player.addExperience(-repaired);
	}

	/** 找一件"带经验修补附魔 + 已损坏"的装备(主手 / 副手 / 各盔甲槽)。 */
	private static ItemStack findDamagedMendingItem(ServerPlayerEntity player) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = player.getEquippedStack(slot);
			if (stack.isDamaged() && hasMending(stack)) {
				return stack;
			}
		}
		return null;
	}

	/**
	 * 物品上是否有「经验修补」附魔。
	 * <p>
	 * 按附魔 key 遍历判断 —— {@code ItemEnchantmentsComponent.getLevel(...)} 要的是
	 * {@code RegistryEntry} 而不是 {@code RegistryKey},而这里只有 key 常量,
	 * 用 {@code RegistryEntry.matchesKey} 最省事。
	 */
	private static boolean hasMending(ItemStack stack) {
		for (RegistryEntry<Enchantment> entry : EnchantmentHelper.getEnchantments(stack).getEnchantments()) {
			if (entry.matchesKey(Enchantments.MENDING)) {
				return true;
			}
		}
		return false;
	}
}

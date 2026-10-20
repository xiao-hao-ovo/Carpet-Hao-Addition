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
	/** 检查间隔:tick 数,20 = 1 秒。 */
	private static final int INTERVAL = 20;

	private AutoMendingHandler() {
	}

	/** 由 {@code ServerPlayerEntity.tick} 的 mixin 在每 tick 末尾调用。 */
	public static void tick(ServerPlayerEntity player) {
		if (!AutoMendingSettings.isEnabled()) {
			return;
		}
		if (player.getWorld().getTime() % INTERVAL != 0) {
			return;
		}
		mending(player);
	}

	/** 把等级进度里已攒下的经验拿去修补一件带「经验修补」的受损装备。 */
	private static void mending(ServerPlayerEntity player) {
		// 等级进度条里已经攒下、尚未消费掉的经验(点数)。
		//
		// 进度为 0 时就是没有可用经验:原版同样不会拿等级里的经验去修补,等级是一道不可分割的
		// 门槛。早先这里"至少给 1 点"兜底,结果是 addExperience(-1) 在 experienceProgress == 0
		// 时进入降级分支(见 PlayerEntity.addExperience:progress < 0 时 addExperienceLevels(-1)),
		// 玩家白白掉一整级,只换来两点耐久。
		int amount = (int) Math.floor(player.experienceProgress * player.getNextLevelExperience());
		if (amount <= 0) {
			return;
		}
		ItemStack target = findDamagedMendingItem(player);
		if (target == null) {
			return;
		}
		ServerWorld world = (ServerWorld) player.getWorld();
		// 这些经验最多能换成多少点耐久(受附魔等级与物品本身的换算影响)。
		// 注意单位:入参是**经验点数**,返回值是**耐久点数**,两者不是 1:1。
		int repairable = EnchantmentHelper.getRepairWithExperience(world, target, amount);
		int repaired = Math.min(repairable, target.getDamage());
		if (repaired <= 0) {
			return;
		}
		target.setDamage(target.getDamage() - repaired);
		// 扣经验必须按**实际用掉的**换算回去,不能拿耐久点数直接当经验扣 ——
		// 经验修补 1 点经验约换 2 点耐久,直接用 repaired 扣会把玩家的经验吞掉近一倍。
		// 修满了就用掉手头这些;只修了一部分就按比例折算(至少 1 点)。
		int cost = repaired >= repairable
				? amount
				: Math.max(1, (int) Math.round((double) amount * repaired / repairable));
		player.addExperience(-Math.min(cost, amount));
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

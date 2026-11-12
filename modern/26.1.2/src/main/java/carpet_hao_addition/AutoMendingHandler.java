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
	/** 检查间隔:tick 数,20 = 1 秒。 */
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
		ItemStack target = findDamagedMendingItem(player);
		if (target == null) {
			return;
		}
		// 先花进度条里已经攒下、尚未消费掉的经验(点数)。
		int available = (int) Math.floor(player.experienceProgress * player.getXpNeededForNextLevel());
		boolean fromLevel = false;
		if (available <= 0) {
			// 进度条空了,退掉一级来换。原版的经验修补只认捡到的经验球,不掉等级;
			// 这条规则的意思正是"用手里已有的经验和等级换修补",所以这里要主动扣级。
			if (player.experienceLevel <= 0) {
				return;
			}
			// 扣掉一级、进度条随之归零,损失的正好是当前级的升级门槛那么多点经验。
			available = player.getXpNeededForNextLevel();
			player.giveExperienceLevels(-1);
			player.experienceProgress = 0.0F;
			fromLevel = true;
		}
		ServerLevel level = player.level();
		// 这些经验最多能换成多少点耐久(受附魔等级与物品本身的换算影响)。
		// 注意单位:入参是**经验点数**,返回值是**耐久点数**,两者不是 1:1。
		int repairable = EnchantmentHelper.modifyDurabilityToRepairFromXp(level, target, available);
		int repaired = Math.min(repairable, target.getDamageValue());
		if (repaired <= 0) {
			if (fromLevel) {
				// 一点耐久也换不来,把这一级原样还回去,别白扣。
				player.giveExperiencePoints(available);
			}
			return;
		}
		target.setDamageValue(target.getDamageValue() - repaired);
		// 扣经验必须按**实际用掉的**换算回去,不能拿耐久点数直接当经验扣 ——
		// 经验修补 1 点经验约换 2 点耐久,直接用 repaired 扣会把玩家的经验吞掉近一倍。
		// 修满了就用掉手头这些;只修了一部分就按比例折算(至少 1 点)。
		int cost = repaired >= repairable
				? available
				: Math.max(1, (int) Math.round((double) available * repaired / repairable));
		if (fromLevel) {
			// 一整级换来的经验通常用不完,余下的还回去。退还时按扣级后的门槛折算,
			// 攒够一级会自己升回去,不会凭空多给。
			player.giveExperiencePoints(available - cost);
		} else {
			player.giveExperiencePoints(-Math.min(cost, available));
		}
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

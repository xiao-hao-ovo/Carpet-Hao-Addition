package carpet_hao_addition.easyplace;

import carpet_hao_addition.mixin.easyplace.BeaconEffectAccessor;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;

/**
 * 信标选中的效果 —— 主效果与副效果各占 7 位，主效果在高 7 位。
 *
 * <p>效果 id 存的是「注册表 id + 1」，0 表示这一半没选效果，所以两半都是 0 时
 * 协议值就是 0，客户端也就能靠"没有位"表示"这是一台空信标"。
 *
 * <p>信标没有方块状态需要还原，因此只实现物品数据这一侧。
 */
public final class BeaconItemData implements ItemDataCodec {

	private static final int HALF_BITS = 7;
	private static final int HALF_MASK = 0x7F;
	private static final String PRIMARY_KEY = "primary_effect";
	private static final String SECONDARY_KEY = "secondary_effect";
	private static final String BLOCK_ENTITY_ID = "minecraft:beacon";

	@Override
	public int encodeBlockEntity(BlockEntity blockEntity) {
		if (!(blockEntity instanceof BeaconBlockEntity beacon)) {
			return 0;
		}
		BeaconEffectAccessor accessor = (BeaconEffectAccessor) beacon;
		return (effectId(accessor.hao$getPrimaryPower()) << HALF_BITS) | effectId(accessor.hao$getSecondaryPower());
	}

	@Override
	public ItemStack decodeStack(int bits, ItemStack stack) {
		int primary = (bits >>> HALF_BITS) & HALF_MASK;
		int secondary = bits & HALF_MASK;
		if (primary == 0 && secondary == 0) {
			return stack;
		}

		CompoundTag tag = blockEntityTag(stack);
		if (tag == null) {
			tag = new CompoundTag();
		}
		if (tag.contains(PRIMARY_KEY) || tag.contains(SECONDARY_KEY)) {
			return stack;
		}

		boolean touched = writeEffect(tag, PRIMARY_KEY, primary);
		touched |= writeEffect(tag, SECONDARY_KEY, secondary);
		if (!touched) {
			return stack;
		}
		tag.putString("id", BLOCK_ENTITY_ID);

		ItemStack restored = stack.copy();
		restored.set(DataComponents.BLOCK_ENTITY_DATA, ItemDataCodec.wrapBlockEntityData(tag));
		return restored;
	}

	// ==================== 内部实现 ====================

	/** 效果 → 注册表 id + 1；没效果就是 0。 */
	private static int effectId(Holder<MobEffect> effect) {
		return effect == null ? 0 : BuiltInRegistries.MOB_EFFECT.getId(effect.value()) + 1;
	}

	/** 把「id + 1」还原成效果名字写进 NBT；写了返回 true。 */
	private static boolean writeEffect(CompoundTag tag, String key, int encodedId) {
		if (encodedId == 0) {
			return false;
		}
		Registry<MobEffect> registry = BuiltInRegistries.MOB_EFFECT;
		MobEffect effect = registry.byId(encodedId - 1);
		if (effect == null) {
			return false;
		}
		tag.putString(key, String.valueOf(registry.getId(effect)));
		return true;
	}

	private static CompoundTag blockEntityTag(ItemStack stack) {
		return ItemDataCodec.unwrapBlockEntityData(stack.get(DataComponents.BLOCK_ENTITY_DATA));
	}
}

package carpet_hao_addition.easyplace;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.DyeColor;

/**
 * 告示牌系列的物品数据通道 —— 站立告示牌 / 墙挂告示牌 / 悬挂告示牌 / 墙挂悬挂告示牌共用。
 *
 * <p>两家告示牌的差别只在方块实体 id 上（{@code minecraft:sign} 与
 * {@code minecraft:hanging_sign}），构造时传进来即可，不必拆成两个类。
 *
 * <p><b>位布局由投影端决定，不可改</b>：
 * <pre>
 *   bit5       正面文字发光
 *   bit6-9     正面文字颜色（DyeColor 序号）
 *   bit10      已打蜡
 *   bit11      背面文字发光
 *   bit12-15   背面文字颜色
 * </pre>
 */
public final class SignItemData implements ItemDataCodec {

	private static final int FRONT_GLOWING = 1 << 5;
	private static final int FRONT_COLOR_SHIFT = 6;
	private static final int WAXED = 1 << 10;
	private static final int BACK_GLOWING = 1 << 11;
	private static final int BACK_COLOR_SHIFT = 12;
	private static final int COLOR_MASK = 0b1111;
	private static final String FRONT = "front_text";
	private static final String BACK = "back_text";
	private static final String GLOWING_KEY = "has_glowing_text";
	private static final String COLOR_KEY = "color";

	private final String blockEntityId;

	public SignItemData(String blockEntityId) {
		this.blockEntityId = blockEntityId;
	}

	@Override
	public int encodeStack(ItemStack stack) {
		return encodeNbt(blockEntityTag(stack));
	}

	@Override
	public int encodeNbt(NbtCompound tag) {
		if (tag == null) {
			return 0;
		}
		int bits = encodeSide(tag, FRONT, FRONT_GLOWING, FRONT_COLOR_SHIFT);
		bits |= encodeSide(tag, BACK, BACK_GLOWING, BACK_COLOR_SHIFT);
		if (tag.getBoolean("is_waxed").orElse(false)) {
			bits |= WAXED;
		}
		return bits;
	}

	@Override
	public int encodeBlockEntity(BlockEntity blockEntity) {
		if (!(blockEntity instanceof SignBlockEntity sign)) {
			return 0;
		}
		int bits = encodeSide(sign.getFrontText(), FRONT_GLOWING, FRONT_COLOR_SHIFT);
		bits |= encodeSide(sign.getBackText(), BACK_GLOWING, BACK_COLOR_SHIFT);
		if (sign.isWaxed()) {
			bits |= WAXED;
		}
		return bits;
	}

	@Override
	public ItemStack decodeStack(int bits, ItemStack stack) {
		ItemStack restored = stack.copy();
		NbtCompound tag = blockEntityTag(restored);
		if (tag == null) {
			tag = new NbtCompound();
		}
		writeSide(tag, FRONT, colorAt(bits, FRONT_COLOR_SHIFT), (bits & FRONT_GLOWING) != 0);
		writeSide(tag, BACK, colorAt(bits, BACK_COLOR_SHIFT), (bits & BACK_GLOWING) != 0);
		tag.putString("id", blockEntityId);
		restored.set(DataComponentTypes.BLOCK_ENTITY_DATA, ItemDataCodec.wrapBlockEntityData(tag));
		return restored;
	}

	// ==================== 内部实现 ====================

	private static int encodeSide(NbtCompound tag, String key, int glowingBit, int colorShift) {
		NbtCompound side = tag.getCompound(key).orElse(null);
		if (side == null || side.isEmpty()) {
			return 0;
		}
		int bits = side.getBoolean(GLOWING_KEY).orElse(false) ? glowingBit : 0;
		String name = side.getString(COLOR_KEY).orElse("");
		for (DyeColor color : DyeColor.values()) {
			if (color.getId().equals(name)) {
				bits |= (color.ordinal() & COLOR_MASK) << colorShift;
				break;
			}
		}
		return bits;
	}

	private static int encodeSide(SignText text, int glowingBit, int colorShift) {
		int bits = text.isGlowing() ? glowingBit : 0;
		return bits | ((text.getColor().ordinal() & COLOR_MASK) << colorShift);
	}

	private static DyeColor colorAt(int bits, int colorShift) {
		int ordinal = (bits >>> colorShift) & COLOR_MASK;
		DyeColor[] colors = DyeColor.values();
		return ordinal < colors.length ? colors[ordinal] : DyeColor.BLACK;
	}

	private static void writeSide(NbtCompound tag, String key, DyeColor color, boolean glowing) {
		NbtCompound side = tag.getCompound(key).orElse(null);
		if (side == null) {
			side = new NbtCompound();
		}
		if (!side.contains("messages")) {
			// 原版放置时会照搬这四行；缺了会让告示牌变成"没写过字"的初始态。
			NbtList messages = new NbtList();
			for (int line = 0; line < 4; ++line) {
				messages.add(NbtString.of("\"\""));
			}
			side.put("messages", messages);
		}
		side.putString(COLOR_KEY, color.getId());
		side.putBoolean(GLOWING_KEY, glowing);
		tag.put(key, side);
	}

	private static NbtCompound blockEntityTag(ItemStack stack) {
		return ItemDataCodec.unwrapBlockEntityData(stack.get(DataComponentTypes.BLOCK_ENTITY_DATA));
	}
}

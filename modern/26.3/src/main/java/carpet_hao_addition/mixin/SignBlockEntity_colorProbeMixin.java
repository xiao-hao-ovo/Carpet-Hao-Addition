package carpet_hao_addition.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标牌颜色自愈。
 * <p>
 * easyplace 放置标牌时,同一个位置上会有多份方块实体 NBT 先后到达:服务端那份带着正确颜色,
 * 另有一份(客户端本地预测/后到的更新)颜色是黑的,会把已经拿到的颜色**覆盖掉**。
 * 实测日志里同一个 pos 上 cyan 与 black 交替出现,最终画出来就是黑字 —— 这不是 NBT 格式问题
 * (反编译 {@code SignBlockEntity.saveAdditional} 可见 front_text 直接由 {@code SignText.CODEC}
 * 序列化,color 就是 {@code DyeColor.CODEC} 的字符串)。
 * <p>
 * 这里在每次 {@code loadAdditional} 之后:读到非黑颜色就记下来;读到黑而该位置曾出现过非黑颜色时,
 * 用记下的颜色恢复它。**恢复后立即清掉记录**(只自愈一次),这样玩家事后主动用染料把标牌染成黑
 * 不会被改回去;本来就是黑字的标牌也不受影响。记录表有上限,避免长期累积。
 */
@Mixin(SignBlockEntity.class)
public class SignBlockEntity_colorProbeMixin {
	/** 记录表上限:防止长期累积(正常远达不到)。 */
	@Unique
	private static final int hao$MAX_KNOWN = 4096;

	/** 位置+面 → 见过的非黑颜色。key 带侧别前缀,客户端与服务端互不干扰。 */
	@Unique
	private static final Map<String, DyeColor> hao$knownColors = new ConcurrentHashMap<>();

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void hao$restoreColor(ValueInput input, CallbackInfo callbackInfo) {
		SignBlockEntity self = (SignBlockEntity) (Object) this;
		Level level = self.getLevel();
		if (level == null) {
			return;
		}
		BlockPos pos = self.getBlockPos();
		String side = level.isClientSide() ? "C" : "S";
		for (SignTextSlot slot : SignTextSlot.values()) {
			SignText text = self.getText(slot);
			String key = side + pos.asLong() + ":" + slot.name();
			DyeColor known = hao$knownColors.get(key);
			if (text.getColor() != DyeColor.BLACK) {
				if (hao$knownColors.size() < hao$MAX_KNOWN) {
					hao$knownColors.put(key, text.getColor());
				}
			} else if (known != null) {
				// 这一份 NBT 把颜色冲成了黑 → 用之前见过的颜色恢复,并清掉记录(只自愈一次)。
				self.setText(text.withColor(known), slot);
				hao$knownColors.remove(key);
			}
		}
	}
}

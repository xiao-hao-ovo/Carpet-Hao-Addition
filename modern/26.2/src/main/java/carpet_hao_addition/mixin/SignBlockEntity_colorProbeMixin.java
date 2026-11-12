package carpet_hao_addition.mixin;

import carpet_hao_addition.HaoDebug;
import carpet_hao_addition.easyplace.SignColorMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 标牌颜色补回。
 * <p>
 * 标牌方块实体会被多份 NBT 先后加载(客户端本地预测、服务端 BE 包、方块重建……),
 * 其中有的是"文字在、颜色黑"或空的默认状态,会把 easyplace 刚设好的颜色冲掉
 * —— 实测同一 pos 的序列是 {@code lime → black → black},最后画出来就是黑字。
 * <p>
 * 这里在每次 {@code loadAdditional} 之后查一次 {@link SignColorMemory}:只要当前颜色是黑、
 * 而这块地方我们记过颜色,就把颜色补回去。**记录不删**,所以后面再来多少份黑 NBT 都挡得住;
 * 玩家用染料染成别的颜色时颜色不是黑,不会被动手。
 * <p>
 * 26.1.2 / 26.2 没有 26.3 的 {@code SignTextSlot} 枚举,也没有 {@code SignText#withColor} ——
 * 这里改用 {@code getText(boolean)} / {@code setText(SignText, boolean)} 与 {@code setColor(DyeColor)},
 * 正反面各处理一次,语义与 26.3 版本一致。
 */
@Mixin(SignBlockEntity.class)
public class SignBlockEntity_colorProbeMixin {
	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void hao$restoreColor(ValueInput input, CallbackInfo callbackInfo) {
		SignBlockEntity self = (SignBlockEntity) (Object) this;
		Level level = self.getLevel();
		if (level == null) {
			return;
		}
		boolean clientSide = level.isClientSide();
		BlockPos pos = self.getBlockPos();
		// front = true, back = false：键名与 26.3 的 SignTextSlot 枚举名保持一致
		for (boolean front : new boolean[]{true, false}) {
			SignText text = self.getText(front);
			String slotName = front ? "FRONT" : "BACK";
			DyeColor want = SignColorMemory.recall(clientSide, pos, slotName);
			if (want == null) {
				continue;
			}
			HaoDebug.log("[hao-sign] loadAdditional " + (clientSide ? "C" : "S") + " pos=" + pos
					+ " " + slotName + " 颜色=" + text.getColor().getName()
					+ " 期望=" + want.getName());
			if (text.getColor() == DyeColor.BLACK) {
				self.setText(text.setColor(want), front);
				HaoDebug.log("[hao-sign] 补回颜色 " + slotName + " -> " + want.getName() + " pos=" + pos);
			}
		}
	}
}

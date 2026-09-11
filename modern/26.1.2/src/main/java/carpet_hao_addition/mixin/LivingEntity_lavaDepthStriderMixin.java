package carpet_hao_addition.mixin;

import carpet_hao_addition.LavaDepthStriderSettings;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * lavaDepthStrider:让岩浆中的玩家走 {@code LivingEntity.travelInFluid} 的<b>水中分支</b>。
 * <p>
 * 原版把流体移动拆成了独立方法:{@code travel} 只负责入口判断
 * ({@code isInWater() || isInLava()} 即进入 {@code travelInFluid}),真正的水/岩浆
 * 分流在 {@code travelInFluid} 内:
 * <pre>
 *   if (this.isInWater())      // 水分支:按 water_movement_efficiency 调整阻尼/加速
 *   else                       // 岩浆分支:固定低加速 + 0.5 水平阻尼(这就是"岩浆减速")
 * </pre>
 * 水中分支读取的 {@code WATER_MOVEMENT_EFFICIENCY} 属性正是深海探索者的载体,所以只要让
 * 「玩家 + 身处岩浆 + 该属性 &gt; 0」这一情形的 {@code isInWater()} 返回 true,
 * 走的就完全是水中的那套公式(含上浮/下潜与附魔等级加成),岩浆分支因 else 被跳过。
 * <p>
 * 本规则默认关闭;未开启或条件不满足时一律交还原版实现({@code original.call})。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntity_lavaDepthStriderMixin {
	@WrapOperation(method = "travelInFluid", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"))
	private boolean hao$lavaActsAsWater(LivingEntity entity, Operation<Boolean> original) {
		if (original.call(entity)) {
			// 真的在水里,保持原版行为。
			return true;
		}
		if (!LavaDepthStriderSettings.isEnabled()) {
			return false;
		}
		// 只对玩家生效(其他生物行为不变)。
		if (!(entity instanceof Player player)) {
			return false;
		}
		if (!player.isInLava()) {
			return false;
		}
		// 深海探索者的原版判据:water_movement_efficiency > 0(只在靴子槽生效)。
		return player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) > 0.0;
	}
}

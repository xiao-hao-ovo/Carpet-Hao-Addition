package carpet_hao_addition.mixin;

import carpet_hao_addition.WackoBeaconsSettings;

import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.AbstractList;
import java.util.List;

/**
 * 信标效果表(客户端)—— 移植自 getcmdrolled/wacko-beacons。
 * <p>
 * 把 {@code EFFECTS_BY_LEVEL}(1.21.x 里效果表的名字;26.x 叫 {@code BEACON_EFFECTS})
 * 换成一张**按规则动态求值**的只读视图:规则关时原样返回原版表,规则开时返回改动过的表
 * (Regeneration 挪到第 2 档)。
 * <p>
 * 为什么要动态视图而不是在 {@code <clinit>} 里直接判断规则:这个静态初始化在类第一次被加载时
 * 就跑完了,那时 Carpet 的规则值未必已经就绪,而且玩家用 {@code /carpet} 开关规则后就再也不会
 * 重新求值。动态视图每次读都按当前规则返回,开关立即生效。
 * <p>
 * 原仓库写的是"带初值的 {@code @Shadow} 字段",那在 Mixin 里是无效的(带初值的 shadow 会被忽略
 * 或直接报错),所以这里按正确的做法实现。
 */
@Mixin(BeaconBlockEntity.class)
public class WackoBeacons_BeaconBlockEntityMixin {
	/** 只写 {@code @Mutable} 不写 {@code @Final}:各 1.21.x 层是否 final 不影响注入。 */
	@Mutable
	@Shadow
	private static List<List<RegistryEntry<StatusEffect>>> EFFECTS_BY_LEVEL;

	/** 改动后的效果表(照原仓库:Regeneration 挪到第 2 档,顶级档不再单列它)。 */
	@Unique
	private static final List<List<RegistryEntry<StatusEffect>>> hao$WACKO_EFFECTS = List.of(
			List.of(StatusEffects.SPEED, StatusEffects.HASTE),
			List.of(StatusEffects.REGENERATION, StatusEffects.RESISTANCE),
			List.of(StatusEffects.JUMP_BOOST, StatusEffects.STRENGTH),
			List.of());

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void hao$installWackoTable(CallbackInfo callbackInfo) {
		List<List<RegistryEntry<StatusEffect>>> vanilla = EFFECTS_BY_LEVEL;
		EFFECTS_BY_LEVEL = new AbstractList<>() {
			@Override
			public List<RegistryEntry<StatusEffect>> get(int index) {
				return WackoBeaconsSettings.isEnabled()
						? hao$WACKO_EFFECTS.get(index)
						: vanilla.get(index);
			}

			@Override
			public int size() {
				return vanilla.size();
			}
		};
	}
}

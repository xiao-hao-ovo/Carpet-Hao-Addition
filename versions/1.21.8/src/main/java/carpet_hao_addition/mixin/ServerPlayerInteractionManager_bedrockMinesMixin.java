package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * haoBedrockMines:在服务器挖掘推进的<b>调用点</b>把基岩的挖掘进度固定为黑曜石。
 * <p>
 * 选择在 ServerPlayerInteractionManager 的 calcBlockBreakingDelta 调用处包装,而不是
 * 直接注入 calcBlockBreakingDelta 本体:这样即使其它扩展(如 Carpet-AMS-Addition 的
 * commandCustomBlockHardness)也在该方法上做取消型注入/自定义硬度,只要本规则开启,
 * 基岩的挖掘进度仍按黑曜石计算(本规则优先);未开启时不干预(AMS 等照常工作)。
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManager_bedrockMinesMixin {
	@WrapOperation(method = "continueMining", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"))
	private float hao$bedrockDeltaContinue(BlockState state, PlayerEntity player, BlockView world, BlockPos pos,
			Operation<Float> original) {
		return hao$bedrockDelta(state, player, world, pos, original);
	}

	@WrapOperation(method = "processBlockBreakingAction", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F",
			ordinal = 0))
	private float hao$bedrockDeltaProcess0(BlockState state, PlayerEntity player, BlockView world, BlockPos pos,
			Operation<Float> original) {
		return hao$bedrockDelta(state, player, world, pos, original);
	}

	@WrapOperation(method = "processBlockBreakingAction", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F",
			ordinal = 1))
	private float hao$bedrockDeltaProcess1(BlockState state, PlayerEntity player, BlockView world, BlockPos pos,
			Operation<Float> original) {
		return hao$bedrockDelta(state, player, world, pos, original);
	}

	private static float hao$bedrockDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos,
			Operation<Float> original) {
		// 先取当前实际挖掘进度(可能已被 AMS customBlockHardness 等设置为自定义硬度,
		// 0 也是合法可挖值,必须保留)。
		float delta = original.call(state, player, world, pos);
		if (BedrockCanBeMinedSettings.isEnabled() && state.isOf(Blocks.BEDROCK) && delta <= 0f) {
			// 基岩仍不可挖(无外部设置,原版为 -1)时,按黑曜石兜底,使其可被挖掘。
			return Blocks.OBSIDIAN.getDefaultState().calcBlockBreakingDelta(player, world, pos);
		}
		return delta;
	}
}

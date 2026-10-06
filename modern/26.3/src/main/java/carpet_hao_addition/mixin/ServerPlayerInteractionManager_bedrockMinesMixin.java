package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * haoBedrockMines:在服务器挖掘推进的<b>调用点</b>把基岩的挖掘进度固定为黑曜石。
 * <p>
 * 选择在 ServerPlayerGameMode 的 calcBlockBreakingDelta 调用处包装,而不是
 * 直接注入 calcBlockBreakingDelta 本体:这样即使其它扩展(如 Carpet-AMS-Addition 的
 * commandCustomBlockHardness)也在该方法上做取消型注入/自定义硬度,只要本规则开启,
 * 基岩的挖掘进度仍按黑曜石计算(本规则优先);未开启时不干预(AMS 等照常工作)。
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerInteractionManager_bedrockMinesMixin {
	@WrapOperation(method = "incrementDestroyProgress", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
	private float hao$bedrockDeltaContinue(BlockState state, Player player, BlockGetter world, BlockPos pos,
			Operation<Float> original) {
		return hao$bedrockDelta(state, player, world, pos, original);
	}

	@WrapOperation(method = "handleBlockBreakAction", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
			ordinal = 0))
	private float hao$bedrockDeltaProcess0(BlockState state, Player player, BlockGetter world, BlockPos pos,
			Operation<Float> original) {
		return hao$bedrockDelta(state, player, world, pos, original);
	}

	@WrapOperation(method = "handleBlockBreakAction", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
			ordinal = 1))
	private float hao$bedrockDeltaProcess1(BlockState state, Player player, BlockGetter world, BlockPos pos,
			Operation<Float> original) {
		return hao$bedrockDelta(state, player, world, pos, original);
	}

	private static float hao$bedrockDelta(BlockState state, Player player, BlockGetter world, BlockPos pos,
			Operation<Float> original) {
		// 先取当前实际挖掘进度(可能已被 AMS customBlockHardness 等设置为自定义硬度,
		// 0 也是合法可挖值,必须保留)。
		float delta = original.call(state, player, world, pos);
		if (BedrockCanBeMinedSettings.isEnabled() && state.is(Blocks.BEDROCK) && delta <= 0f) {
			// 基岩仍不可挖(无外部设置,原版为 -1)时,按黑曜石兜底,使其可被挖掘。
			return hao$bedrockDelta(state, player);
		}
		return delta;
	}

	/** 按固定硬度 50 计算挖掘进度(与"基岩可挖时的硬度"一致,不借用黑曜石状态)。 */
	private static float hao$bedrockDelta(BlockState state, Player player) {
		int divisor = player.hasCorrectToolForDrops(state) ? 30 : 100;
		return player.getDestroySpeed(state) / 50.0F / divisor;
	}
}

package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * haoBedrockMines(客户端层):在<b>调用点</b>把基岩的挖掘进度兜底为黑曜石。
 * <p>
 * 为什么客户端要单独做一份:{@link BlockState_bedrockCanBeMinedMixin} 是在
 * {@code BlockStateBase.getDestroyProgress} 的 RETURN 处兜底,但其它扩展(典型是
 * Carpet-AMS-Addition 的 {@code commandCustomBlockHardness})会在该方法 <b>HEAD</b> 处
 * 直接设置返回值并取消,方法提前返回,Return 注入被整段跳过。
 * <p>
 * 而挖掘进度是<b>客户端驱动</b>的:客户端算不出进度就不会发挖掘包,服务端再兜底也没用,
 * 表现就是"对着基岩按住左键连裂纹都不出现"。在调用点包装可绕开方法体被取消的问题。
 * <p>
 * 与外部扩展共存:未开启规则、非基岩、或外部已给出有效进度(&gt; 0)时一律放行。
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameMode_bedrockMinesMixin {
	@WrapOperation(method = "continueDestroyBlock", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
	private float hao$bedrockDeltaClient(BlockState state, Player player, BlockGetter level, BlockPos pos,
			Operation<Float> original) {
		float delta = original.call(state, player, level, pos);
		if (BedrockCanBeMinedSettings.isEnabled() && state.is(Blocks.BEDROCK) && delta <= 0f) {
			// 按黑曜石兜底,使客户端能累积进度并发出挖掘包。
			return Blocks.OBSIDIAN.defaultBlockState().getDestroyProgress(player, level, pos);
		}
		return delta;
	}
}

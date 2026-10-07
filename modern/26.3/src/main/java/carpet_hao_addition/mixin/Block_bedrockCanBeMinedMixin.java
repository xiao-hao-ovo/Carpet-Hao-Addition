package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * bedrockCanBeMined:基岩被(非创造)玩家挖碎后,在其位置掉落基岩物品。
 * <p>
 * 26.3 起 {@code Block.playerDestroy} 的前两个参数由 {@code Level}/{@code Player} 变成了
 * {@code ServerLevel}/{@code ServerPlayer},所以 handler 的参数类型必须跟着改
 * (26.1.2 / 26.2 仍是 {@code Level}/{@code Player} 的旧签名)。
 */
@Mixin(Block.class)
public abstract class Block_bedrockCanBeMinedMixin {
	@Inject(method = "playerDestroy", at = @At("HEAD"))
	private void hao$bedrockDropsItself(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state,
			BlockEntity blockEntity, ItemStack stack, CallbackInfo ci) {
		if (player == null || player.isCreative()) {
			return;
		}
		if (!BedrockCanBeMinedSettings.isEnabled() || !state.is(Blocks.BEDROCK)) {
			return;
		}

		Block.popResource(world, pos, new ItemStack(Blocks.BEDROCK));
	}
}

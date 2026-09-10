package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * bedrockCanBeMined:基岩被(非创造)玩家挖碎后,在其位置掉落基岩物品。
 */
@Mixin(Block.class)
public abstract class Block_bedrockCanBeMinedMixin {
	@Inject(method = "playerDestroy", at = @At("HEAD"))
	private void hao$bedrockDropsItself(Level world, Player player, BlockPos pos, BlockState state,
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

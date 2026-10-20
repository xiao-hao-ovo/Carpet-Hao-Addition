package carpet_hao_addition.mixin;

import carpet_hao_addition.BedrockCanBeMinedSettings;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * bedrockCanBeMined:基岩被(非创造)玩家挖碎后,在其位置掉落基岩物品。
 */
@Mixin(Block.class)
public abstract class Block_bedrockCanBeMinedMixin {
	@Inject(method = "afterBreak", at = @At("HEAD"))
	private void hao$bedrockDropsItself(World world, PlayerEntity player, BlockPos pos, BlockState state,
			BlockEntity blockEntity, ItemStack stack, CallbackInfo ci) {
		// 这是 @Mixin(Block.class) 的 afterBreak —— 每次玩家破坏任何方块都会走到,
		// 所以 isOf 放在 carpet 规则查询前面。
		if (!state.isOf(Blocks.BEDROCK)) {
			return;
		}
		if (player == null || player.getAbilities().creativeMode) {
			return;
		}
		if (!BedrockCanBeMinedSettings.isEnabled()) {
			return;
		}

		Block.dropStack(world, pos, new ItemStack(Blocks.BEDROCK));
	}
}

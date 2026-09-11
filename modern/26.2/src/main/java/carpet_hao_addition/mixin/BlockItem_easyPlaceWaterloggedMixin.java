package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.PlaceWaterloggedHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * easyPlaceWaterlogged(服务端):方块放下后,若玩家正在轻松放置,就给可含水却没含水的方块补上。
 */
@Mixin(BlockItem.class)
public abstract class BlockItem_easyPlaceWaterloggedMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void hao$waterlogAfterPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		if (!cir.getReturnValue().consumesAction()) {
			return;
		}
		if (!(context.getPlayer() instanceof ServerPlayer player)) {
			return;
		}
		if (!(context.getLevel() instanceof ServerLevel world)) {
			return;
		}
		BlockPos pos = context.getClickedPos();
		BlockState placed = world.getBlockState(pos);
		PlaceWaterloggedHandler.onBlockPlaced(player, world, pos, placed);
	}
}

package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 轻松放置标牌时, 让 postPlacement 的【返回值】为 false。 */
@Mixin(StandingAndWallBlockItem.class)
public class SignPlacementMixin {
    @Inject(method = "postPlacement", at = @At("RETURN"), cancellable = true)
    private void hao$suppressSuccess(BlockPos pos, Level world, Player player, ItemStack stack,
                                     BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ProjectionPlacement.isProjected()
                && ProjectionPlacement.getPlaceTargetPos().equals(pos)
                && world.getBlockState(pos).getBlock() instanceof SignBlock) {
            cir.setReturnValue(false);
        }
    }
}

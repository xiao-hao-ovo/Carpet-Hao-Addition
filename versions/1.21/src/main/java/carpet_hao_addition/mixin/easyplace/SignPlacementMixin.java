package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.SignItem;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.block.SignBlock;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 轻松放置标牌时, 让 postPlacement 的【返回值】为 false。 */
@Mixin(SignItem.class)
public class SignPlacementMixin {
    @Inject(method = "postPlacement", at = @At("RETURN"), cancellable = true)
    private void hao$suppressSuccess(BlockPos pos, World world, PlayerEntity player, ItemStack stack,
                                     BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ProjectionPlacement.isProjected()
                && ProjectionPlacement.placeTarget().equals(pos)
                && world.getBlockState(pos).getBlock() instanceof SignBlock) {
            cir.setReturnValue(false);
        }
    }
}

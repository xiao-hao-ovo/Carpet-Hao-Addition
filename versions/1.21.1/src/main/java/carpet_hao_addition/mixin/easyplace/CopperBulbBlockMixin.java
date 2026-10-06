package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BulbBlock;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BulbBlock.class, priority = 900)
public abstract class CopperBulbBlockMixin {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void hao_cancelOnPlace(BlockState state, World world, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        if (!BetterEasyPlaceProtocolHandler.isEasyPlaceState()) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void hao_cancelNeighborUpdate(BlockState state, World world, BlockPos pos, Block block, net.minecraft.block.enums.Orientation orientation, boolean isMoving, CallbackInfo ci) {
        if (!BetterEasyPlaceProtocolHandler.isEasyPlaceState()) {
            return;
        }
        if (pos.equals(BetterEasyPlaceProtocolHandler.getPlaceTargetPos()) && block == BetterEasyPlaceProtocolHandler.getPlaceTargetBlock()) {
            ci.cancel();
        }
    }
}

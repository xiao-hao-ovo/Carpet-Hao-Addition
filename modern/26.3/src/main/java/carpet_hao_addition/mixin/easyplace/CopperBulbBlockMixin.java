package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CopperBulbBlock.class, priority = 900)
public abstract class CopperBulbBlockMixin {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void hao_cancelOnPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        if (!BetterEasyPlaceProtocolHandler.isEasyPlaceState()) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void hao_cancelNeighborUpdate(BlockState state, Level world, BlockPos pos, Block block, Orientation orientation, boolean isMoving, CallbackInfo ci) {
        if (!BetterEasyPlaceProtocolHandler.isEasyPlaceState()) {
            return;
        }
        if (pos.equals(BetterEasyPlaceProtocolHandler.getPlaceTargetPos()) && block == BetterEasyPlaceProtocolHandler.getPlaceTargetBlock()) {
            ci.cancel();
        }
    }
}

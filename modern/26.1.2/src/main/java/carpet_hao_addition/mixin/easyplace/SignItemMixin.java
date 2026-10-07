package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 轻松放置标牌时, 让方块实体数据的写入【返回值】为 false。 */
@Mixin(SignItem.class)
public class SignItemMixin {
    @Inject(method = "updateCustomBlockEntityTag", at = @At("RETURN"), cancellable = true)
    private void hao$suppressSuccess(BlockPos pos, Level world, Player player, ItemStack stack,
                                     BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (BetterEasyPlaceProtocolHandler.isEasyPlaceState()
                && BetterEasyPlaceProtocolHandler.getPlaceTargetPos().equals(pos)
                && world.getBlockState(pos).getBlock() instanceof SignBlock) {
            cir.setReturnValue(false);
        }
    }
}

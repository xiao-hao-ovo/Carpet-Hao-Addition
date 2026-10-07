package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.StandingAndWallBlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.getRelativeHitZ;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.isProtocol;

/**
 * 标牌/旗帜等"可贴墙物品"自己覆写了 getPlacementState,不走 {@link BlockItemMixin} 的解码,
 * 所以要对子类再注入一次,朝向(rotation/facing)才能跟随投影。
 */
@Mixin(StandingAndWallBlockItem.class)
public abstract class VerticallyAttachableBlockItemMixin {
    /** 不能用 @Shadow getBlock():它是 Item 声明的方法,子类没声明会启动期报错。 */
    @Unique
    private Block hao$block() {
        return ((net.minecraft.world.item.BlockItem) (Object) this).getBlock();
    }

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void hao$decodeEasyPlaceProtocol(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        // 必须 RETURN:子类原本会先决定站牌还是墙牌,HEAD 直接改返回值会跳过这个选择。
        BlockState baseState = cir.getReturnValue();
        if (baseState == null) {
            return;
        }
        if (!BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
            return;
        }
        double relativeHitZ = getRelativeHitZ(context.getClickLocation(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
        if (!isProtocol(relativeHitZ)) {
            return;
        }
        BlockState state = BetterEasyPlaceProtocolHandler.decodePlacementState(baseState.getBlock(), context, baseState);
        System.out.println("[hao-easyplace] [子类解码] block=" + hao$block()
                + " 原=" + baseState + " 改=" + state
                + " side=" + context.getClickedFace() + " hitPos=" + context.getClickLocation()
                + " 世界该处=" + context.getLevel().getBlockState(context.getClickedPos()));
        if (state == null) {
            cir.setReturnValue(null);
            return;
        }
        cir.setReturnValue(state);
    }
}

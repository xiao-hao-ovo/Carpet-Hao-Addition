package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import carpet_hao_addition.easyplace.ClientEasyPlaceProtocolHelper;
import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;
import carpet_hao_addition.easyplace.EasyPlacePendingPlacement;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.1.2 用的 litematica(0.27.14)把协议钩子
 * {@code applyCarpetProtocolHitVec} / {@code applyPlacementProtocolV3} 与
 * {@code doEasyPlaceAction} 都放在 {@code WorldUtils} 上,所以这里合并成一个 mixin;
 * 26.2 起 litematica 把前两个钩子挪到了 {@code EasyPlaceUtils}(那边因此拆成两个 mixin)。
 */
@Mixin(WorldUtils.class)
public abstract class WorldUtilsMixin {

    @Inject(
            method = "applyCarpetProtocolHitVec",
            at = @At(value = "RETURN"),
            require = 0,
            cancellable = true)
    private static void hao$encodeProtocolBits(BlockPos pos, BlockState state, Vec3 hitVecIn, CallbackInfoReturnable<Vec3> cir) {
        if (BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
            Vec3 out = ClientEasyPlaceProtocolHelper.encodeHitPosItemData(cir.getReturnValue(), pos, state);
            System.out.println("[hao-easyplace] [CarpetVec] pos=" + pos + " in=" + cir.getReturnValue() + " out=" + out);
            cir.setReturnValue(out);
        }
    }

    @Inject(
            method = "applyPlacementProtocolV3",
            at = @At(value = "RETURN"),
            require = 0,
            cancellable = true)
    private static void hao$encodeProtocolBitsV3(BlockPos pos, BlockState state, Vec3 hitVecIn, CallbackInfoReturnable<Vec3> cir) {
        if (!BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
            return;
        }
        Vec3 encoded = ClientEasyPlaceProtocolHelper.encodeHitPosItemData(cir.getReturnValue(), pos, state);
        System.out.println("[hao-easyplace] [V3] pos=" + pos + " state=" + state
                + " litematica返回=" + cir.getReturnValue() + " 我们编码后=" + encoded);
        EasyPlacePendingPlacement.pos = pos;
        EasyPlacePendingPlacement.schematic = state;
        EasyPlacePendingPlacement.hitVec = encoded;
        cir.setReturnValue(encoded);
    }

    /**
     * 多阶段方块(雪层/海泡菜)的循环放置:原版一次只放一层,需连续调用多次才能堆到投影层数。
     * 用缓存的参数在 doEasyPlaceAction 返回后补放(不依赖 litematica 的局部变量表)。
     */
    @Inject(
            method = "doEasyPlaceAction",
            at = @At(value = "RETURN"),
            require = 0)
    private static void hao$completeMultiStageBlocks(Minecraft mc, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos pos = EasyPlacePendingPlacement.pos;
        BlockState stateSchematic = EasyPlacePendingPlacement.schematic;
        Vec3 hitVec = EasyPlacePendingPlacement.hitVec;
        EasyPlacePendingPlacement.clear();

        if (pos == null || stateSchematic == null || hitVec == null || mc.level == null || mc.player == null) {
            return;
        }
        if (!(BetterEasyPlaceProtocolHandler.getAdapter(stateSchematic.getBlock())
                instanceof MultiStageBlockProtocolStateAdapter multiStageAdapter)) {
            return;
        }

        MultiStageBlockProtocolStateAdapter.LoopContext ctx = new MultiStageBlockProtocolStateAdapter.LoopContext();
        ctx.stateSchematic = stateSchematic;
        ctx.stateClient = mc.level.getBlockState(pos);
        multiStageAdapter.hao$setLoopCount(ctx);

        int stackCount = Integer.MAX_VALUE;
        if (!mc.player.getAbilities().instabuild) {
            stackCount = mc.player.getMainHandItem().getCount();
        }
        int loopCount = Math.min(stackCount, ctx.loopCount);
        for (int i = 0; i < loopCount; ++i) {
            ctx.loopIndex = i;
            int protocolRawValue = multiStageAdapter.hao$toProtocolValueLoop(ctx);
            Vec3 protocolHitVec = EasyPlaceExtraProtocolHelper.encodeProtocolValueToHitVecZ(protocolRawValue, hitVec);
            BlockHitResult hitResult = new BlockHitResult(protocolHitVec, Direction.UP, pos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
            ctx.stateClient = mc.level.getBlockState(pos);
        }
        if (loopCount > 0) {
            EasyPlaceUtilsInvoker.invokeCacheEasyPlacePosition(pos);
            System.out.println("[hao-easyplace] [多阶段补放] " + pos + " 层数=" + loopCount
                    + " 目标=" + stateSchematic);
        }
    }

    }

package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import carpet_hao_addition.easyplace.ClientEasyPlaceProtocolHelper;
import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.block.CoralWallFanBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
//?<= 1.20.6 ? import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(WorldUtils.class)
public abstract class WorldUtilsMixin {
    //?<= 1.20.6 ? @Shadow private static void cacheEasyPlacePosition(BlockPos pos) {}

    @Inject(
            method = "applyCarpetProtocolHitVec",
            at = @At(value = "RETURN"),
            require = 0,
            cancellable = true)
    private static void hao_replaceHitPos(BlockPos pos, BlockState state, Vec3d hitVecIn, CallbackInfoReturnable<Vec3d> cir) {
        if (BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
            Vec3d out = ClientEasyPlaceProtocolHelper.encodeHitPosItemData(cir.getReturnValue(), pos, state);
            System.out.println("[hao-easyplace] [CarpetVec] pos=" + pos + " in=" + cir.getReturnValue() + " out=" + out);
            cir.setReturnValue(out);
        }
    }

    /** 记录本次投影放置的位置/状态/hitVec, 供 doEasyPlaceAction 返回后补放多阶段方块。 */
    @Unique
    private static BlockPos hao$msPos = null;
    @Unique
    private static BlockState hao$msSchematic = null;
    @Unique
    private static Vec3d hao$msHitVec = null;

    @Inject(
            method = "applyPlacementProtocolV3",
            at = @At(value = "RETURN"),
            require = 0,
            cancellable = true)
    private static void hao_replaceHitPosV3(BlockPos pos, BlockState state, Vec3d hitVecIn, CallbackInfoReturnable<Vec3d> cir) {
        if (!BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
            return;
        }
        Vec3d encoded = ClientEasyPlaceProtocolHelper.encodeHitPosItemData(cir.getReturnValue(), pos, state);
        System.out.println("[hao-easyplace] [V3] pos=" + pos + " state=" + state
                + " litematica返回=" + cir.getReturnValue() + " 我们编码后=" + encoded);
        hao$msPos = pos;
        hao$msSchematic = state;
        hao$msHitVec = encoded;
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
    private static void hao$completeMultiStageBlocks(MinecraftClient mc, CallbackInfoReturnable<ActionResult> cir) {
        BlockPos pos = hao$msPos;
        BlockState stateSchematic = hao$msSchematic;
        Vec3d hitVec = hao$msHitVec;
        hao$msPos = null;
        hao$msSchematic = null;
        hao$msHitVec = null;

        if (pos == null || stateSchematic == null || hitVec == null || mc.world == null || mc.player == null) {
            return;
        }
        if (!(BetterEasyPlaceProtocolHandler.getAdapter(stateSchematic.getBlock())
                instanceof MultiStageBlockProtocolStateAdapter multiStageAdapter)) {
            return;
        }

        MultiStageBlockProtocolStateAdapter.LoopContext ctx = new MultiStageBlockProtocolStateAdapter.LoopContext();
        ctx.stateSchematic = stateSchematic;
        ctx.stateClient = mc.world.getBlockState(pos);
        multiStageAdapter.hao$setLoopCount(ctx);

        int stackCount = Integer.MAX_VALUE;
        if (!mc.player.getAbilities().creativeMode) {
            stackCount = mc.player.getMainHandStack().getCount();
        }
        int loopCount = Math.min(stackCount, ctx.loopCount);
        for (int i = 0; i < loopCount; ++i) {
            ctx.loopIndex = i;
            int protocolRawValue = multiStageAdapter.hao$toProtocolValueLoop(ctx);
            Vec3d protocolHitVec = EasyPlaceExtraProtocolHelper.encodeProtocolValueToHitVecZ(protocolRawValue, hitVec);
            BlockHitResult hitResult = new BlockHitResult(protocolHitVec, Direction.UP, pos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            ctx.stateClient = mc.world.getBlockState(pos);
        }
        if (loopCount > 0) {
            EasyPlaceUtilsInvoker.invokeCacheEasyPlacePosition(pos);
            System.out.println("[hao-easyplace] [多阶段补放] " + pos + " 层数=" + loopCount
                    + " 目标=" + stateSchematic);
        }
    }

    /**
     * 修正珊瑚墙扇的点击面:原版按 {@code facing = side.getOpposite()} 决定贴哪面墙,
     * 所以 side 要传 {@code facing.getOpposite()};blockPos 保持 litematica 给的原值(它是协议解码基准)。
     */
    @WrapOperation(
            method = "doEasyPlaceAction",
            at = @At(value = "NEW", target = "(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;Z)Lnet/minecraft/util/hit/BlockHitResult;"),
            require = 0)
    private static BlockHitResult hao$fixWallFanHitResult(Vec3d hitVec, Direction side, BlockPos blockPos,
                                                          boolean insideBlock, Operation<BlockHitResult> original) {
        BlockState schematic = hao$msSchematic;
        BlockPos fanPos = hao$msPos;
        if (schematic == null || fanPos == null
                || !(schematic.getBlock() instanceof CoralWallFanBlock
                     || schematic.getBlock() instanceof net.minecraft.block.DeadCoralWallFanBlock)) {
            return original.call(hitVec, side, blockPos, insideBlock);
        }
        net.minecraft.state.property.Property<Direction> facingProperty =
                EasyPlaceExtraProtocolHelper.getFirstDirectionProperty(schematic);
        if (facingProperty == null) {
            return original.call(hitVec, side, blockPos, insideBlock);
        }
        Direction facing = schematic.get(facingProperty);
        // blockPos 不能动(协议解码基准);side 要取反向(原版会再取一次)。
        Direction attachSide = facing.getOpposite();
        System.out.println("[hao-easyplace] [墙扇修正] 扇=" + fanPos + " 墙=" + fanPos.offset(facing)
                + " side=" + attachSide + " (期望 facing=" + facing
                + ", litematica 原为 " + blockPos + "/" + side + ")");
        return new BlockHitResult(hitVec, attachSide, blockPos, false);
    }

}

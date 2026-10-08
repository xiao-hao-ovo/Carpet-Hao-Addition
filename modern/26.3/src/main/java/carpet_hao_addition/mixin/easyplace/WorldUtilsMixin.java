package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.HaoDebug;

import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
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
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldUtils.class)
public abstract class WorldUtilsMixin {

    /**
     * 多阶段方块(雪层/海泡菜)的循环放置:原版一次只放一层,需连续调用多次才能堆到投影层数。
     * 用缓存的参数在 doEasyPlaceAction 返回后补放(不依赖 litematica 的局部变量表)。
     * 缓存由 {@link EasyPlaceUtilsMixin} 在协议钩子里写入。
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
            HaoDebug.log("[hao-easyplace] [多阶段补放] " + pos + " 层数=" + loopCount
                    + " 目标=" + stateSchematic);
        }
    }

    /**
     * 修正珊瑚墙扇的点击面:原版按 {@code facing = side.getOpposite()} 决定贴哪面墙,
     * 所以 side 要传 {@code facing.getOpposite()};blockPos 保持 litematica 给的原值(它是协议解码基准)。
     * <p>
     * 26.x 里活/失活珊瑚墙扇都归到 {@link BaseCoralWallFanBlock}(没有单独的 DeadCoralWallFanBlock)。
     */
    @WrapOperation(
            method = "doEasyPlaceAction",
            at = @At(value = "NEW", target = "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/phys/BlockHitResult;"),
            require = 0)
    private static BlockHitResult hao$fixWallFanHitResult(Vec3 hitVec, Direction side, BlockPos blockPos,
                                                          boolean insideBlock, Operation<BlockHitResult> original) {
        BlockState schematic = EasyPlacePendingPlacement.schematic;
        BlockPos fanPos = EasyPlacePendingPlacement.pos;
        if (schematic == null || fanPos == null
                || !(schematic.getBlock() instanceof BaseCoralWallFanBlock)) {
            return original.call(hitVec, side, blockPos, insideBlock);
        }
        Property<Direction> facingProperty = EasyPlaceExtraProtocolHelper.getFirstDirectionProperty(schematic);
        if (facingProperty == null) {
            return original.call(hitVec, side, blockPos, insideBlock);
        }
        Direction facing = schematic.getValue(facingProperty);
        // blockPos 不能动(协议解码基准);side 要取反向(原版会再取一次)。
        Direction attachSide = facing.getOpposite();
        HaoDebug.log("[hao-easyplace] [墙扇修正] 扇=" + fanPos + " 墙=" + fanPos.relative(facing)
                + " side=" + attachSide + " (期望 facing=" + facing
                + ", litematica 原为 " + blockPos + "/" + side + ")");
        return new BlockHitResult(hitVec, attachSide, blockPos, false);
    }

}

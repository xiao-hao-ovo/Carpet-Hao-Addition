package carpet_hao_addition.mixin.easyplace;


import carpet_hao_addition.easyplace.ProjectionPlacement;
import carpet_hao_addition.easyplace.SchematicPlacementEncoder;
import carpet_hao_addition.easyplace.PlacementBitTools;
import carpet_hao_addition.easyplace.PlacementCodec;
import carpet_hao_addition.easyplace.PlacementRules;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
//?<= 1.20.6 ? import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(WorldUtils.class)
public abstract class SchematicHitVecMixin {
    //?<= 1.20.6 ? @Shadow private static void cacheEasyPlacePosition(BlockPos pos) {}

    @Inject(
            method = "applyCarpetProtocolHitVec",
            at = @At(value = "RETURN"),
            require = 0,
            cancellable = true)
    private static void hao$encodeProtocolBits(BlockPos pos, BlockState state, Vec3 hitVecIn, CallbackInfoReturnable<Vec3> cir) {
        if (ProjectionPlacement.enabled()) {
            Vec3 out = SchematicPlacementEncoder.encodeHitPosItemData(cir.getReturnValue(), pos, state);
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
    private static Vec3 hao$msHitVec = null;

    @Inject(
            method = "applyPlacementProtocolV3",
            at = @At(value = "RETURN"),
            require = 0,
            cancellable = true)
    private static void hao$encodeProtocolBitsV3(BlockPos pos, BlockState state, Vec3 hitVecIn, CallbackInfoReturnable<Vec3> cir) {
        if (!ProjectionPlacement.enabled()) {
            return;
        }
        Vec3 encoded = SchematicPlacementEncoder.encodeHitPosItemData(cir.getReturnValue(), pos, state);
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
    private static void hao$completeMultiStageBlocks(Minecraft mc, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos pos = hao$msPos;
        BlockState stateSchematic = hao$msSchematic;
        Vec3 hitVec = hao$msHitVec;
        hao$msPos = null;
        hao$msSchematic = null;
        hao$msHitVec = null;

        if (pos == null || stateSchematic == null || hitVec == null || mc.level == null || mc.player == null) {
            return;
        }
        PlacementCodec codec = PlacementRules.codec(stateSchematic.getBlock());
        if (codec == null || !codec.multiStage()) {
            return;
        }

        int stackCount = Integer.MAX_VALUE;
        if (!mc.player.getAbilities().instabuild) {
            stackCount = mc.player.getMainHandItem().getCount();
        }
        int loopCount = Math.min(stackCount, codec.missingSteps(stateSchematic, mc.level.getBlockState(pos)));
        for (int i = 0; i < loopCount; ++i) {
            int stepBits = codec.stepBits(stateSchematic, mc.level.getBlockState(pos), i);
            Vec3 protocolHitVec = PlacementBitTools.bitsToHitVec(stepBits, hitVec);
            BlockHitResult hitResult = new BlockHitResult(protocolHitVec, Direction.UP, pos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        }
        if (loopCount > 0) {
            SchematicUtilsInvoker.invokeCacheEasyPlacePosition(pos);
            System.out.println("[hao-easyplace] [多阶段补放] " + pos + " 层数=" + loopCount
                    + " 目标=" + stateSchematic);
        }
    }
}

package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.PendingMultiStage;
import carpet_hao_addition.easyplace.PlacementBitTools;
import carpet_hao_addition.easyplace.PlacementCodec;
import carpet_hao_addition.easyplace.PlacementRules;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 多阶段方块(雪层/海泡菜)的循环补放：原版一次只放一层，需连续调用多次才能堆到投影层数。
 * <p>
 * 这个方法在 Litematica 的 WorldUtils 里（不在 EasyPlaceUtils），所以单独一个 mixin。
 * 暂存参数由编码端写入 PendingMultiStage，这里取出后补放。
 */
@Mixin(WorldUtils.class)
public abstract class WorldActionMixin {
    @Inject(
            method = "doEasyPlaceAction",
            at = @At(value = "RETURN"),
            require = 1)
    private static void hao$completeMultiStageBlocks(Minecraft mc, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos pos = PendingMultiStage.pos();
        BlockState stateSchematic = PendingMultiStage.schematic();
        Vec3 hitVec = PendingMultiStage.hitVec();

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
            SchematicUtilsInvoker.hao$cachePlacement(pos);
            System.out.println("[hao-easyplace] [multi-stage-fill] " + pos + " steps=" + loopCount
                    + " target=" + stateSchematic);
        }
    }
}

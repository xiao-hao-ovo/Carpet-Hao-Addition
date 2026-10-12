package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import carpet_hao_addition.easyplace.SchematicPlacementEncoder;
import carpet_hao_addition.easyplace.PlacementBitTools;
import carpet_hao_addition.easyplace.PlacementCodec;
import carpet_hao_addition.easyplace.PendingMultiStage;
import carpet_hao_addition.easyplace.PlacementRules;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.util.EasyPlaceUtils;
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
@Mixin(EasyPlaceUtils.class)
public abstract class SchematicHitVecMixin {
    //?<= 1.20.6 ? @Shadow private static void cacheEasyPlacePosition(BlockPos pos) {}

    @Inject(
            method = "applyCarpetProtocolHitVec",
            at = @At(value = "RETURN"),
            require = 1,
            cancellable = true)
    private static void hao$encodeProtocolBits(BlockPos pos, BlockState state, Vec3 hitVecIn, CallbackInfoReturnable<Vec3> cir) {
        if (ProjectionPlacement.ruleEnabled()) {
            Vec3 out = SchematicPlacementEncoder.encodeSchematicItemData(cir.getReturnValue(), pos, state);
            System.out.println("[hao-easyplace] [CarpetVec] pos=" + pos + " in=" + cir.getReturnValue() + " out=" + out);
            cir.setReturnValue(out);
        }
    }

    @Inject(
            method = "applyPlacementProtocolV3",
            at = @At(value = "RETURN"),
            require = 1,
            cancellable = true)
    private static void hao$encodeProtocolBitsV3(BlockPos pos, BlockState state, Vec3 hitVecIn, CallbackInfoReturnable<Vec3> cir) {
        if (!ProjectionPlacement.ruleEnabled()) {
            return;
        }
        Vec3 encoded = SchematicPlacementEncoder.encodeSchematicItemData(cir.getReturnValue(), pos, state);
        System.out.println("[hao-easyplace] [V3] pos=" + pos + " state=" + state
                + " litematica返回=" + cir.getReturnValue() + " 我们编码后=" + encoded);
        PendingMultiStage.note(pos, state, encoded);
        
        
        cir.setReturnValue(encoded);
    }
}

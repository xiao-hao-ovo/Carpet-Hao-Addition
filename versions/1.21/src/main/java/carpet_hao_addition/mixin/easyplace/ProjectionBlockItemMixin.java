package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.HaoDebug;

import carpet_hao_addition.ProjectionPlacementSettings;
import carpet_hao_addition.easyplace.ProjectionPlacement;
import carpet_hao_addition.easyplace.PendingWaxState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import carpet_hao_addition.easyplace.PlacementBitTools;
import static carpet_hao_addition.easyplace.PlacementBitTools.readBits;
import static carpet_hao_addition.easyplace.PlacementBitTools.hitOffsetX;
import static carpet_hao_addition.easyplace.PlacementBitTools.hitOffsetZ;
import static carpet_hao_addition.easyplace.PlacementBitTools.isProtocolHit;

@Mixin(value = BlockItem.class, priority = 950)
public abstract class ProjectionBlockItemMixin {
    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract boolean canPlace(ItemPlacementContext context, BlockState state);

    /** 服务端写完方块实体数据后【必须主动广播】, 否则客户端看不到。 */
    @Unique
    private static void hao$notifyClient(World level, BlockPos pos) {
        if (level == null || level.isClient()) {
            return;
        }
        BlockState now = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            be.markDirty();
            // 标牌文字靠它自己的 updateListeners 同步,只调 World.updateListeners 不够。
            if (be instanceof SignBlockEntity signBe) {
                ((SignListenerInvoker) signBe).hao$callUpdateListeners();
                HaoDebug.log("[hao-easyplace] [广播刷新] 标牌 updateListeners: " + pos);
            }
        }
        level.updateListeners(pos, now, now, 3);
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void hao$restorePlacementState(ItemPlacementContext context, CallbackInfoReturnable<BlockState> cir) {
        double relativeHitZ = hitOffsetZ(context.getHitPos(), PlacementBitTools.clickedPos(context));
        HaoDebug.log("[hao-easyplace] [getPlacementState] 规则="
                + ProjectionPlacement.enabled()
                + " hitPos=" + context.getHitPos() + " blockPos=" + PlacementBitTools.clickedPos(context)
                + " relZ=" + String.format("%.3f", relativeHitZ)
                + " isProtocol=" + isProtocolHit(relativeHitZ)
                + " block=" + this.getBlock());
        if (!ProjectionPlacement.enabled()) return;
        if (!isProtocolHit(relativeHitZ)) return;

        BlockState baseState = cir.getReturnValue();
        if (baseState == null) {
            baseState = this.getBlock().getPlacementState(context);
        }
        BlockState state = ProjectionPlacement.decodeSchematicState(this.getBlock(), context, baseState);
        HaoDebug.log("[hao-easyplace]   [放置解码] hitPos=" + context.getHitPos()
                + " blockPos=" + PlacementBitTools.clickedPos(context) + " side=" + context.getSide()
                + " 还原状态=" + state);
        if (state == null) {
            cir.setReturnValue(null);
            return;
        }
        if (!this.canPlace(context, state)) return;
        cir.setReturnValue(state);
    }

    @WrapMethod(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;")
    private ActionResult hao$enterPlacementWindow(ItemPlacementContext context, Operation<ActionResult> original) {
        if (ProjectionPlacement.enabled()
                && isProtocolHit(hitOffsetX(context.getHitPos(), PlacementBitTools.clickedPos(context)))) {
            ProjectionPlacement.markProjectedState(true);
            ProjectionPlacement.assignPlaceTargetPos(PlacementBitTools.clickedPos(context));
            ProjectionPlacement.assignPlaceTargetBlock(this.getBlock());
        }
        try {
            return original.call(context);
        } finally {
            ProjectionPlacement.markProjectedState(false);
            ProjectionPlacement.noteWindowFlags(0);
            ProjectionPlacement.assignPlaceTargetPos(BlockPos.ORIGIN);
            ProjectionPlacement.assignPlaceTargetBlock(Blocks.AIR);
        }
    }

    @WrapOperation(
             method = "place",
             at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BlockItem;postPlacement(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/block/BlockState;)Z")
     )
    private boolean hao$applyItemData(BlockItem instance, BlockPos pos, World level, PlayerEntity player, ItemStack stack, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) ItemPlacementContext context)
    {
        // 优先使用「完整 NBT 通道」送来的数据。
        carpet_hao_addition.BlockDataChannel.PendingData data = carpet_hao_addition.BlockDataChannel.take(player, pos);
        if (data != null) {
            net.minecraft.nbt.NbtCompound fullNbt = data.nbt();
            if (fullNbt != null) {
                ItemStack withFullNbt = stack.copy();
                withFullNbt.set(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA,
                        net.minecraft.component.type.NbtComponent.of(fullNbt));
                HaoDebug.log("[hao-easyplace] [服务端] 应用完整 NBT: 位置=" + pos + " id=" + fullNbt.getString("id"));
                boolean hao$ok = original.call(instance, pos, level, player, withFullNbt, state);
                hao$notifyClient(level, pos);
                return hao$ok;
            }
            boolean hao$ok2 = original.call(instance, pos, level, player, stack, state);
            hao$notifyClient(level, pos);
            return hao$ok2;
        }
        HaoDebug.log("[hao-easyplace] [postPlacement] pos=" + pos
                + " side=" + context.getSide() + " state=" + state
                + " 世界该处=" + level.getBlockState(pos));
        ItemStack newStack = ProjectionPlacement.applySchematicItemData(stack, context);
        if (newStack == null) {
            return original.call(instance, pos, level, player, stack, state);
        }
        boolean result = original.call(instance, pos, level, player, newStack, state);
        double relativeHitZ = hitOffsetZ(context.getHitPos(), PlacementBitTools.clickedPos(context));
        int protocolValue = readBits(relativeHitZ);
        if ((protocolValue & 0b100_0000_0000) != 0) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SignBlockEntity sbe) {
                ((PendingWaxState) sbe).hao$setPendingWaxed(true);
            }
        }
        return result;
    }
}

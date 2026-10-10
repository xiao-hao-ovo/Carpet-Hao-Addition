package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.HaoDebug;

import carpet_hao_addition.BetterEasyPlaceProtocolSettings;
import carpet_hao_addition.easyplace.ProjectionPlacement;
import carpet_hao_addition.easyplace.PendingWaxState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

    /** 服务端写完方块实体数据后【必须主动广播】, 否则客户端看不到。 */
    @Unique
    private static void hao$notifyClient(Level level, BlockPos pos) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState now = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            be.setChanged();
            // 标牌文字靠它自己的 updateListeners 同步,只调 Level.updateListeners 不够。
            if (be instanceof SignBlockEntity signBe) {
                ((SignListenerInvoker) signBe).hao$callUpdateListeners();
                HaoDebug.log("[hao-easyplace] [广播刷新] 标牌 updateListeners: " + pos);
            }
        }
        level.sendBlockUpdated(pos, now, now, 3);
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void hao$restorePlacementState(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        double relativeHitZ = hitOffsetZ(context.getClickLocation(), PlacementBitTools.clickedPos(context));
        HaoDebug.log("[hao-easyplace] [getPlacementState] 规则="
                + ProjectionPlacement.enabled()
                + " hitPos=" + context.getClickLocation() + " blockPos=" + PlacementBitTools.clickedPos(context)
                + " relZ=" + String.format("%.3f", relativeHitZ)
                + " isProtocol=" + isProtocolHit(relativeHitZ)
                + " block=" + this.getBlock());
        if (!ProjectionPlacement.enabled()) return;
        if (!isProtocolHit(relativeHitZ)) return;

        BlockState baseState = cir.getReturnValue();
        if (baseState == null) {
            baseState = this.getBlock().getStateForPlacement(context);
        }
        BlockState state = ProjectionPlacement.decodePlacementState(this.getBlock(), context, baseState);
        HaoDebug.log("[hao-easyplace]   [放置解码] hitPos=" + context.getClickLocation()
                + " blockPos=" + PlacementBitTools.clickedPos(context) + " side=" + context.getClickedFace()
                + " 还原状态=" + state);
        if (state == null) {
            cir.setReturnValue(null);
            return;
        }
        if (!this.canPlace(context, state)) return;
        cir.setReturnValue(state);
    }

    @WrapMethod(method = "place(Lnet/minecraft/item/BlockPlaceContext;)Lnet/minecraft/util/InteractionResult;")
    private InteractionResult hao$enterPlacementWindow(BlockPlaceContext context, Operation<InteractionResult> original) {
        if (ProjectionPlacement.enabled()
                && isProtocolHit(hitOffsetX(context.getClickLocation(), PlacementBitTools.clickedPos(context)))) {
            ProjectionPlacement.setEasyPlaceState(true);
            ProjectionPlacement.setPlaceTargetPos(PlacementBitTools.clickedPos(context));
            ProjectionPlacement.setPlaceTargetBlock(this.getBlock());
        }
        try {
            return original.call(context);
        } finally {
            ProjectionPlacement.setEasyPlaceState(false);
            ProjectionPlacement.setPlaceProperty(0);
            ProjectionPlacement.setPlaceTargetPos(BlockPos.ZERO);
            ProjectionPlacement.setPlaceTargetBlock(Blocks.AIR);
        }
    }

    @WrapOperation(
             method = "place",
             at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BlockItem;postPlacement(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/Level;Lnet/minecraft/entity/player/Player;Lnet/minecraft/item/ItemStack;Lnet/minecraft/block/BlockState;)Z")
     )
    private boolean hao$applyItemData(BlockItem instance, BlockPos pos, Level level, Player player, ItemStack stack, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) BlockPlaceContext context)
    {
        // 优先使用「完整 NBT 通道」送来的数据。
        carpet_hao_addition.BlockDataChannel.PendingData data = carpet_hao_addition.BlockDataChannel.take(player, pos);
        if (data != null) {
            net.minecraft.nbt.CompoundTag fullNbt = data.nbt();
            if (fullNbt != null) {
                ItemStack withFullNbt = stack.copy();
                withFullNbt.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                        net.minecraft.world.item.component.TypedEntityData.of(
                                net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                                        net.minecraft.resources.Identifier.tryParse(fullNbt.getString("id").orElse("")))
                                        .map(net.minecraft.core.Holder.Reference::value)
                                        .orElse(null),
                                fullNbt));
                HaoDebug.log("[hao-easyplace] [服务端] 应用完整 NBT: 位置=" + pos + " id=" + fullNbt.getString("id").orElse(""));
                boolean hao$ok = original.call(instance, pos, level, player, withFullNbt, state);
                hao$notifyClient(level, pos);
                return hao$ok;
            }
            boolean hao$ok2 = original.call(instance, pos, level, player, stack, state);
            hao$notifyClient(level, pos);
            return hao$ok2;
        }
        HaoDebug.log("[hao-easyplace] [postPlacement] pos=" + pos
                + " side=" + context.getClickedFace() + " state=" + state
                + " 世界该处=" + level.getBlockState(pos));
        ItemStack newStack = ProjectionPlacement.applyItemStackProtocolData(stack, context);
        if (newStack == null) {
            return original.call(instance, pos, level, player, stack, state);
        }
        boolean result = original.call(instance, pos, level, player, newStack, state);
        double relativeHitZ = hitOffsetZ(context.getClickLocation(), PlacementBitTools.clickedPos(context));
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

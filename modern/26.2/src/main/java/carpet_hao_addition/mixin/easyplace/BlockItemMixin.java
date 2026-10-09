package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.BetterEasyPlaceProtocolSettings;
import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import carpet_hao_addition.easyplace.ISignBlockEntity;
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

import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.decodeProtocolValueFromHitDim;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.getRelativeHitX;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.getRelativeHitZ;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.isProtocol;

@Mixin(value = BlockItem.class, priority = 950)
public abstract class BlockItemMixin {
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
                ((SignBlockEntityInvoker) signBe).hao$callUpdateListeners();
                System.out.println("[hao-easyplace] [广播刷新] 标牌 updateListeners: " + pos);
            }
        }
        level.sendBlockUpdated(pos, now, now, 3);
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void hao_betterEasyPlaceProtocolDecode(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        double relativeHitZ = getRelativeHitZ(context.getClickLocation(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
        System.out.println("[hao-easyplace] [getPlacementState] 规则="
                + BetterEasyPlaceProtocolHandler.isRuleEnabled()
                + " hitPos=" + context.getClickLocation() + " blockPos=" + EasyPlaceExtraProtocolHelper.getClickedPos(context)
                + " relZ=" + String.format("%.3f", relativeHitZ)
                + " isProtocol=" + isProtocol(relativeHitZ)
                + " block=" + this.getBlock());
        if (!BetterEasyPlaceProtocolHandler.isRuleEnabled()) return;
        if (!isProtocol(relativeHitZ)) return;

        BlockState baseState = cir.getReturnValue();
        if (baseState == null) {
            baseState = this.getBlock().getStateForPlacement(context);
        }
        BlockState state = BetterEasyPlaceProtocolHandler.decodePlacementState(this.getBlock(), context, baseState);
        System.out.println("[hao-easyplace]   [放置解码] hitPos=" + context.getClickLocation()
                + " blockPos=" + EasyPlaceExtraProtocolHelper.getClickedPos(context) + " side=" + context.getClickedFace()
                + " 还原状态=" + state);
        if (state == null) {
            cir.setReturnValue(null);
            return;
        }
        if (!this.canPlace(context, state)) return;
        cir.setReturnValue(state);
    }

    @WrapMethod(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;")
    private InteractionResult hao_setPlaceState(BlockPlaceContext context, Operation<InteractionResult> original) {
        if (BetterEasyPlaceProtocolHandler.isRuleEnabled()
                && isProtocol(getRelativeHitX(context.getClickLocation(), EasyPlaceExtraProtocolHelper.getClickedPos(context)))) {
            BetterEasyPlaceProtocolHandler.setEasyPlaceState(true);
            BetterEasyPlaceProtocolHandler.setPlaceTargetPos(EasyPlaceExtraProtocolHelper.getClickedPos(context));
            BetterEasyPlaceProtocolHandler.setPlaceTargetBlock(this.getBlock());
        }
        try {
            InteractionResult hao$result = original.call(context);
            return hao$result;
        } finally {
            BetterEasyPlaceProtocolHandler.setEasyPlaceState(false);
            BetterEasyPlaceProtocolHandler.setPlaceProperty(0);
            BetterEasyPlaceProtocolHandler.setPlaceTargetPos(BlockPos.ZERO);
            BetterEasyPlaceProtocolHandler.setPlaceTargetBlock(Blocks.AIR);
        }
    }

    @WrapOperation(
             method = "place",
             at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;updateCustomBlockEntityTag(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Z")
     )
    private boolean hao_betterEasyPlaceProtocolItemStack(BlockItem instance, BlockPos pos, Level level, Player player, ItemStack stack, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) BlockPlaceContext context)
    {
        // 优先使用「完整 NBT 通道」送来的数据。
        carpet_hao_addition.EasyPlaceNbtHandler.PendingData data = carpet_hao_addition.EasyPlaceNbtHandler.take(player, pos);
        if (data != null) {
            net.minecraft.nbt.CompoundTag fullNbt = data.nbt();
            // 类型取该位置实际落下的方块实体,原版 updateCustomBlockEntityTag 会核对它。
            BlockEntity hao$placedBe = level.getBlockEntity(pos);
            if (fullNbt != null && hao$placedBe != null) {
                ItemStack withFullNbt = stack.copy();
                withFullNbt.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                        net.minecraft.world.item.component.TypedEntityData.of(hao$placedBe.getType(), fullNbt));
                System.out.println("[hao-easyplace] [服务端] 应用完整 NBT: 位置=" + pos + " id=" + fullNbt.getString("id"));
                boolean hao$ok = original.call(instance, pos, level, player, withFullNbt, state);
                hao$notifyClient(level, pos);
                return hao$ok;
            }
            boolean hao$ok2 = original.call(instance, pos, level, player, stack, state);
            hao$notifyClient(level, pos);
            return hao$ok2;
        }
        System.out.println("[hao-easyplace] [postPlacement] pos=" + pos
                + " side=" + context.getClickedFace() + " state=" + state
                + " 世界该处=" + level.getBlockState(pos));
        ItemStack newStack = BetterEasyPlaceProtocolHandler.applyItemStackProtocolData(stack, context);
        if (newStack == null) {
            return original.call(instance, pos, level, player, stack, state);
        }
        boolean result = original.call(instance, pos, level, player, newStack, state);
        double relativeHitZ = getRelativeHitZ(context.getClickLocation(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
        int protocolValue = decodeProtocolValueFromHitDim(relativeHitZ);
        if ((protocolValue & 0b100_0000_0000) != 0) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SignBlockEntity sbe) {
                ((ISignBlockEntity) sbe).hao$setPendingWaxed(true);
            }
        }
        return result;
    }
}

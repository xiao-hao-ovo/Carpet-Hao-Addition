package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.BetterEasyPlaceProtocolSettings;
import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import carpet_hao_addition.easyplace.ISignBlockEntity;
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

import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.decodeProtocolValueFromHitDim;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.getRelativeHitX;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.getRelativeHitZ;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.isProtocol;

@Mixin(value = BlockItem.class, priority = 950)
public abstract class BlockItemMixin {
    @Shadow
    public abstract Block getBlock();

    /** 珊瑚类待强制的目标: 由 postPlacement 记录, 在 place 流程【全部结束】后统一设置。 */
    @Unique
    private static BlockPos hao$coralPos = null;
    @Unique
    private static BlockState hao$coralState = null;

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
                ((SignBlockEntityInvoker) signBe).hao$callUpdateListeners();
                System.out.println("[hao-easyplace] [广播刷新] 标牌 updateListeners: " + pos);
            }
        }
        level.updateListeners(pos, now, now, 3);
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void hao_betterEasyPlaceProtocolDecode(ItemPlacementContext context, CallbackInfoReturnable<BlockState> cir) {
        double relativeHitZ = getRelativeHitZ(context.getHitPos(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
        System.out.println("[hao-easyplace] [getPlacementState] 规则="
                + BetterEasyPlaceProtocolHandler.isRuleEnabled()
                + " hitPos=" + context.getHitPos() + " blockPos=" + EasyPlaceExtraProtocolHelper.getClickedPos(context)
                + " relZ=" + String.format("%.3f", relativeHitZ)
                + " isProtocol=" + isProtocol(relativeHitZ)
                + " block=" + this.getBlock());
        if (!BetterEasyPlaceProtocolHandler.isRuleEnabled()) return;
        if (!isProtocol(relativeHitZ)) return;

        BlockState baseState = cir.getReturnValue();
        if (baseState == null) {
            baseState = this.getBlock().getPlacementState(context);
        }
        BlockState state = BetterEasyPlaceProtocolHandler.decodePlacementState(this.getBlock(), context, baseState);
        System.out.println("[hao-easyplace]   [放置解码] hitPos=" + context.getHitPos()
                + " blockPos=" + EasyPlaceExtraProtocolHelper.getClickedPos(context) + " side=" + context.getSide()
                + " 还原状态=" + state);
        if (state == null) {
            cir.setReturnValue(null);
            return;
        }
        if (!this.canPlace(context, state)) return;
        cir.setReturnValue(state);
    }

    @WrapMethod(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;")
    private ActionResult hao_setPlaceState(ItemPlacementContext context, Operation<ActionResult> original) {
        if (BetterEasyPlaceProtocolHandler.isRuleEnabled()
                && isProtocol(getRelativeHitX(context.getHitPos(), EasyPlaceExtraProtocolHelper.getClickedPos(context)))) {
            BetterEasyPlaceProtocolHandler.setEasyPlaceState(true);
            BetterEasyPlaceProtocolHandler.setPlaceTargetPos(EasyPlaceExtraProtocolHelper.getClickedPos(context));
            BetterEasyPlaceProtocolHandler.setPlaceTargetBlock(this.getBlock());
        }
        try {
            ActionResult hao$result = original.call(context);
            // 珊瑚类:整个放置流程走完后再强制设置(在 postPlacement 里做会被后续的
            // placeFromNbt 覆盖一次)。
            if (hao$coralState != null && hao$coralPos != null && !context.getWorld().isClient()) {
                World hao$w = context.getWorld();
                BlockState hao$now = hao$w.getBlockState(hao$coralPos);
                if (!hao$now.equals(hao$coralState)) {
                    hao$w.setBlockState(hao$coralPos, hao$coralState, 3);
                    System.out.println("[hao-easyplace] [珊瑚强制投影状态/末端] " + hao$coralPos
                            + " " + hao$now + " -> " + hao$coralState);
                }
            }
            hao$coralPos = null;
            hao$coralState = null;
            return hao$result;
        } finally {
            BetterEasyPlaceProtocolHandler.setEasyPlaceState(false);
            BetterEasyPlaceProtocolHandler.setPlaceProperty(0);
            BetterEasyPlaceProtocolHandler.setPlaceTargetPos(BlockPos.ORIGIN);
            BetterEasyPlaceProtocolHandler.setPlaceTargetBlock(Blocks.AIR);
        }
    }

    @WrapOperation(
             method = "place",
             at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BlockItem;postPlacement(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/block/BlockState;)Z")
     )
    private boolean hao_betterEasyPlaceProtocolItemStack(BlockItem instance, BlockPos pos, World level, PlayerEntity player, ItemStack stack, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) ItemPlacementContext context)
    {
        // 优先使用「完整 NBT 通道」送来的数据。
        carpet_hao_addition.EasyPlaceNbtHandler.PendingData data = carpet_hao_addition.EasyPlaceNbtHandler.take(player, pos);
        if (data != null) {
            // 只有珊瑚类会带 stateNbt,按投影强制设置(不受 canPlaceAt 约束);
            // 其他方块恒为 null,不执行这里,行为不变。
            if (false && !level.isClient() && data.stateNbt() != null) { // 老层无 NbtHelper.toBlockState
                // 这里只记录,不直接设置 —— 原版 place 在这之后还会再设一次,会把它覆盖掉。
                try {
                    BlockState target = net.minecraft.nbt.NbtHelper.toBlockState(
                            level.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.BLOCK),
                            data.stateNbt());
                    if (target != null) {
                        hao$coralPos = pos;
                        hao$coralState = target;
                        System.out.println("[hao-easyplace] [珊瑚] 记录待强制: " + pos + " -> " + target);
                    }
                } catch (Exception e) {
                    System.out.println("[hao-easyplace] [珊瑚] 解析投影状态失败: " + e);
                }
            }
            net.minecraft.nbt.NbtCompound fullNbt = data.nbt();
            if (fullNbt != null) {
                ItemStack withFullNbt = stack.copy();
                withFullNbt.set(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA,
                        net.minecraft.component.type.NbtComponent.of(fullNbt));
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
                + " side=" + context.getSide() + " state=" + state
                + " 世界该处=" + level.getBlockState(pos));
        ItemStack newStack = BetterEasyPlaceProtocolHandler.applyItemStackProtocolData(stack, context);
        if (newStack == null) {
            return original.call(instance, pos, level, player, stack, state);
        }
        boolean result = original.call(instance, pos, level, player, newStack, state);
        double relativeHitZ = getRelativeHitZ(context.getHitPos(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
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

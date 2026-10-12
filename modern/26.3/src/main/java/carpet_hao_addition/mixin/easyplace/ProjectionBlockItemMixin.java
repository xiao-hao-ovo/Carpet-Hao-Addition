package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.HaoDebug;

import carpet_hao_addition.ProjectionPlacementSettings;
import carpet_hao_addition.easyplace.ProjectionPlacement;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import carpet_hao_addition.easyplace.PlacementBitTools;
import carpet_hao_addition.easyplace.SignColorMemory;
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
            // 标牌文字靠它自己的更新广播同步,只调 Level.updateListeners 不够。
            if (be instanceof SignBlockEntity signBe) {
                ((SignListenerInvoker) signBe).hao$callMarkUpdated();
                HaoDebug.log("[hao-easyplace] [广播刷新] 标牌 updateListeners: " + pos);
            }
        }
        level.sendBlockUpdated(pos, now, now, 3);
    }

    // ==================== 标牌文字颜色 / 发光 ====================
    // 1.21.8 靠投影 NBT 里的 color 字符串就能生效;26.x 的标牌方块实体改成了组件读写,
    // 认不出那份字符串格式(实测文字能还原、颜色丢失)。所以这里用 26.x 的 API 主动设定,
    // 并记一笔到 SignColorMemory,由 SignBlockEntity_colorProbeMixin 在后续 NBT 覆盖时补回。

    /** 按投影 NBT 里的 front_text/back_text 设置颜色与发光。 */
    @Unique
    private static void hao$applySignColors(Level level, BlockPos pos, CompoundTag fullNbt) {
        // 客户端(预测放置那一次)与服务端各设一次:只设服务端时客户端要靠 BE 同步才看得到,
        // 实测同步过不来,放出来仍是黑字。
        if (fullNbt == null || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return;
        }
        hao$applySignText(level, sign, fullNbt, "front_text", SignTextSlot.FRONT);
        hao$applySignText(level, sign, fullNbt, "back_text", SignTextSlot.BACK);
    }

    /** 把投影 NBT 里某一面(文字/color/has_glowing_text)的颜色与发光设到标牌上。 */
    @Unique
    private static void hao$applySignText(Level level, SignBlockEntity sign, CompoundTag fullNbt,
                                          String key, SignTextSlot slot) {
        CompoundTag text = fullNbt.getCompound(key).orElse(null);
        if (text == null) {
            return;
        }
        boolean glowing = text.getBoolean("has_glowing_text").orElse(false);
        String colorName = text.getString("color").orElse("");
        SignText current = sign.getText(slot);
        SignText updated = current.withGlowingText(glowing);
        if (!colorName.isEmpty()) {
            // DyeColor.byName 找不到时回退到当前颜色(而不是黑),避免把好颜色改坏。
            DyeColor want = DyeColor.byName(colorName, current.getColor());
            updated = updated.withColor(want);
            // 记下来:后面会有"文字在、颜色黑"的 NBT 把这个颜色冲掉,由颜色自愈 mixin 补回。
            SignColorMemory.remember(level.isClientSide(), sign.getBlockPos(), slot.name(), want);
        }
        sign.setText(updated, slot);
        HaoDebug.log("[hao-sign] set(" + (level.isClientSide() ? "CLIENT" : "SERVER") + ") " + key
                + " color=" + colorName + "->" + sign.getText(slot).getColor().getName()
                + " pos=" + sign.getBlockPos());
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // 标牌自身的数据包有时不触发,直接把这份 BE 数据强制广播出去。
            ClientboundBlockEntityDataPacket packet = sign.getUpdatePacket();
            if (packet != null) {
                serverLevel.getServer().getPlayerList().broadcastAll(packet);
            }
        }
    }

    /**
     * 按协议位设置标牌颜色/发光。
     * 位定义与 SignItemData 一致:bit5=正面发光, bit6-9=正面颜色 ordinal,
     * bit11=背面发光, bit12-15=背面颜色 ordinal。
     */
    @Unique
    private static void hao$applySignColorsFromProtocol(Level level, SignBlockEntity sign, int protocolValue) {
        DyeColor[] colors = DyeColor.values();
        int frontOrdinal = (protocolValue >>> 6) & 0b1111;
        int backOrdinal = (protocolValue >>> 12) & 0b1111;
        boolean frontGlowing = (protocolValue & 0b10_0000) != 0;
        boolean backGlowing = (protocolValue & 0b1000_0000_0000) != 0;
        hao$setSignColor(level, sign, SignTextSlot.FRONT,
                frontOrdinal < colors.length ? colors[frontOrdinal] : DyeColor.BLACK, frontGlowing);
        hao$setSignColor(level, sign, SignTextSlot.BACK,
                backOrdinal < colors.length ? colors[backOrdinal] : DyeColor.BLACK, backGlowing);
    }

    /** 把颜色/发光设到标牌某一面;服务端再强制广播一次。 */
    @Unique
    private static void hao$setSignColor(Level level, SignBlockEntity sign, SignTextSlot slot,
                                         DyeColor color, boolean glowing) {
        SignText updated = sign.getText(slot).withColor(color).withGlowingText(glowing);
        sign.setText(updated, slot);
        SignColorMemory.remember(level.isClientSide(), sign.getBlockPos(), slot.name(), color);
        HaoDebug.log("[hao-sign] set(" + (level.isClientSide() ? "CLIENT" : "SERVER") + ") " + slot
                + " color->" + sign.getText(slot).getColor().getName() + " pos=" + sign.getBlockPos());
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            ClientboundBlockEntityDataPacket packet = sign.getUpdatePacket();
            if (packet != null) {
                serverLevel.getServer().getPlayerList().broadcastAll(packet);
            }
        }
    }

    /**
     * 两条通道(完整 NBT / 协议值)共用的标牌颜色修复。
     * 先按投影 NBT 里的字符串 color 设;协议位再兜一次 —— 只有协议里真的编过标牌属性
     * (bit5 以上非 0)时才兜,免得把颜色误设成 ordinal 0 的白色。
     */
    @Unique
    private static void hao$fixSignColors(Level level, BlockPos pos, int protocolValue, CompoundTag fullNbt) {
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return;
        }
        HaoDebug.log("[hao-sign] fix pos=" + pos + " side=" + (level.isClientSide() ? "CLIENT" : "SERVER")
                + " protocolValue=0b" + Integer.toBinaryString(protocolValue)
                + " 带NBT=" + (fullNbt != null));
        if (fullNbt != null) {
            hao$applySignColors(level, pos, fullNbt);
        }
        if ((protocolValue >>> 5) != 0) {
            hao$applySignColorsFromProtocol(level, sign, protocolValue);
        }
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void hao$restorePlacementState(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        double relativeHitZ = hitOffsetZ(context.getClickLocation(), PlacementBitTools.clickedPos(context));
        HaoDebug.log("[hao-easyplace] [getPlacementState] 规则="
                + ProjectionPlacement.ruleEnabled()
                + " hitPos=" + context.getClickLocation() + " blockPos=" + PlacementBitTools.clickedPos(context)
                + " relZ=" + String.format("%.3f", relativeHitZ)
                + " isProtocol=" + isProtocolHit(relativeHitZ)
                + " block=" + this.getBlock());
        if (!ProjectionPlacement.ruleEnabled()) return;
        if (!isProtocolHit(relativeHitZ)) return;

        BlockState baseState = cir.getReturnValue();
        if (baseState == null) {
            baseState = this.getBlock().getStateForPlacement(context);
        }
        BlockState state = ProjectionPlacement.decodeSchematicState(this.getBlock(), context, baseState);
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

    @WrapMethod(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;")
    private InteractionResult hao$enterPlacementWindow(BlockPlaceContext context, Operation<InteractionResult> original) {
        if (ProjectionPlacement.ruleEnabled()
                && isProtocolHit(hitOffsetX(context.getClickLocation(), PlacementBitTools.clickedPos(context)))) {
            ProjectionPlacement.markProjectedState(true);
            ProjectionPlacement.assignPlaceTargetPos(PlacementBitTools.clickedPos(context));
            ProjectionPlacement.assignPlaceTargetBlock(this.getBlock());
        }
        try {
            return original.call(context);
        } finally {
            ProjectionPlacement.markProjectedState(false);
            ProjectionPlacement.noteWindowFlags(0);
            ProjectionPlacement.assignPlaceTargetPos(BlockPos.ZERO);
            ProjectionPlacement.assignPlaceTargetBlock(Blocks.AIR);
        }
    }

    @WrapOperation(
             method = "place",
             at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;updateCustomBlockEntityTag(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Z")
     )
    private static boolean hao$applyItemData(Level level, Player player, BlockPos pos, ItemStack stack,
                                             Operation<Boolean> original,
                                             @Local(argsOnly = true) BlockPlaceContext context) {
        int protocolValue = readBits(hitOffsetZ(context.getClickLocation(), PlacementBitTools.clickedPos(context)));
        // 优先使用「完整 NBT 通道」送来的数据。
        carpet_hao_addition.BlockDataChannel.PendingData data = carpet_hao_addition.BlockDataChannel.take(player, pos);
        if (data != null) {
            CompoundTag fullNbt = data.nbt();
            boolean hao$ok;
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
                hao$ok = original.call(level, player, pos, withFullNbt);
            } else {
                hao$ok = original.call(level, player, pos, stack);
            }
            // 先按投影 NBT 设标牌颜色/发光,再广播 —— 顺序反了客户端收到的还是旧状态。
            hao$fixSignColors(level, pos, protocolValue, data.nbt());
            hao$notifyClient(level, pos);
            return hao$ok;
        }
        HaoDebug.log("[hao-easyplace] [方块实体数据] pos=" + pos
                + " side=" + context.getClickedFace() + " state=" + level.getBlockState(pos));
        ItemStack newStack = ProjectionPlacement.applySchematicItemData(stack, context);
        boolean result = newStack == null
                ? original.call(level, player, pos, stack)
                : original.call(level, player, pos, newStack);
        hao$fixSignColors(level, pos, protocolValue, null);
        return result;
    }
}

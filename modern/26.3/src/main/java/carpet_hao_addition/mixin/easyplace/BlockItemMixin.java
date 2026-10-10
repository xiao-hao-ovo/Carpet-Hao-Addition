package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.HaoDebug;

import carpet_hao_addition.BetterEasyPlaceProtocolSettings;
import carpet_hao_addition.easyplace.BetterEasyPlaceProtocolHandler;
import carpet_hao_addition.easyplace.ISignBlockEntity;
import carpet_hao_addition.easyplace.SignColorMemory;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            // 标牌文字靠它自己的 markUpdated 同步,只调 Level.updateListeners 不够。
            if (be instanceof SignBlockEntity signBe) {
                ((SignBlockEntityInvoker) signBe).hao$callUpdateListeners();
                HaoDebug.log("[hao-easyplace] [广播刷新] 标牌 markUpdated: " + pos);
            }
        }
        level.sendBlockUpdated(pos, now, now, 3);
    }


    /**
     * 标牌文字颜色 / 发光:客户端与服务端都用 26.3 的 API 直接设置。
     * <p>
     * 为什么不直接靠"把投影 NBT 整份写进方块实体":26.x 的标牌 BE 改成了 ValueInput / 组件读写,
     * 投影里那份 {@code front_text}(颜色是字符串 {@code "blue"})它**认不出来** ——
     * 实测文字能还原、颜色丢失(放出来黑字)。所以这里自己解析投影 NBT 里的
     * {@code front_text}/{@code back_text}(color / has_glowing_text),再用
     * {@link SignBlockEntity#setText} + {@code SignText.withColor/withGlowingText} 设上去,
     * 完全不依赖 26.x 的 NBT 字段格式。
     * <p>
     * 协议位(easyplace 编码进 hitVec 的那些)服务端**也是能拿到的** —— 客户端把协议值编进了命中
     * 坐标,服务端在同一次 place 里解出来用(本文件下面那个分支就是拿它判断 waxed 的)。
     * 所以协议值路径的颜色由 {@code hao$applySignColorsFromProtocol} 按同样的位补上。
     */
    @Unique
    private static void hao$applySignColors(Level level, BlockPos pos, net.minecraft.nbt.CompoundTag fullNbt) {
        // **两侧都要做**:1.21.8 是靠客户端把颜色写进 NBT 才生效的;26.x 的标牌 BE 不认那份
        // 字符串格式的 color,所以这里客户端(预测放置时本地那一次)与服务端各设一次 ——
        // 只设服务端时客户端要靠 BE 同步才看得到,实测同步过不来,放出来仍是黑字。
        if (fullNbt == null || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return;
        }
        hao$applySignText(level, sign, fullNbt, "front_text", SignTextSlot.FRONT);
        hao$applySignText(level, sign, fullNbt, "back_text", SignTextSlot.BACK);
    }

    /** 把投影 NBT 里某一面(messages/color/has_glowing_text)的颜色与发光设到标牌上。 */
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
            // 记下来:后面会有"文字在、颜色黑"的 NBT 把这个颜色冲掉,由 SignBlockEntity_colorProbeMixin 补回。
            SignColorMemory.remember(level.isClientSide(), sign.getBlockPos(), slot.name(), want);
        }
        sign.setText(updated, slot);
        HaoDebug.log("[hao-sign] set(" + (level.isClientSide() ? "CLIENT" : "SERVER") + ") " + key
                + " color=" + colorName + "->" + sign.getText(slot).getColor().getName()
                + " pos=" + sign.getBlockPos());
        if (level.isClientSide() == false && level instanceof ServerLevel serverLevel) {
            // 修:标牌的 markUpdated 有时不触发方块实体数据包,结果服务端内存颜色对了、客户端一直黑。
            // 这里直接把这份 BE 数据强制广播出去。
            ClientboundBlockEntityDataPacket packet = sign.getUpdatePacket();
            if (packet != null) {
                serverLevel.getServer().getPlayerList().broadcastAll(packet);
            }
        }
    }

    /**
     * 按协议值里的位设置标牌颜色/发光。
     * <p>
     * 位定义与 {@code StandingSignBlockProtocolAdapter} 保持一致:
     * bit5=正面发光,bit6-9=正面颜色 ordinal,bit10=已打蜡,bit11=背面发光,bit12-15=背面颜色 ordinal。
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

    /** 把颜色/发光设到标牌某一面;服务端再强制广播一次这份 BE 数据。 */
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
     * <p>
     * 先按投影 NBT 里的字符串 color 设;再用协议位兜一次 —— 协议位是客户端从**投影世界的标牌
     * 方块实体**读的,与 NBT 同源,两个都设不会打架;但只有协议里真的编过标牌属性
     * (bit5 以上非 0)时才兜,免得把颜色误设成 ordinal 0 的白色。
     */
    @Unique
    private static void hao$fixSignColors(Level level, BlockPos pos, int protocolValue, CompoundTag fullNbt) {
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            HaoDebug.log("[hao-sign] fix 跳过:该位置不是标牌方块实体 pos=" + pos);
            return;
        }
        HaoDebug.log("[hao-sign] fix pos=" + pos + " side=" + (level.isClientSide() ? "CLIENT" : "SERVER")
                + " protocolValue=0b" + Integer.toBinaryString(protocolValue)
                + " 带NBT=" + (fullNbt != null)
                + " 传入颜色=" + (fullNbt == null ? "-" : fullNbt.getCompound("front_text")
                        .map(t -> t.getString("color").orElse("?")).orElse("无front_text")));
        if (fullNbt != null) {
            hao$applySignColors(level, pos, fullNbt);
        }
        if ((protocolValue >>> 5) != 0) {
            hao$applySignColorsFromProtocol(level, sign, protocolValue);
        }
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void hao$decodePlacementState(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        double relativeHitZ = getRelativeHitZ(context.getClickLocation(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
        HaoDebug.log("[hao-easyplace] [getPlacementState] 规则="
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
        HaoDebug.log("[hao-easyplace]   [放置解码] hitPos=" + context.getClickLocation()
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
    private InteractionResult hao$setPlacementState(BlockPlaceContext context, Operation<InteractionResult> original) {
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

    /**
     * 26.3 起 BlockItem 不再有实例方法 updateCustomBlockEntityTag,place 里改为直接调静态版
     * {@code updateCustomBlockEntityTag(Level, Player, BlockPos, ItemStack)},所以包装的是静态调用。
     */
    @WrapOperation(
             method = "place",
             at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;updateCustomBlockEntityTag(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Z")
     )
    private static boolean hao$applyItemData(Level level, Player player, BlockPos pos, ItemStack stack,
                                                                Operation<Boolean> original,
                                                                @Local(argsOnly = true) BlockPlaceContext context)
    {
        // 优先使用「完整 NBT 通道」送来的数据。
        carpet_hao_addition.EasyPlaceNbtHandler.PendingData data = carpet_hao_addition.EasyPlaceNbtHandler.take(player, pos);
        if (data != null) {
            net.minecraft.nbt.CompoundTag fullNbt = data.nbt();
            // 方块实体数据组件是 TypedEntityData(必须带方块实体类型),不再是裸 NBT 包;
            // 类型取该位置实际落下的方块实体,原版 updateCustomBlockEntityTag 会核对它。
            BlockEntity hao$placedBe = level.getBlockEntity(pos);
            if (fullNbt != null && hao$placedBe != null) {
                ItemStack withFullNbt = stack.copy();
                withFullNbt.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                        net.minecraft.world.item.component.TypedEntityData.of(hao$placedBe.getType(), fullNbt));
                HaoDebug.log("[hao-easyplace] [服务端] 应用完整 NBT: 位置=" + pos + " id=" + fullNbt.getString("id").orElse("?"));
				HaoDebug.log("[hao-sign] 服务端应用 NBT front_text=" + fullNbt.getCompound("front_text").orElse(null));
                boolean hao$ok = original.call(level, player, pos, withFullNbt);
                // 先按投影 NBT 设标牌颜色/发光,再广播 —— 顺序反了客户端收到的还是旧状态。
                int hao$protocolValue = decodeProtocolValueFromHitDim(getRelativeHitZ(context.getClickLocation(),
                        EasyPlaceExtraProtocolHelper.getClickedPos(context)));
                hao$fixSignColors(level, pos, hao$protocolValue, data.nbt());
                hao$notifyClient(level, pos);
                return hao$ok;
            }
            boolean hao$ok2 = original.call(level, player, pos, stack);
            hao$notifyClient(level, pos);
            return hao$ok2;
        }
        HaoDebug.log("[hao-easyplace] [方块实体数据] pos=" + pos
                + " side=" + context.getClickedFace()
                + " 世界该处=" + level.getBlockState(pos));
        ItemStack newStack = BetterEasyPlaceProtocolHandler.applyItemStackProtocolData(stack, context);
        if (newStack == null) {
            return original.call(level, player, pos, stack);
        }
        boolean result = original.call(level, player, pos, newStack);
        double relativeHitZ = getRelativeHitZ(context.getClickLocation(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
        int protocolValue = decodeProtocolValueFromHitDim(relativeHitZ);
        if ((protocolValue & 0b100_0000_0000) != 0) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SignBlockEntity sbe) {
                ((ISignBlockEntity) sbe).hao$setPendingWaxed(true);
            }
        }
        // 这条路径写进 BE 的 front_text 是 1.21.x 那份格式,26.x 的标牌 BE 读回来会丢色
        // (文字在、颜色黑),所以按协议值里的位再设一次颜色 —— 客户端与服务端都会走到。
        hao$fixSignColors(level, pos, protocolValue, null);
        return result;
    }
}

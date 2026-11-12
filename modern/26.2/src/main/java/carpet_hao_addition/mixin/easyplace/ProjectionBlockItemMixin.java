package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.ProjectionPlacementSettings;
import carpet_hao_addition.easyplace.ProjectionPlacement;
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
import carpet_hao_addition.HaoDebug;
import static carpet_hao_addition.easyplace.PlacementBitTools.readBits;
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
                ((SignListenerInvoker) signBe).hao$callMarkUpdated();
                HaoDebug.log("[hao-easyplace] [广播刷新] 标牌 updateListeners: " + pos);
            }
        }
        level.sendBlockUpdated(pos, now, now, 3);
        // 同上：方块实体数据必须显式标记才会同步给客户端。
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(pos);
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
        // 投影给的形状在当前位置立不住（例如坡轨坡顶没有方块）→ 这一格直接放弃。
        // 若交给原版，它会把坡轨重算成平轨，放出来的就不是投影的样子了；
        // 而如果强行保持投影形状，又会放出之后必然掉落的浮空坡轨。
        if (!ProjectionPlacement.stateCanExist(state, context.getLevel(), context.getClickedPos())) {
            HaoDebug.log("[hao-easyplace]   [放弃放置] 状态在当前环境立不住 state=" + state
                    + " pos=" + context.getClickedPos());
            cir.setReturnValue(null);
            return;
        }
        if (!this.canPlace(context, state)) return;
        cir.setReturnValue(state);
    }

    @WrapMethod(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;")
    private InteractionResult hao$enterPlacementWindow(BlockPlaceContext context, Operation<InteractionResult> original) {
        boolean projected = ProjectionPlacement.ruleEnabled()
                && isProtocolHit(hitOffsetZ(context.getClickLocation(), PlacementBitTools.clickedPos(context)));
        if (projected) {
            ProjectionPlacement.markProjectedState(true);
            ProjectionPlacement.assignPlaceTargetPos(PlacementBitTools.clickedPos(context));
            ProjectionPlacement.assignPlaceTargetBlock(this.getBlock());
        }
        // 无条件记录：applyItemData 里要用它读 hit offset，投影协议没命中时也不能是 null。
        ProjectionPlacement.assignPlaceContext(context);
        InteractionResult result;
        try {
            result = original.call(context);
        } finally {
            ProjectionPlacement.markProjectedState(false);
            ProjectionPlacement.noteWindowFlags(0);
            ProjectionPlacement.assignPlaceTargetPos(BlockPos.ZERO);
            ProjectionPlacement.assignPlaceTargetBlock(Blocks.AIR);
            ProjectionPlacement.assignPlaceContext(null);
        }
        // 投影放置后让这一格自己重新检查一次红石状态：先放拉杆、后放铁轨时，
        // 铁轨的 onBlockAdded 不一定重新计算 powered，需要主动邻居更新一次。
        if (projected && result.consumesAction()) {
            BlockPos target = context.getClickedPos();
            BlockState placed = context.getLevel().getBlockState(target);
            if (!placed.isAir()) {
                context.getLevel().updateNeighborsAt(target, placed.getBlock(),
                        (net.minecraft.world.level.redstone.Orientation) null);
            }
        }
        return result;
    }

    // 26.2 的 place 调用的是实例版 updateCustomBlockEntityTag(BlockPos, Level, Player, ItemStack, BlockState)，
    // 不是静态 4 参版 —— 这正是 1.21.8 里 postPlacement 的对应方法。
    // 原来挂在静态版上会扫到 0 个目标，整条方块实体数据链路失效。
    @WrapMethod(method = "updateCustomBlockEntityTag(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Z")
    private boolean hao$applyItemData(BlockPos pos, Level level, Player player, ItemStack stack,
                                      BlockState state, Operation<Boolean> original)
    {
        final BlockPlaceContext context = ProjectionPlacement.currentPlaceContext();
        if (context == null) {
            // 不是从 place 进来的：没有上下文，按原样走。
            return original.call(pos, level, player, stack, state);
        }
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
                boolean hao$ok = original.call(pos, level, player, withFullNbt, state);
                hao$notifyClient(level, pos);
                return hao$ok;
            }
            boolean hao$ok2 = original.call(pos, level, player, stack, state);
            hao$notifyClient(level, pos);
            return hao$ok2;
        }
        HaoDebug.log("[hao-easyplace] [postPlacement] pos=" + pos
                + " side=" + context.getClickedFace() + " state=" + level.getBlockState(pos)
                + " 世界该处=" + level.getBlockState(pos));
        ItemStack newStack = ProjectionPlacement.applySchematicItemData(stack, context);
        if (newStack == null) {
            return original.call(pos, level, player, stack, state);
        }
        boolean result = original.call(pos, level, player, newStack, state);
        // 兜底分支也要广播：客户端预测放置的方块实体没有投影数据，不回传就一直空白。
        hao$notifyClient(level, pos);
        double relativeHitZ = hitOffsetZ(context.getClickLocation(), PlacementBitTools.clickedPos(context));
        int protocolValue = readBits(relativeHitZ);
        return result;
    }
}

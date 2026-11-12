package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.HaoDebug;

import carpet_hao_addition.ProjectionPlacementSettings;
import carpet_hao_addition.easyplace.ProjectionPlacement;
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
        // 方块实体数据不会随 updateListeners 同步（Yarn 文档明确要求用 markForUpdate），
        // 少了这一句：服务端写进去了，客户端却收不到，表现为"告示牌空白、右键一次才出现文字"。
        if (level instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void hao$restorePlacementState(ItemPlacementContext context, CallbackInfoReturnable<BlockState> cir) {
        double relativeHitZ = hitOffsetZ(context.getHitPos(), PlacementBitTools.clickedPos(context));
        HaoDebug.log("[hao-easyplace] [getPlacementState] 规则="
                + ProjectionPlacement.ruleEnabled()
                + " hitPos=" + context.getHitPos() + " blockPos=" + PlacementBitTools.clickedPos(context)
                + " relZ=" + String.format("%.3f", relativeHitZ)
                + " isProtocol=" + isProtocolHit(relativeHitZ)
                + " block=" + this.getBlock());
        if (!ProjectionPlacement.ruleEnabled()) return;
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
        // 投影给的形状在当前位置立不住（例如坡轨坡顶没有方块）→ 这一格直接放弃。
        // 若交给原版，它会把坡轨重算成平轨，放出来的就不是投影的样子了；
        // 而如果强行保持投影形状，又会放出之后必然掉落的浮空坡轨。
        if (!ProjectionPlacement.stateCanExist(state, context.getWorld(), context.getBlockPos())) {
            HaoDebug.log("[hao-easyplace]   [放弃放置] 状态在当前环境立不住 state=" + state
                    + " pos=" + context.getBlockPos());
            cir.setReturnValue(null);
            return;
        }
        if (!this.canPlace(context, state)) return;
        cir.setReturnValue(state);
    }

    @WrapMethod(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;")
    private ActionResult hao$enterPlacementWindow(ItemPlacementContext context, Operation<ActionResult> original) {
        boolean projected = ProjectionPlacement.ruleEnabled()
                && isProtocolHit(hitOffsetX(context.getHitPos(), PlacementBitTools.clickedPos(context)));
        if (projected) {
            ProjectionPlacement.markProjectedState(true);
            ProjectionPlacement.assignPlaceTargetPos(PlacementBitTools.clickedPos(context));
            ProjectionPlacement.assignPlaceTargetBlock(this.getBlock());
        }
        ActionResult result;
        try {
            result = original.call(context);
        } finally {
            ProjectionPlacement.markProjectedState(false);
            ProjectionPlacement.noteWindowFlags(0);
            ProjectionPlacement.assignPlaceTargetPos(BlockPos.ORIGIN);
            ProjectionPlacement.assignPlaceTargetBlock(Blocks.AIR);
        }
        // 投影放置后让这一格自己重新检查一次红石状态：先放拉杆、后放铁轨时，
        // 铁轨的 onBlockAdded 不一定重新计算 powered，需要主动邻居更新一次。
        if (projected && result.isAccepted() && !context.getWorld().isClient()) {
            BlockPos target = context.getBlockPos();
            BlockState placed = context.getWorld().getBlockState(target);
            if (!placed.isAir()) {
                context.getWorld().updateNeighbor(target, placed.getBlock(), target);
            }
        }
        return result;
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
                // postPlacement 不会把 ItemStack 的 BLOCK_ENTITY_DATA 写进方块实体，手动应用一次。
                hao$applyBlockEntityData(level, pos, withFullNbt, state);
                hao$notifyClient(level, pos);
                return hao$ok;
            }
            boolean hao$ok2 = original.call(instance, pos, level, player, stack, state);
            hao$applyBlockEntityData(level, pos, stack, state);
            hao$notifyClient(level, pos);
            return hao$ok2;
        }
        HaoDebug.log("[hao-easyplace] [postPlacement] pos=" + pos
                + " side=" + context.getSide() + " state=" + state
                + " 世界该处=" + level.getBlockState(pos));
        ItemStack newStack = ProjectionPlacement.applySchematicItemData(stack, context);
        if (newStack == null) {
            boolean hao$plain = original.call(instance, pos, level, player, stack, state);
            hao$applyBlockEntityData(level, pos, stack, state);
            // 写完必须主动广播：客户端预测放置用的是原始 stack，本地方块实体没有这份数据，
            // 不广播就一直是空白，直到玩家右键触发一次方块实体同步。
            hao$notifyClient(level, pos);
            return hao$plain;
        }
        boolean result = original.call(instance, pos, level, player, newStack, state);
        hao$applyBlockEntityData(level, pos, newStack, state);
        hao$notifyClient(level, pos);
        double relativeHitZ = hitOffsetZ(context.getHitPos(), PlacementBitTools.clickedPos(context));
        int protocolValue = readBits(relativeHitZ);
        return result;
    }


/** 把 {@code BLOCK_ENTITY_DATA} 组件写到刚放置的方块实体上（{@code postPlacement} 本身不会做这件事）。 */
		@Unique
		private static void hao$applyBlockEntityData(World level, BlockPos pos, ItemStack stack, BlockState state) {;
				if (level.isClient()) {
						return;
				}
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity == null) {
						return;
				}
				net.minecraft.component.type.NbtComponent blockData =
								stack.get(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA);
				if (blockData == null || blockData.isEmpty()) {
						return;
				}
				if (blockData.applyToBlockEntity(blockEntity, level.getRegistryManager())) {
						blockEntity.markDirty();
						level.updateListeners(pos, state, state, 3);
				}
		}
}

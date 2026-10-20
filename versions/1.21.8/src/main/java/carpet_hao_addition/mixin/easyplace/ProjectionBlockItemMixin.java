package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.HaoDebug;

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
								if (HaoDebug.ENABLED) {
										HaoDebug.log("[hao-easyplace] [广播刷新] 标牌 updateListeners: " + pos);
								}
						}
				}
				level.updateListeners(pos, now, now, 3);
		}

		@Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
		private void hao$restorePlacementState(ItemPlacementContext context, CallbackInfoReturnable<BlockState> cir) {
				double relativeHitZ = hitOffsetZ(context.getHitPos(), PlacementBitTools.clickedPos(context));
				if (HaoDebug.ENABLED) {
						HaoDebug.log("[hao-easyplace] [getPlacementState] 规则="
										+ ProjectionPlacement.ruleEnabled()
										+ " hitPos=" + context.getHitPos() + " blockPos=" + PlacementBitTools.clickedPos(context)
										+ " relZ=" + String.format("%.3f", relativeHitZ)
										+ " isProtocol=" + isProtocolHit(relativeHitZ)
										+ " block=" + this.getBlock());
				}
				if (!ProjectionPlacement.ruleEnabled()) return;
				if (!isProtocolHit(relativeHitZ)) return;

				// HEAD 注入时方法体还没执行，cir.getReturnValue() 恒为 null，
				// 所以直接现算一份基础状态（用 Block 的那个重载，不是被本 mixin 包住的路径，不会递归）。
				BlockState baseState = this.getBlock().getPlacementState(context);
				BlockState state = ProjectionPlacement.decodeSchematicState(this.getBlock(), context, baseState);
				if (HaoDebug.ENABLED) {
						HaoDebug.log("[hao-easyplace]   [放置解码] hitPos=" + context.getHitPos()
										+ " blockPos=" + PlacementBitTools.clickedPos(context) + " side=" + context.getSide()
										+ " 还原状态=" + state);
				}
				if (state == null) {
						cir.setReturnValue(null);
						return;
				}
				// 投影给的形状在当前位置立不住（例如坡轨坡顶没有方块）→ 这一格直接放弃。
				// 若交给原版，它会把坡轨重算成平轨，放出来的就不是投影的样子了；
				// 而如果强行保持投影形状，又会放出之后必然掉落的浮空坡轨。
				if (!ProjectionPlacement.stateCanExist(state, context.getWorld(), context.getBlockPos())) {
						if (HaoDebug.ENABLED) {
								HaoDebug.log("[hao-easyplace]   [放弃放置] 状态在当前环境立不住 state=" + state
												+ " pos=" + context.getBlockPos());
						}
						cir.setReturnValue(null);
						return;
				}
				if (!this.canPlace(context, state)) return;
				cir.setReturnValue(state);
		}

		@WrapMethod(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;")
		private ActionResult hao$enterPlacementWindow(ItemPlacementContext context, Operation<ActionResult> original) {
				// 协议值藏在 **z** 的偏移上（见 PlacementBitTools.bitsToHitVec 只改 z），
				// 所以这里必须用 hitOffsetZ。早先误写成 hitOffsetX，而 x 偏移恒在 [0,1)、
				// 阈值是 2.0 —— 条件恒假，整条放置窗口链路（铜灯不翻转、告示牌不弹编辑界面）
				// 从来没有生效过。
				if (ProjectionPlacement.ruleEnabled()
								&& isProtocolHit(hitOffsetZ(context.getHitPos(), PlacementBitTools.clickedPos(context)))) {
						ProjectionPlacement.markProjectedState(true);
						ProjectionPlacement.assignPlaceTargetPos(PlacementBitTools.clickedPos(context));
				}
				try {
						return original.call(context);
				} finally {
						ProjectionPlacement.markProjectedState(false);
						ProjectionPlacement.noteWindowFlags(0);
						ProjectionPlacement.assignPlaceTargetPos(BlockPos.ORIGIN);
				}
		}

		@WrapOperation(
						method = "place",
						at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BlockItem;postPlacement(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/block/BlockState;)Z")
		)
		private boolean hao$applyItemData(BlockItem instance, BlockPos pos, World level, PlayerEntity player, ItemStack stack, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) ItemPlacementContext context)
		{
				// 优先使用「完整 NBT 通道」送来的数据。
				carpet_hao_addition.easyplace.BlockDataChannel.PendingData data = carpet_hao_addition.easyplace.BlockDataChannel.take(player, pos, state);
				if (data != null) {
						net.minecraft.nbt.NbtCompound fullNbt = data.nbt();
						if (fullNbt != null) {
								ItemStack withFullNbt = stack.copy();
								withFullNbt.set(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA,
												net.minecraft.component.type.NbtComponent.of(fullNbt));
								if (HaoDebug.ENABLED) {
										HaoDebug.log("[hao-easyplace] [服务端] 应用完整 NBT: 位置=" + pos + " id=" + fullNbt.getString("id"));
								}
								boolean hao$ok = original.call(instance, pos, level, player, withFullNbt, state);
								hao$notifyClient(level, pos);
								return hao$ok;
						}
						boolean hao$ok2 = original.call(instance, pos, level, player, stack, state);
						hao$notifyClient(level, pos);
						return hao$ok2;
				}
				if (HaoDebug.ENABLED) {
						HaoDebug.log("[hao-easyplace] [postPlacement] pos=" + pos
										+ " side=" + context.getSide() + " state=" + state
										+ " 世界该处=" + level.getBlockState(pos));
				}
				ItemStack newStack = ProjectionPlacement.applySchematicItemData(stack, context);
				if (newStack == null) {
						return original.call(instance, pos, level, player, stack, state);
				}
				boolean result = original.call(instance, pos, level, player, newStack, state);
				return result;
		}
}

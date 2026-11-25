package carpet_hao_addition.mixin.pickup;

import carpet_hao_addition.pickup.PickupFilter;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 配合 carpet 的 {@code blockDropsDirectlyEnterInventory}（1.21.x 版本）。
 * <p>
 * 它的实现是把掉落表里的每个 ItemStack 直接 {@code insertStack} 进玩家背包、
 * 再把已经空掉的栈过滤掉，全程没有物品实体。所以要在掉落表刚生成时
 * 记下被名单拦下的那些 ItemStack 对象，等 {@code insertStack} 被调用时
 * 按对象身份认出来、返回 false，这样该栈不会被消耗，会照常以掉落物形式生到地上。
 */
@Mixin(Block.class)
public abstract class Block_pickupFilterDropsMixin {
	private static final String GET_DROPPED_STACKS =
			"getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;"
					+ "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;"
					+ "Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;";

	@Inject(method = GET_DROPPED_STACKS, at = @At("HEAD"))
	private static void hao$beginDrops(BlockState state, ServerWorld world, BlockPos pos, BlockEntity blockEntity,
	                                   Entity breaker, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> cir) {
		PickupFilter.beginDrops();
	}

	@Inject(method = GET_DROPPED_STACKS, at = @At("RETURN"))
	private static void hao$markDrops(BlockState state, ServerWorld world, BlockPos pos, BlockEntity blockEntity,
	                                  Entity breaker, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> cir) {
		if (!(breaker instanceof PlayerEntity)) {
			return;
		}
		List<ItemStack> drops = cir.getReturnValue();
		if (drops == null || drops.isEmpty()) {
			return;
		}
		for (ItemStack stack : drops) {
			PickupFilter.markDrop(stack);
		}
	}
}
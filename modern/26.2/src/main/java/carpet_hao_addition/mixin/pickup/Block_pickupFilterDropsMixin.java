package carpet_hao_addition.mixin.pickup;

import carpet_hao_addition.pickup.PickupFilter;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 配合 carpet 的 {@code blockDropsDirectlyEnterInventory} 的兜底路径。
 * <p>
 * 26.x 的 carpet 走 {@code ServerLevel#addEntity}（见 {@link ServerLevel_pickupFilterMixin}），
 * 一般不经过掉落表。这里保留同样的保护：万一某个版本改成「掉落表直接入包」，
 * 就把被名单拦下的 ItemStack 对象记下来，等 {@code Inventory#add} 时按身份认出来。
 */
@Mixin(Block.class)
public abstract class Block_pickupFilterDropsMixin {
    private static final String GET_DROPS =
            "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;"
                    + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;";

    @Inject(method = GET_DROPS, at = @At("HEAD"))
    private static void hao$beginDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity,
                                       Entity breaker, net.minecraft.world.item.ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        PickupFilter.beginDrops();
    }

    @Inject(method = GET_DROPS, at = @At("RETURN"))
    private static void hao$markDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity,
                                      Entity breaker, net.minecraft.world.item.ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(breaker instanceof Player)) {
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
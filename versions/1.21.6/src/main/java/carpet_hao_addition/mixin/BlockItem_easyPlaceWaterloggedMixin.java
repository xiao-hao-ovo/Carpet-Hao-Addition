package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.PlaceWaterloggedHandler;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * easyPlaceWaterlogged(服务端):方块放下之后,若客户端之前请求过该位置补水,就给方块补上含水。
 * <p>
 * 时机很关键:必须在<b>方块落下之后</b>补水。<b>不能提前在原地放一格水</b> —— 单独放下的水是
 * 流动水,会扩散流走;慢速放置时方块紧跟其后还能把水锁住,快速放置时水已经流走、方块落下时
 * 位置已无水,结果就是"方块放下了却不含水"。改在放置后补水,水直接被方块吸收,既不会流走,
 * 也不会挡住方块放置。
 */
@Mixin(BlockItem.class)
public abstract class BlockItem_easyPlaceWaterloggedMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void hao$waterlogAfterPlacement(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		if (!cir.getReturnValue().isAccepted()) {
			return;
		}
		if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
			return;
		}
		if (!(context.getWorld() instanceof ServerWorld world)) {
			return;
		}
		BlockPos pos = context.getBlockPos();
		BlockState placed = world.getBlockState(pos);
		PlaceWaterloggedHandler.onBlockPlaced(player, world, pos, placed);
	}
}

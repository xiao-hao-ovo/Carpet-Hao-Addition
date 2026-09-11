package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.PlaceWaterloggedHandler;
import carpet_hao_addition.PlaceWaterloggedPayload;

import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * easyPlaceWaterlogged(客户端):告诉服务端「正在轻松放置」以及「哪些位置该放水源」。
 * <p>
 * 钩子挂在 {@link PlacementHandler#applyPlacementProtocolToPlacementState}:它是 Litematica
 * 轻松放置给每个待放方块计算最终状态时都会调用的方法,参数 {@code UseContext.pos()} 就是
 * 本次目标位置。这里<b>只发请求,不修改返回值</b> —— 改写放置状态会让状态与实际位置不符,
 * 服务端校验失败,方块根本放不下去。
 * <p>
 * 26.x 的 Litematica 已没有 {@code WorldUtils.easyPlaceBlockChecksCancel},所以 1.21.x 的
 * 那份客户端 mixin 在这里换成了本实现;服务端侧逻辑(补含水 / 放水源)完全一致。
 */
@Mixin(PlacementHandler.class)
public abstract class WorldUtils_easyPlaceWaterloggedMixin {
	@Inject(method = "applyPlacementProtocolToPlacementState", at = @At("RETURN"))
	private static void hao$requestWater(BlockState state, PlacementHandler.UseContext context,
			CallbackInfoReturnable<BlockState> cir) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		Player player = context.entity() instanceof Player p ? p : null;
		if (player == null || !player.level().isClientSide()) {
			return;
		}
		if (!hao$hasIce(player)) {
			return;
		}
		List<BlockPos> waterPositions = new ArrayList<>(1);
		// 目标位置:投影里是水源的,单独请服务端放一格水。
		BlockPos pos = context.pos();
		if (hao$shouldPlaceWater(player, pos)) {
			waterPositions.add(pos);
		}
		// 只在这些情况下才需要打扰服务端。
		boolean needsWaterlog = state != null && state.hasProperty(BlockStateProperties.WATERLOGGED)
				&& !Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED));
		if (!needsWaterlog && waterPositions.isEmpty()) {
			return;
		}
		PlaceWaterloggedHandler.registerPayloadType();
		ClientPlayNetworking.send(new PlaceWaterloggedPayload(true, waterPositions));
	}

	/** 该位置是否应当补一格水源:投影里必须是<b>水源</b>,且实际位置还没有水。 */
	@Unique
	private static boolean hao$shouldPlaceWater(Player player, BlockPos pos) {
		WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
		if (schematicWorld == null) {
			return false;
		}
		BlockState desired = schematicWorld.getBlockState(pos);
		// 只放水源:投影里的流动水不用管,它们会被相邻水源自然填满,逐个去放既多余又浪费冰。
		if (!desired.is(Blocks.WATER) || !desired.getFluidState().isSource()) {
			return false;
		}
		FluidState fluid = player.level().getFluidState(pos);
		return fluid.isEmpty();
	}

	@Unique
	private static boolean hao$hasIce(Player player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack s = player.getInventory().getItem(slot);
			if (!s.isEmpty() && s.is(Items.ICE)) {
				return true;
			}
		}
		return false;
	}
}

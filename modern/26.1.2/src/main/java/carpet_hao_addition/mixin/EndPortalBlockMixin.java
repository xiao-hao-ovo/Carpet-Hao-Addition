package carpet_hao_addition.mixin;

import carpet_hao_addition.NoEndPortalTeleportSettings;
import carpet_hao_addition.portal.PlayerNoEndPortalTeleportList;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * noEndPortalTeleport 规则的行为接入点。
 * <p>
 * EndPortalBlock.onEntityCollision 是末地传送门的传送入口。规则开启时按
 * {@link PlayerNoEndPortalTeleportList} 判定:全局模式下所有玩家不被传送,
 * 否则名单内玩家不被传送;其它实体(物品、矿车等)与未命中玩家保持原版行为。
 * 该设计仿照 Carpet-AMS-Addition 的 NoNetherPortalTeleport(黑名单 + globalMode)。
 */
@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void hao$noEndPortalTeleport(BlockState state, Level world, BlockPos pos, Entity entity,
			InsideBlockEffectApplier collisionHandler, boolean flag, CallbackInfo ci) {
		if (world.isClientSide() || !(entity instanceof ServerPlayer player)) {
			return;
		}
		if (!NoEndPortalTeleportSettings.isEnabled()) {
			return;
		}
		if (!PlayerNoEndPortalTeleportList.shouldBlock(player.getUUID())) {
			return;
		}

		// 名单/全局模式命中:该玩家不被末地传送门传送。
		ci.cancel();
	}
}

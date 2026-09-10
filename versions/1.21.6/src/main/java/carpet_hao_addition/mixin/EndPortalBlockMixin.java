package carpet_hao_addition.mixin;

import carpet_hao_addition.NoEndPortalTeleportSettings;
import carpet_hao_addition.portal.PlayerNoEndPortalTeleportList;

import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
	@Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
	private void hao$noEndPortalTeleport(BlockState state, World world, BlockPos pos, Entity entity,
			EntityCollisionHandler collisionHandler, CallbackInfo ci) {
		if (world.isClient() || !(entity instanceof ServerPlayerEntity player)) {
			return;
		}
		if (!NoEndPortalTeleportSettings.isEnabled()) {
			return;
		}
		if (!PlayerNoEndPortalTeleportList.shouldBlock(player.getUuid())) {
			return;
		}

		// 名单/全局模式命中:该玩家不被末地传送门传送。
		ci.cancel();
	}
}

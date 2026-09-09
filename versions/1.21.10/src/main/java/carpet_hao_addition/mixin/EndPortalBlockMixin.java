package carpet_hao_addition.mixin;

import carpet_hao_addition.NoEndPortalTeleportSettings;

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
 * EndPortalBlock.onEntityCollision 是末地传送门的传送入口。规则开启时对
 * 玩家(ServerPlayerEntity)直接跳过该处理,使其不被传送;其它实体
 * (物品、矿车等)保持原版行为,与 Carpet-AMS-Addition 的
 * NoNetherPortalTeleport(仅拦截玩家)语义一致。
 */
@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
	@Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
	private void hao$noEndPortalTeleport(BlockState state, World world, BlockPos pos, Entity entity,
			EntityCollisionHandler collisionHandler, CallbackInfo ci) {
		if (world.isClient() || !(entity instanceof ServerPlayerEntity)) {
			return;
		}
		if (!NoEndPortalTeleportSettings.isEnabled()) {
			return;
		}

		// 玩家不被末地传送门传送;实体仍正常离开该方块碰撞处理。
		ci.cancel();
	}
}

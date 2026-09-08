package carpet_hao_addition.zoneguard.command;

import carpet_hao_addition.zoneguard.ZoneguardSettings;
import carpet_hao_addition.zoneguard.region.ZoneguardSavedData;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * /zoneguard 命令的权限判定:
 * 规则关闭时整棵命令树不可见/不可用;开启后,玩家需为单机主人、命令权限 ≥2
 * (游戏管理员)或本扩展白名单成员。op 子命令仅限单机主人或游戏管理员。
 */
public final class ZoneguardPermissions {
	private ZoneguardPermissions() {
	}

	public static boolean canUse(ServerCommandSource source) {
		// The whole /zoneguard tree is hidden unless the zoneguard rule is enabled.
		// Read the authoritative Carpet rule value (not the static field).
		if (!ZoneguardSettings.isEnabled()) {
			return false;
		}

		if (!source.isExecutedByPlayer()) {
			return true;
		}

		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			return true;
		}

		MinecraftServer server = source.getServer();
		GameProfile profile = player.getGameProfile();
		if (server.isHost(profile) || source.hasPermissionLevel(2)) {
			return true;
		}

		return ZoneguardSavedData.get(server).isOperator(profile.getId());
	}

	public static boolean canManageOperators(ServerCommandSource source) {
		if (!source.isExecutedByPlayer()) {
			return true;
		}

		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			return true;
		}

		MinecraftServer server = source.getServer();
		GameProfile profile = player.getGameProfile();
		return server.isHost(profile) || source.hasPermissionLevel(2);
	}
}

package carpet_hao_addition.zoneguard.command;

import carpet_hao_addition.zoneguard.ZoneguardSettings;
import carpet_hao_addition.zoneguard.region.ZoneguardSavedData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * /zoneguard 命令的权限判定(1.21.10 版,使用 NameAndId):
 * 规则关闭时整棵命令树不可见/不可用;开启后,玩家需为单机主人、命令权限 ≥2
 * (游戏管理员)或本扩展白名单成员。op 子命令仅限单机主人或游戏管理员。
 */
public final class ZoneguardPermissions {
	private ZoneguardPermissions() {
	}

	public static boolean canUse(CommandSourceStack source) {
		// The whole /zoneguard tree is hidden unless the zoneguard rule is enabled.
		// Read the authoritative Carpet rule value (not the static field).
		if (!ZoneguardSettings.isEnabled()) {
			return false;
		}

		if (!source.isPlayer()) {
			return true;
		}

		ServerPlayer player = source.getPlayer();
		if (player == null) {
			return true;
		}

		MinecraftServer server = source.getServer();
		NameAndId entry = new NameAndId(player.getGameProfile());
		if (server.isSingleplayerOwner(entry) || Commands.LEVEL_GAMEMASTERS.check(source.permissions())) {
			return true;
		}

		return ZoneguardSavedData.get(server).isOperator(entry.id());
	}

	public static boolean canManageOperators(CommandSourceStack source) {
		if (!source.isPlayer()) {
			return true;
		}

		ServerPlayer player = source.getPlayer();
		if (player == null) {
			return true;
		}

		MinecraftServer server = source.getServer();
		NameAndId entry = new NameAndId(player.getGameProfile());
		return server.isSingleplayerOwner(entry) || Commands.LEVEL_GAMEMASTERS.check(source.permissions());
	}
}

package carpet_hao_addition.portal;

import carpet.utils.Translations;

import carpet_hao_addition.NoEndPortalTeleportSettings;
import carpet_hao_addition.portal.PlayerNoEndPortalTeleportList;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /playerNoEndPortalTeleport — 管理“末地门不传送”名单(仿 Carpet-AMS-Addition 的
 * /playerNoNetherPortalTeleport)。仅在规则 noEndPortalTeleport 开启时可见/可用。
 * <p>
 * 子命令:
 * - globalMode [true|false]  全局模式:true=所有玩家不被末地门传送;false=仅名单内玩家
 * - add &lt;player&gt; / remove &lt;player&gt;  增删名单(在线玩家)
 * - clear                   清空名单
 * - list                    查看名单
 * - help                    显示用法
 * <p>
 * 名单与全局模式保存在内存(PlayerNoEndPortalTeleportList),重启后清空;
 * 判定由版本层 EndPortalBlockMixin 调用 shouldBlock(uuid)。
 */
public final class PlayerNoEndPortalTeleportCommands {
	private PlayerNoEndPortalTeleportCommands() {
	}

	private static final String MSG = "playerNoEndPortalTeleport.commands.";

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("playerNoEndPortalTeleport")
				.requires(PlayerNoEndPortalTeleportCommands::canUse)
				.executes(PlayerNoEndPortalTeleportCommands::help)
				.then(literal("globalMode")
						.executes(PlayerNoEndPortalTeleportCommands::showGlobalMode)
						.then(argument("bool", BoolArgumentType.bool())
								.executes(PlayerNoEndPortalTeleportCommands::setGlobalMode)))
				.then(literal("add")
						.then(argument("player", EntityArgument.player())
								.executes(PlayerNoEndPortalTeleportCommands::add)))
				.then(literal("remove")
						.then(argument("player", EntityArgument.player())
								.executes(PlayerNoEndPortalTeleportCommands::remove)))
				.then(literal("clear")
						.executes(PlayerNoEndPortalTeleportCommands::clear))
				.then(literal("list")
						.executes(PlayerNoEndPortalTeleportCommands::list))
				.then(literal("help")
						.executes(PlayerNoEndPortalTeleportCommands::help))
		);
	}

	/** 规则开启后才可用;操作名单需要游戏管理员权限(OP≥2)或控制台。 */
	private static boolean canUse(CommandSourceStack source) {
		if (!NoEndPortalTeleportSettings.isEnabled()) {
			return false;
		}
		return !source.isPlayer() || Commands.LEVEL_GAMEMASTERS.check(source.permissions());
	}

	private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		UUID uuid = player.getUUID();
		String name = player.getName().getString();
		if (PlayerNoEndPortalTeleportList.add(uuid, name)) {
			context.getSource().sendSuccess(() -> msg("add.success", name), true);
			return 1;
		}
		context.getSource().sendSuccess(() -> msg("add.exists", name), false);
		return 0;
	}

	private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		UUID uuid = player.getUUID();
		if (PlayerNoEndPortalTeleportList.remove(uuid)) {
			context.getSource().sendSuccess(() -> msg("remove.success", player.getName().getString()), true);
			return 1;
		}
		context.getSource().sendFailure(msg("remove.notFound", player.getName().getString()));
		return 0;
	}

	private static int clear(CommandContext<CommandSourceStack> context) {
		int cleared = PlayerNoEndPortalTeleportList.clear();
		if (cleared == 0) {
			context.getSource().sendSuccess(() -> msg("clear.empty"), false);
			return 0;
		}
		context.getSource().sendSuccess(() -> msg("clear.success", String.valueOf(cleared)), true);
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> context) {
		Map<UUID, String> entries = PlayerNoEndPortalTeleportList.entries();
		if (entries.isEmpty()) {
			context.getSource().sendSuccess(() -> msg("list.empty"), false);
			return 0;
		}

		context.getSource().sendSuccess(() -> msg("list.header"), false);
		entries.forEach((uuid, name) -> context.getSource().sendSuccess(
				() -> msg("list.item", name, uuid.toString()), false
		));
		return entries.size();
	}

	private static int showGlobalMode(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSuccess(
				() -> msg("globalMode.state", String.valueOf(PlayerNoEndPortalTeleportList.globalMode)), false);
		return 1;
	}

	private static int setGlobalMode(CommandContext<CommandSourceStack> context) {
		boolean mode = BoolArgumentType.getBool(context, "bool");
		PlayerNoEndPortalTeleportList.globalMode = mode;
		context.getSource().sendSuccess(() -> msg(mode ? "globalMode.enable" : "globalMode.disable"), true);
		return 1;
	}

	private static int help(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> msg("help.header"), false);
		source.sendSuccess(() -> msg("help.globalMode"), false);
		source.sendSuccess(() -> msg("help.add"), false);
		source.sendSuccess(() -> msg("help.remove"), false);
		source.sendSuccess(() -> msg("help.clear"), false);
		source.sendSuccess(() -> msg("help.list"), false);
		return 1;
	}

	/** 服务端渲染翻译键(carpet 翻译表),转成纯文本组件,不依赖客户端语言文件。 */
	private static Component msg(String key, String... args) {
		String pattern = Translations.tr(MSG + key);
		return Component.literal(args.length == 0 ? pattern : pattern.formatted((Object[]) args));
	}
}

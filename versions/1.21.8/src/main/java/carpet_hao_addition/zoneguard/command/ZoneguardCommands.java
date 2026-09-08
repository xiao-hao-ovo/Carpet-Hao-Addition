package carpet_hao_addition.zoneguard.command;

import carpet_hao_addition.zoneguard.region.DetectorRegion;
import carpet_hao_addition.zoneguard.region.RegionObserverRefresh;
import carpet_hao_addition.zoneguard.region.ZoneguardSavedData;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.StringJoiner;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /zoneguard 命令树(功能与参考实现等价,适配 1.21.8 yarn API)。
 * <p>
 * 所有用户可见文案都走 Minecraft 翻译键(assets/carpet-hao-addition/lang/{en_us,zh_cn}.json),
 * 代码中不硬编码语言文本;命令参数名使用英文 token。
 * <p>
 * /zoneguard set &lt;id&gt; &lt;from&gt; &lt;to&gt;  - 设置立方禁用区域
 * /zoneguard view                       - 列出区域
 * /zoneguard clear &lt;id&gt;              - 清除区域并重启区域内面对面的侦测器对
 * /zoneguard op add|remove|list &lt;player&gt; - 管理白名单
 * <p>
 * 命令注册入口在版本层,由共享层扩展的 registerCommands 委托调用。
 */
public final class ZoneguardCommands {
	private ZoneguardCommands() {
	}

	private static final String MSG = "zoneguard.commands.";

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("zoneguard")
				.requires(ZoneguardPermissions::canUse)
				.executes(ZoneguardCommands::view)
				.then(literal("set")
						.then(argument("id", IntegerArgumentType.integer(0))
								.then(argument("from", BlockPosArgumentType.blockPos())
										.then(argument("to", BlockPosArgumentType.blockPos())
												.executes(ZoneguardCommands::setRegion)))))
				.then(literal("view")
						.executes(ZoneguardCommands::view))
				.then(literal("clear")
						.then(argument("id", IntegerArgumentType.integer(0))
								.executes(ZoneguardCommands::clearRegion)))
				.then(literal("op")
						.requires(ZoneguardPermissions::canManageOperators)
						.then(literal("add")
								.then(argument("player", GameProfileArgumentType.gameProfile())
										.executes(ZoneguardCommands::addOperator)))
						.then(literal("remove")
								.then(argument("player", GameProfileArgumentType.gameProfile())
										.executes(ZoneguardCommands::removeOperator)))
						.then(literal("list")
								.executes(ZoneguardCommands::listOperators)))
		);
	}

	private static int setRegion(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ServerWorld world = source.getWorld();
		int id = IntegerArgumentType.getInteger(context, "id");
		BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
		BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");

		DetectorRegion region = ZoneguardSavedData.get(source.getServer()).setRegion(id, world, from, to);
		source.sendFeedback(() -> Text.translatable(MSG + "set.success",
				id, region.dimension().getValue(), formatPos(region.min()), formatPos(region.max())), true);
		return 1;
	}

	private static int view(CommandContext<ServerCommandSource> context) {
		ZoneguardSavedData data = ZoneguardSavedData.get(context.getSource().getServer());
		if (data.regions().isEmpty()) {
			context.getSource().sendFeedback(() -> Text.translatable(MSG + "view.empty"), false);
			return 0;
		}

		context.getSource().sendFeedback(() -> Text.translatable(MSG + "view.header"), false);
		data.regions().forEach((id, region) -> context.getSource().sendFeedback(
				() -> Text.translatable(MSG + "view.item",
						id, region.dimension().getValue(), formatPos(region.min()), formatPos(region.max())),
				false
		));
		return data.regions().size();
	}

	private static int clearRegion(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		int id = IntegerArgumentType.getInteger(context, "id");
		ZoneguardSavedData data = ZoneguardSavedData.get(source.getServer());
		return data.removeRegion(id).map(region -> {
			ServerWorld world = source.getServer().getWorld(region.dimension());
			int startedPairs = world == null ? 0 : RegionObserverRefresh.startLoadedFaceToFacePairs(world, region);
			source.sendFeedback(() -> Text.translatable(MSG + "clear.success",
					id, region.dimension().getValue(), formatPos(region.min()), formatPos(region.max()), startedPairs), true);
			return 1;
		}).orElseGet(() -> {
			source.sendError(Text.translatable(MSG + "clear.notFound", id));
			return 0;
		});
	}

	private static int addOperator(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
		ZoneguardSavedData data = ZoneguardSavedData.get(source.getServer());
		int added = 0;

		for (GameProfile profile : profiles) {
			if (data.addOperator(profile)) {
				added++;
				source.sendFeedback(() -> Text.translatable(MSG + "op.add.success", profile.getName()), true);
			} else {
				source.sendFeedback(() -> Text.translatable(MSG + "op.add.exists", profile.getName()), false);
			}
		}

		return added;
	}

	private static int removeOperator(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
		ZoneguardSavedData data = ZoneguardSavedData.get(source.getServer());
		int removed = 0;

		for (GameProfile profile : profiles) {
			OptionalName removedName = removeOperator(data, profile);
			if (removedName.found()) {
				removed++;
				source.sendFeedback(() -> Text.translatable(MSG + "op.remove.success", removedName.name()), true);
			} else {
				source.sendError(Text.translatable(MSG + "op.remove.notFound", profile.getName()));
			}
		}

		return removed;
	}

	private static OptionalName removeOperator(ZoneguardSavedData data, GameProfile profile) {
		return data.removeOperator(profile)
				.map(name -> new OptionalName(true, name))
				.or(() -> data.removeOperatorByName(profile.getName()).map(name -> new OptionalName(true, name)))
				.orElseGet(() -> new OptionalName(false, profile.getName()));
	}

	private static int listOperators(CommandContext<ServerCommandSource> context) {
		ZoneguardSavedData data = ZoneguardSavedData.get(context.getSource().getServer());
		if (data.operators().isEmpty()) {
			context.getSource().sendFeedback(() -> Text.translatable(MSG + "op.list.empty"), false);
			return 0;
		}

		StringJoiner joiner = new StringJoiner(", ");
		data.operators().values().forEach(joiner::add);
		context.getSource().sendFeedback(() -> Text.translatable(MSG + "op.list", joiner), false);
		return data.operators().size();
	}

	private static String formatPos(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private record OptionalName(boolean found, String name) {
	}
}

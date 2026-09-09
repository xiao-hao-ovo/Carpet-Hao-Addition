package carpet_hao_addition.zoneguard.command;

import carpet.utils.Translations;

import carpet_hao_addition.zoneguard.region.DetectorRegion;
import carpet_hao_addition.zoneguard.region.RegionObserverRefresh;
import carpet_hao_addition.zoneguard.region.ZoneguardSavedData;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.StringJoiner;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /zoneguard 命令树(1.21.10 版;1.21.10 把玩家/名单类型换成 PlayerConfigEntry)。
 * <p>
 * 文案在 assets/carpet-hao-addition/lang/{en_us,zh_cn}.json 中维护,由
 * canHasTranslations 并入 carpet 的翻译表。消息在<b>服务端</b>按 carpet 语言渲染成
 * 纯文本后再发送(见 {@link #msg(String, String...)}),因此客户端不装本 mod 也能
 * 正常显示,不会出现裸键名或“发送不了”的编码问题。
 * <p>
 * /zoneguard set &lt;id&gt; &lt;from&gt; &lt;to&gt;  - 设置立方禁用区域
 * /zoneguard view                       - 列出区域
 * /zoneguard help                       - 显示使用说明
 * /zoneguard clear &lt;id&gt;              - 清除区域并重启区域内面对面的侦测器对
 * /zoneguard op add|remove|list &lt;player&gt; - 管理白名单
 * <p>
 * 树根节点的 {@code requires(ZoneguardPermissions::canUse)} 同时负责权限与规则门控:
 * 只有 /carpet zoneguard true 开启后整棵树(含 help)才对玩家可见、可执行。
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
				.then(literal("help")
						.executes(ZoneguardCommands::help))
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

	/**
	 * 设置/覆盖一个区域:两个对角坐标经 DetectorRegion.fromCorners 归一化
	 * (自动取最小/最大角),写入随世界存档持久化的 SavedData 并落盘。
	 */
	private static int setRegion(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ServerWorld world = source.getWorld();
		int id = IntegerArgumentType.getInteger(context, "id");
		BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
		BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");

		DetectorRegion region = ZoneguardSavedData.get(source.getServer()).setRegion(id, world, from, to);
		source.sendFeedback(() -> msg("set.success",
				String.valueOf(id), regionDimension(region), formatPos(region.min()), formatPos(region.max())), true);
		return 1;
	}

	private static int view(CommandContext<ServerCommandSource> context) {
		ZoneguardSavedData data = ZoneguardSavedData.get(context.getSource().getServer());
		if (data.regions().isEmpty()) {
			context.getSource().sendFeedback(() -> msg("view.empty"), false);
			return 0;
		}

		context.getSource().sendFeedback(() -> msg("view.header"), false);
		data.regions().forEach((id, region) -> context.getSource().sendFeedback(
				() -> msg("view.item", id, regionDimension(region), formatPos(region.min()), formatPos(region.max())),
				false
		));
		return data.regions().size();
	}

	/**
	 * 输出使用说明(纯帮助,不改变任何数据/行为)。
	 * 仅在规则开启时整树可见(由 canUse 门控),因此 help 天然也在其内。
	 */
	private static int help(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		source.sendFeedback(() -> msg("help.header"), false);
		source.sendFeedback(() -> msg("help.line.help"), false);
		source.sendFeedback(() -> msg("help.line.set"), false);
		source.sendFeedback(() -> msg("help.line.view"), false);
		source.sendFeedback(() -> msg("help.line.clear"), false);
		source.sendFeedback(() -> msg("help.line.op"), false);
		source.sendFeedback(() -> msg("help.line.off"), false);
		return 1;
	}

	/**
	 * 清除区域。原区域中可能残留被禁用期间卡住的“面对面侦测器对”,
	 * 因此调用 RegionObserverRefresh 给已加载区块中的这类侦测器各计划一次刻,
	 * 让它们恢复运作(反馈中报告触发对数,0 表示该区域无已加载的面对面侦测器对)。
	 */
	private static int clearRegion(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		int id = IntegerArgumentType.getInteger(context, "id");
		ZoneguardSavedData data = ZoneguardSavedData.get(source.getServer());
		return data.removeRegion(id).map(region -> {
			ServerWorld world = source.getServer().getWorld(region.dimension());
			int startedPairs = world == null ? 0 : RegionObserverRefresh.startLoadedFaceToFacePairs(world, region);
			source.sendFeedback(() -> msg("clear.success",
					String.valueOf(id), regionDimension(region), formatPos(region.min()), formatPos(region.max()),
					String.valueOf(startedPairs)), true);
			return 1;
		}).orElseGet(() -> {
			source.sendError(msg("clear.notFound", String.valueOf(id)));
			return 0;
		});
	}

	private static int addOperator(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		Collection<PlayerConfigEntry> entries = GameProfileArgumentType.getProfileArgument(context, "player");
		ZoneguardSavedData data = ZoneguardSavedData.get(source.getServer());
		int added = 0;

		for (PlayerConfigEntry entry : entries) {
			if (data.addOperator(entry)) {
				added++;
				source.sendFeedback(() -> msg("op.add.success", entry.name()), true);
			} else {
				source.sendFeedback(() -> msg("op.add.exists", entry.name()), false);
			}
		}

		return added;
	}

	private static int removeOperator(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		Collection<PlayerConfigEntry> entries = GameProfileArgumentType.getProfileArgument(context, "player");
		ZoneguardSavedData data = ZoneguardSavedData.get(source.getServer());
		int removed = 0;

		for (PlayerConfigEntry entry : entries) {
			OptionalName removedName = removeOperator(data, entry);
			if (removedName.found()) {
				removed++;
				source.sendFeedback(() -> msg("op.remove.success", removedName.name()), true);
			} else {
				source.sendError(msg("op.remove.notFound", entry.name()));
			}
		}

		return removed;
	}

	private static OptionalName removeOperator(ZoneguardSavedData data, PlayerConfigEntry entry) {
		return data.removeOperator(entry)
				.map(name -> new OptionalName(true, name))
				.or(() -> data.removeOperatorByName(entry.name()).map(name -> new OptionalName(true, name)))
				.orElseGet(() -> new OptionalName(false, entry.name()));
	}

	private static int listOperators(CommandContext<ServerCommandSource> context) {
		ZoneguardSavedData data = ZoneguardSavedData.get(context.getSource().getServer());
		if (data.operators().isEmpty()) {
			context.getSource().sendFeedback(() -> msg("op.list.empty"), false);
			return 0;
		}

		StringJoiner joiner = new StringJoiner(", ");
		data.operators().values().forEach(joiner::add);
		context.getSource().sendFeedback(() -> msg("op.list", joiner.toString()), false);
		return data.operators().size();
	}

	/**
	 * 服务端渲染翻译键(语言由 carpet 翻译表 / carpet language 决定),
	 * 结果转成纯文本组件,不依赖客户端语言文件。
	 */
	private static Text msg(String key, String... args) {
		String pattern = Translations.tr(MSG + key);
		return Text.literal(args.length == 0 ? pattern : pattern.formatted((Object[]) args));
	}

	private static String regionDimension(DetectorRegion region) {
		return String.valueOf(region.dimension().getValue());
	}

	private static String formatPos(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private record OptionalName(boolean found, String name) {
	}
}

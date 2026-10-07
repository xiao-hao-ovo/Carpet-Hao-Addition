package carpet_hao_addition;

import carpet.utils.Messenger;
import carpet.utils.Translations;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /easyPlaceEntityCount —— 设置**自己**一次放几个实体(每个玩家各一份,互不影响)。
 * <p>
 * 用法:
 * <ul>
 *   <li>{@code /easyPlaceEntityCount &lt;1-64&gt;} —— 设置自己的数量;</li>
 *   <li>{@code /easyPlaceEntityCount} —— 查看自己当前的数量。</li>
 * </ul>
 * 总开关是 Carpet 规则 {@code /carpet easyPlaceEntity true|false};数量只存在内存里。
 * 命令对所有人开放(只能改自己的设置),不需要 OP。
 */
public final class EasyPlaceEntityCommands {
	/** 语言键前缀(与 lang 文件里的键对应)。 */
	private static final String MSG = "easyPlaceEntity.commands.";

	private EasyPlaceEntityCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("easyPlaceEntityCount")
				.executes(context -> show(context.getSource()))
				.then(argument("count", IntegerArgumentType.integer(1, EasyPlaceEntitySettings.MAX_COUNT))
						.executes(context -> set(context.getSource(),
								IntegerArgumentType.getInteger(context, "count")))));
		dispatcher.register(literal("easyPlaceEntityUi")
				.executes(context -> toggleUi(context.getSource()))
				.then(argument("enabled", BoolArgumentType.bool())
						.executes(context -> setUi(context.getSource(),
								BoolArgumentType.getBool(context, "enabled")))));
	}

	/** 显示自己当前的选择器界面开关状态。 */
	private static int showUi(CommandSourceStack source) {
		ServerPlayer player = self(source);
		if (player == null) {
			return 0;
		}
		Messenger.m(source, "w " + tr("uiStatus",
				String.valueOf(EasyPlaceEntitySettings.uiEnabled(player.getUUID())),
				String.valueOf(EasyPlaceEntitySettings.isEnabled())));
		return 1;
	}

	/** 不带参数:切换开关(总是弹界面 ↔ 单个直接放)。 */
	private static int toggleUi(CommandSourceStack source) {
		ServerPlayer player = self(source);
		if (player == null) {
			return 0;
		}
		EasyPlaceEntitySettings.setUiEnabled(player.getUUID(),
				!EasyPlaceEntitySettings.uiEnabled(player.getUUID()));
		return showUi(source);
	}

	/** 带参数:显式打开/关闭。 */
	private static int setUi(CommandSourceStack source, boolean enabled) {
		ServerPlayer player = self(source);
		if (player == null) {
			return 0;
		}
		EasyPlaceEntitySettings.setUiEnabled(player.getUUID(), enabled);
		if (!EasyPlaceEntitySettings.isEnabled()) {
			Messenger.m(source, "y " + tr("notEnabled"));
		}
		return showUi(source);
	}

	/** 显示自己当前的数量。 */
	private static int show(CommandSourceStack source) {
		ServerPlayer player = self(source);
		if (player == null) {
			return 0;
		}
		Messenger.m(source, "w " + tr("status",
				Integer.toString(EasyPlaceEntitySettings.count(player.getUUID())),
				String.valueOf(EasyPlaceEntitySettings.isEnabled())));
		return 1;
	}

	/** 设置自己的数量。 */
	private static int set(CommandSourceStack source, int count) {
		ServerPlayer player = self(source);
		if (player == null) {
			return 0;
		}
		EasyPlaceEntitySettings.setCount(player.getUUID(), count);
		if (!EasyPlaceEntitySettings.isEnabled()) {
			Messenger.m(source, "y " + tr("notEnabled"));
		}
		return show(source);
	}

	/** 取执行命令的玩家;控制台等非玩家来源给出提示并返回 null。 */
	private static ServerPlayer self(CommandSourceStack source) {
		if (source.getEntity() instanceof ServerPlayer player) {
			return player;
		}
		Messenger.m(source, "r " + tr("playerOnly"));
		return null;
	}

	/** 取语言键并按需套用参数。 */
	private static String tr(String key, Object... args) {
		String pattern = Translations.tr(MSG + key);
		return args.length == 0 ? pattern : String.format(pattern, args);
	}
}

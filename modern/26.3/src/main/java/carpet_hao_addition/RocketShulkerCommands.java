package carpet_hao_addition;

import carpet.utils.Messenger;
import carpet.utils.Translations;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /rocketShulker —— 设置**自己**的火箭潜影盒补给位置(每个玩家各一份,互不影响)。
 * <p>
 * 用法:
 * <ul>
 *   <li>{@code /rocketShulker} —— 查看自己的设置;</li>
 *   <li>{@code /rocketShulker offhand} —— 补到自己的副手;</li>
 *   <li>{@code /rocketShulker mainhand [1-9]} —— 补到自己的主手快捷栏第 1-9 格。</li>
 * </ul>
 * 总开关是 Carpet 规则 {@code /carpet rocketShulker true|false};设置只存在内存里。
 * 命令对所有人开放(只能改自己的设置),不需要 OP。
 */
public final class RocketShulkerCommands {
	/** 语言键前缀(与 lang 文件里的键对应)。 */
	private static final String MSG = "rocketShulker.commands.";

	private RocketShulkerCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("rocketShulker")
				.executes(context -> show(context.getSource()))
				.then(literal("offhand")
						.executes(context -> setTarget(context.getSource(),
								RocketShulkerSettings.Target.OFFHAND, 1)))
				.then(literal("mainhand")
						.executes(context -> setTarget(context.getSource(),
								RocketShulkerSettings.Target.MAINHAND, 1))
						.then(argument("slot", IntegerArgumentType.integer(1, 9))
								.executes(context -> setTarget(context.getSource(),
										RocketShulkerSettings.Target.MAINHAND,
										IntegerArgumentType.getInteger(context, "slot"))))));
	}

	/** 显示自己的当前设置。 */
	private static int show(CommandSourceStack source) {
		ServerPlayer player = self(source);
		if (player == null) {
			return 0;
		}
		RocketShulkerSettings.Target target = RocketShulkerSettings.target(player.getUUID());
		Messenger.m(source, "w " + tr("status", String.valueOf(RocketShulkerSettings.isEnabled()),
				target.name().toLowerCase()));
		if (target == RocketShulkerSettings.Target.MAINHAND) {
			Messenger.m(source, "w " + tr("status.slot",
					Integer.toString(RocketShulkerSettings.hotbarSlot(player.getUUID()))));
		}
		if (!RocketShulkerSettings.isEnabled()) {
			Messenger.m(source, "y " + tr("notEnabled"));
		}
		return 1;
	}

	/** 设置自己的补给位置(不影响其他玩家)。 */
	private static int setTarget(CommandSourceStack source, RocketShulkerSettings.Target target, int slot) {
		ServerPlayer player = self(source);
		if (player == null) {
			return 0;
		}
		RocketShulkerSettings.setTarget(player.getUUID(), target);
		if (target == RocketShulkerSettings.Target.MAINHAND) {
			RocketShulkerSettings.setHotbarSlot(player.getUUID(), slot);
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

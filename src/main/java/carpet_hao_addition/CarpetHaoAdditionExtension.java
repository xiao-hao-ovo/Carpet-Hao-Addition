package carpet_hao_addition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.utils.Messenger;
import carpet.utils.Translations;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

import carpet_hao_addition.zoneguard.ZoneguardHooks;
import carpet_hao_addition.zoneguard.ZoneguardSettings;
import carpet_hao_addition.zoneguard.command.ZoneguardCommands;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Carpet-Hao-Addition: a Carpet extension with shared (version-independent) code.
 * <p>
 * The extension registers itself through the Fabric {@link ModInitializer} entrypoint
 * (see fabric.mod.json). Carpet calls {@link CarpetServer#onGameStarted()} from its own
 * client/server entrypoints, which run after ModInitializers, so by then this extension
 * is already registered and its {@link #onGameStarted()} callback will be invoked.
 * <p>
 * User-visible text is internationalized: chat messages use MC translatable keys that
 * live in {@code assets/carpet-hao-addition/lang/{en_us,zh_cn}.json}, while Carpet rule
 * texts are provided per-language through {@link #canHasTranslations(String)}.
 */
public class CarpetHaoAdditionExtension implements CarpetExtension, ModInitializer
{
    public static final String MOD_ID = "carpet-hao-addition";
    /** Identifier of the settings manager, also the name of its command: /haoaddition */
    public static final String MANAGER_ID = "haoaddition";
    public static final String MOD_NAME = "Carpet-Hao-Addition";
    /** Rule name of the zoneguard toggle, registered with Carpet's default manager. */
    public static final String ZONEGUARD_RULE = "zoneguard";

    private final String modVersion;
    private final SettingsManager settingsManager;
    private MinecraftServer server;
    private boolean zoneguardObserverRegistered;

    public CarpetHaoAdditionExtension()
    {
        this.modVersion = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        this.settingsManager = new SettingsManager(modVersion, MANAGER_ID, MOD_NAME);
    }

    @Override
    public void onInitialize()
    {
        CarpetServer.manageExtension(this);
    }

    @Override
    public void onGameStarted()
    {
        // parseSettingsClass internally refreshes carpet's translations first, which
        // collects the keys returned by canHasTranslations() (see below), so the rule
        // parser always finds the required "carpet.rule.zoneguard.desc" key.
        settingsManager.parseSettingsClass(CarpetHaoAdditionSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(ZoneguardSettings.class);

        registerZoneguardRuleObserver();
    }

    @Override
    public void onServerLoaded(MinecraftServer server)
    {
        this.server = server;
    }

    private void registerZoneguardRuleObserver()
    {
        if (this.zoneguardObserverRegistered)
        {
            return;
        }
        this.zoneguardObserverRegistered = true;

        // Toggling the zoneguard rule OFF must restore observers that were frozen while
        // the rule was ON (e.g. face-to-face pairs), otherwise they never update again.
        CarpetServer.settingsManager.registerRuleObserver((source, changedRule, userInput) ->
        {
            if (!ZONEGUARD_RULE.equals(changedRule.name()))
            {
                return;
            }
            if (!(changedRule.value() instanceof Boolean enabled) || enabled)
            {
                // Only react when the rule is turned OFF.
                return;
            }

            MinecraftServer srv = source != null ? source.getServer() : null;
            if (srv == null)
            {
                srv = this.server;
            }
            if (srv != null)
            {
                ZoneguardHooks.refreshAllRegions(srv);
            }
        });
    }

    @Override
    public SettingsManager extensionSettingsManager()
    {
        return settingsManager;
    }

    @Override
    @SuppressWarnings("deprecation") // old registerCommands(dispatcher) is forwarded by Carpet
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        // Example command: /hao
        dispatcher.register(literal("hao").executes(context ->
        {
            Messenger.m(context.getSource(), "w " + Translations.tr(MANAGER_ID + ".command.hao.header"));
            Messenger.m(context.getSource(), "w  - exampleBoolean: " + CarpetHaoAdditionSettings.exampleBoolean);
            Messenger.m(context.getSource(), "w  - exampleString: " + CarpetHaoAdditionSettings.exampleString);
            Messenger.m(context.getSource(), "w " + Translations.tr(MANAGER_ID + ".command.hao.manage"));
            return 1;
        }));

        // /zoneguard command tree lives in the versioned layer (versions/<mc>/src),
        // keeping per-Minecraft-version APIs out of this shared entry point.
        ZoneguardCommands.register(dispatcher);
    }

    @Override
    public String version()
    {
        return modVersion;
    }

    @Override
    public Map<String, String> canHasTranslations(String lang)
    {
        boolean zh = lang != null && lang.toLowerCase(Locale.ROOT).startsWith("zh");
        Map<String, String> translations = new HashMap<>();
        String rulePrefix = MANAGER_ID + ".rule.";
        String commandPrefix = MANAGER_ID + ".command.";

        translations.put(rulePrefix + "exampleBoolean.name", "Example Boolean");
        translations.put(rulePrefix + "exampleBoolean.desc", zh
                ? "本扩展的一个示例布尔规则。"
                : "An example boolean rule of this extension.");
        translations.put(rulePrefix + "exampleString.name", "Example String");
        translations.put(rulePrefix + "exampleString.desc", zh
                ? "一个带几个预设选项的示例字符串规则。"
                : "An example string rule with a few preset options.");
        translations.put(MANAGER_ID + ".category." + MANAGER_ID, MOD_NAME);

        // /hao command example texts (Carpet-rendered, so served from this table).
        translations.put(commandPrefix + "hao.header", zh
                ? "扩展 " + MOD_NAME + " 的当前规则："
                : "Current rules of " + MOD_NAME + ":");
        translations.put(commandPrefix + "hao.manage", zh
                ? "使用 /" + MANAGER_ID + " <规则> <值> 管理它们"
                : "Manage them with /" + MANAGER_ID + " <rule> <value>");

        // zoneguard rule lives in Carpet's DEFAULT settings manager ("/carpet"),
        // under the "Hao" category, so its keys use the "carpet." namespace.
        translations.put("carpet.category.Hao", "Hao");
        translations.put("carpet.rule." + ZONEGUARD_RULE + ".name", ZONEGUARD_RULE);
        translations.put("carpet.rule." + ZONEGUARD_RULE + ".desc", zh
                ? "在 /zoneguard 配置的立方区域内禁用侦测器(观察者)行为。关闭规则会恢复区域内侦测器。"
                : "Disables observers inside the cubic regions configured with /zoneguard. Turning the rule off restores observers in those regions.");

        return translations;
    }
}

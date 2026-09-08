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
        CarpetServer.settingsManager.parseSettingsClass(GoldenCarrotCompostSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(SnowyCalciteSettings.class);

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

        // Toggling the zoneguard rule must take effect immediately for online players:
        // turning it OFF also restores observers that were frozen while it was ON.
        CarpetServer.settingsManager.registerRuleObserver((source, changedRule, userInput) ->
        {
            if (!ZONEGUARD_RULE.equals(changedRule.name()))
            {
                return;
            }
            if (!(changedRule.value() instanceof Boolean enabled))
            {
                return;
            }

            MinecraftServer srv = source != null ? source.getServer() : null;
            if (srv == null)
            {
                srv = this.server;
            }
            if (srv == null)
            {
                return;
            }

            if (!enabled)
            {
                ZoneguardHooks.refreshAllRegions(srv);
            }
            // Clients cache the command tree at login; re-send it so /zoneguard
            // disappears (rule off) or appears (rule on) without relogging.
            ZoneguardHooks.refreshCommandTree(srv);
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
        // All user-visible texts live in the language files under
        // assets/carpet-hao-addition/lang/{en_us,zh_cn}.json — nothing is hardcoded
        // here. Carpet merges the returned map into its translation table (used for
        // rule names/descriptions and the /hao command), and the same keys are also
        // shipped as regular Minecraft lang files for client-side rendering.
        String language = lang == null ? "en_us" : lang;
        Map<String, String> translations = Translations.getTranslationFromResourcePath(
                "assets/" + MOD_ID + "/lang/" + language + ".json");
        if (translations.isEmpty() && !"en_us".equals(language))
        {
            // Fall back to English when the requested language file is missing, so
            // Carpet's rule parser always finds the required name/desc keys.
            translations = Translations.getTranslationFromResourcePath(
                    "assets/" + MOD_ID + "/lang/en_us.json");
        }
        return translations;
    }
}

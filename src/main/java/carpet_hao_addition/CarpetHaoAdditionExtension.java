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
import carpet_hao_addition.portal.PlayerNoEndPortalTeleportCommands;
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
    public static final String HAO_MOD_ID = "carpet-hao-addition";
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
                .getModContainer(HAO_MOD_ID)
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
        CarpetServer.settingsManager.parseSettingsClass(NoEndPortalTeleportSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(TerracottaUncolorSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(BedrockCanBeMinedSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(WitherSkeletonDropReductionSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(DirectDropsSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(LavaDepthStriderSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(EasyPlaceWaterloggedSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(RocketShulkerSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(UseDyeOnShulkerBoxSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CopperStonecuttingSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(AutoMendingSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(EasyPlaceEntitySettings.class);
        CarpetServer.settingsManager.parseSettingsClass(ProjectionPlacementSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CraftableTrimTemplateSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(WorldEaterProMaxSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(WackoBeaconsSettings.class);

        registerZoneguardRuleObserver();
    }

    @Override
    public void onServerLoaded(MinecraftServer server)
    {
        this.server = server;
        // Deploy & enable the terracotta-uncolor datapack right after startup (deferred to
        // the first tick so the level is fully loaded), so recipes work without /reload.
        server.execute(() -> RecipeDeployHooks.ensureDeployed(server));
        // Same treatment for the copper-stonecutter datapack (door / trapdoor / bulb).
        server.execute(() -> ensureCopperStonecutting(server));
        server.execute(() -> ensureTrimTemplates(server));
    }

    @Override
    public void onReload(MinecraftServer server)
    {
        // Ensure the terracotta-uncolor stonecutter recipes are deployed to the world
        // datapack and enabled (fabric-loader does not auto-load mod data/ as a datapack).
        RecipeDeployHooks.ensureDeployed(server);
        ensureCopperStonecutting(server);
        ensureTrimTemplates(server);
    }

    private void registerZoneguardRuleObserver()
    {
        if (this.zoneguardObserverRegistered)
        {
            return;
        }
        this.zoneguardObserverRegistered = true;

        // Toggling rules must take effect immediately for online players:
        // command-gating rules refresh the command tree, and terracottaUncolor
        // enables/disables the recipe datapack (so the UI entries appear/disappear).
        CarpetServer.settingsManager.registerRuleObserver((source, changedRule, userInput) ->
        {
            String ruleName = changedRule.name();
            if (!(changedRule.value() instanceof Boolean enabled))
            {
                return;
            }

            // 铜规则同样靠数据包启停:开关一变就立即重新部署(含 enable/disable 与 reload)。
            if (CopperStonecuttingSettings.RULE_NAME.equals(ruleName))
            {
                MinecraftServer srv = source != null ? source.getServer() : null;
                if (srv == null)
                {
                    srv = this.server;
                }
                if (srv != null)
                {
                    ensureCopperStonecutting(srv);
                }
                return;
            }

            // 可合成纹饰模板规则同样靠数据包启停:开关一变就立即重新部署。
            if (CraftableTrimTemplateSettings.RULE_NAME.equals(ruleName))
            {
                MinecraftServer srv = source != null ? source.getServer() : null;
                if (srv == null)
                {
                    srv = this.server;
                }
                if (srv != null)
                {
                    ensureTrimTemplates(srv);
                }
                return;
            }

            if (TerracottaUncolorSettings.RULE_NAME.equals(ruleName))
            {
                MinecraftServer srv = source != null ? source.getServer() : null;
                if (srv == null)
                {
                    srv = this.server;
                }
                if (srv != null)
                {
                    // Turn the recipe datapack on/off (and reload) right away.
                    RecipeDeployHooks.ensureDeployed(srv);
                }
                return;
            }

            boolean zoneguardRule = ZONEGUARD_RULE.equals(ruleName);
            // Rules whose ON/OFF state hides or reveals commands need a tree refresh.
            boolean commandGatingRule = zoneguardRule || NoEndPortalTeleportSettings.RULE_NAME.equals(ruleName);
            if (!commandGatingRule)
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

            if (!enabled && zoneguardRule)
            {
                // Turning the zoneguard rule OFF also restores observers frozen while ON.
                ZoneguardHooks.refreshAllRegions(srv);
            }
            // Clients cache the command tree at login; re-send it so the gated commands
            // appear (rule on) or disappear (rule off) without relogging.
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
        ZoneguardCommands.registerCommand(dispatcher);

        // /playerNoEndPortalTeleport — manages the noEndPortalTeleport blacklist/global mode.
        PlayerNoEndPortalTeleportCommands.registerCommand(dispatcher);

        // /rocketShulker — 设置火箭潜影盒的补给位置(副手 / 主手快捷栏 1-9)。
        RocketShulkerCommands.registerCommand(dispatcher);

        // /easyPlaceEntityCount — 设置自己一次放置几个实体(1-64)。
        EasyPlaceEntityCommands.registerCommand(dispatcher);
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
                "assets/" + HAO_MOD_ID + "/lang/" + language + ".json");
        if (translations.isEmpty() && !"en_us".equals(language))
        {
            // Fall back to English when the requested language file is missing, so
            // Carpet's rule parser always finds the required name/desc keys.
            translations = Translations.getTranslationFromResourcePath(
                    "assets/" + HAO_MOD_ID + "/lang/en_us.json");
        }
        return translations;
    }

    /**
     * 可合成纹饰模板配方的部署同样只在部分层实现(1.21.8 / 26.3)。
     * <p>
     * 与 {@link #ensureCopperStonecutting} 同理用反射:该层没有
     * {@code TrimTemplateRecipeDeployHook} 时静默跳过,其他层照常编译。
     */
    private static void ensureTrimTemplates(MinecraftServer server)
    {
        try
        {
            Class.forName("carpet_hao_addition.TrimTemplateRecipeDeployHook")
                    .getMethod("ensureDeployed", MinecraftServer.class)
                    .invoke(null, server);
        }
        catch (Throwable ignored)
        {
            // 本层未实现该功能, 跳过
        }
    }

    /**
     * 铜切石机配方部署只在部分层实现(1.21.8 / 1.21.10 / 1.21.11)。
     * <p>
     * 这里用反射调用:该层没有 {@code CopperStonecuttingDeployHook} 时静默跳过,
     * 这样 1.21 ~ 1.21.6 等层依然可以正常编译(它们本来就没有这个功能)。
     */
    private static void ensureCopperStonecutting(MinecraftServer server)
    {
        try
        {
            Class.forName("carpet_hao_addition.CopperStonecuttingDeployHook")
                    .getMethod("ensureDeployed", MinecraftServer.class)
                    .invoke(null, server);
        }
        catch (Throwable ignored)
        {
            // 本层未实现该功能, 跳过
        }
    }
}

package carpet_hao_addition;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把"可合成纹饰模板"的 19 条合成配方部署到当前世界的数据包,按 {@code haoCraftableTrimTemplates} 规则启停。
 * <p>
 * 配方 = <b>原版"复制配方"去掉必须的本体模板</b>:7 个钻石 + 1 个该模板的获取途径材料(C)→ 1 个模板。
 * 原版复制配方的形状是 {@code #S# / #C# / ###}(# 钻石、S 本体模板、C 材料),
 * 这里把 S 那格留空就成了"从零合成"。
 * <p>
 * 做法与 {@link RecipeDeployHooks}(陶瓦还原)完全一致:fabric-loader 不会自动把 mod 的 data/ 当数据包,
 * 所以由本类在服务器首个 tick、数据包重载、规则变化时:
 * 1. 把配方写进 world/datapacks/hao-trim-templates/(内容变化才重写);
 * 2. 规则开启 → reload + addPack;关闭 → removePack;
 * 3. 仅状态变化时触发 reload,幂等。
 */
public final class TrimTemplateRecipeDeployHook {
	private static final Logger HAO_LOGGER = LoggerFactory.getLogger("carpet-hao-addition");
	private static final String PACK_NAME = "hao-trim-templates";
	private static final String PACK_ID = "file/" + PACK_NAME;
	/** 26.x 的数据包格式上限(与 RecipeDeployHooks 保持一致)。 */
	private static final int PACK_FORMAT_MIN = 48;
	private static final int PACK_FORMAT_MAX = 121;

	// 注意:材料(C)**必须互不相同** —— 同一组材料若有多条配方,MC 只会认其中一条,
	// 其他模板就永远合不出来(因为本体模板那格是空的,没法用来区分)。所以这里按"该模板的获取
	// 地点"各挑一个独有的代表方块,而不是照抄原版复制配方的材料(原版有重复,它靠本体模板区分)。
	private static final String[][] TEMPLATES = {
			{"minecraft:sentry_armor_trim_smithing_template", "\"minecraft:cobblestone\""},          // 掠夺者前哨站
			{"minecraft:vex_armor_trim_smithing_template", "\"minecraft:amethyst_shard\""},          // 林地府邸;青紫配色
			{"minecraft:wild_armor_trim_smithing_template", "\"minecraft:mossy_cobblestone\""},      // 丛林神庙
			{"minecraft:coast_armor_trim_smithing_template", "\"minecraft:prismarine_shard\""},      // 沉船;青白配色
			{"minecraft:dune_armor_trim_smithing_template", "\"minecraft:sandstone\""},              // 沙漠神殿
			{"minecraft:wayfinder_armor_trim_smithing_template", "\"minecraft:terracotta\""},        // 古迹废墟
			{"minecraft:shaper_armor_trim_smithing_template", "\"minecraft:mud_bricks\""},           // 古迹废墟
			{"minecraft:host_armor_trim_smithing_template", "\"minecraft:mud\""},                    // 古迹废墟
			{"minecraft:raiser_armor_trim_smithing_template", "\"minecraft:gravel\""},               // 古迹废墟
			{"minecraft:ward_armor_trim_smithing_template", "\"minecraft:cobbled_deepslate\""},      // 远古城市
			{"minecraft:silence_armor_trim_smithing_template", "\"minecraft:sculk\""},               // 远古城市
			{"minecraft:eye_armor_trim_smithing_template", "\"minecraft:end_stone\""},               // 要塞(末地传送门);黄白配色
			{"minecraft:spire_armor_trim_smithing_template", "\"minecraft:purpur_block\""},          // 末地城
			{"minecraft:tide_armor_trim_smithing_template", "\"minecraft:prismarine\""},             // 海底神殿
			{"minecraft:snout_armor_trim_smithing_template", "\"minecraft:blackstone\""},             // 堡垒遗迹
			{"minecraft:rib_armor_trim_smithing_template", "\"minecraft:nether_bricks\""},            // 下界要塞
			{"minecraft:flow_armor_trim_smithing_template", "\"minecraft:breeze_rod\""},              // 试炼密室
			{"minecraft:bolt_armor_trim_smithing_template",                                           // 试炼密室
					"[\"minecraft:copper_block\", \"minecraft:waxed_copper_block\"]"},
			{"minecraft:netherite_upgrade_smithing_template", "\"minecraft:obsidian\""}               // 堡垒遗迹
	};

	private TrimTemplateRecipeDeployHook() {
	}

	public static void ensureDeployed(MinecraftServer server) {
		try {
			Path packDir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
			boolean changed = false;

			Files.createDirectories(packDir);
			changed |= writeIfChanged(packDir.resolve("pack.mcmeta"), mcmetaContent());

			Path dir = packDir.resolve("data/carpet-hao-addition/recipe");
			Files.createDirectories(dir);
			for (String[] template : TEMPLATES) {
				changed |= writeIfChanged(dir.resolve(recipeFileName(template[0])),
						recipeJson(template[0], template[1]));
			}

			boolean wantEnabled = CraftableTrimTemplateSettings.isEnabled();
			PackRepository packManager = server.getPackRepository();
			packManager.reload();
			boolean enabled = packManager.getSelectedIds().contains(PACK_ID);
			if (wantEnabled && packManager.getPack(PACK_ID) != null && !enabled) {
				packManager.addPack(PACK_ID);
				changed = true;
				HAO_LOGGER.info("Enabled datapack '{}' ({} craftable trim-template recipes).",
						PACK_NAME, TEMPLATES.length);
			} else if (!wantEnabled && enabled) {
				packManager.removePack(PACK_ID);
				changed = true;
				HAO_LOGGER.info("Disabled datapack '{}' (rule haoCraftableTrimTemplates is off).", PACK_NAME);
			}

			if (changed) {
				server.reloadResources(packManager.getSelectedIds());
			}
		} catch (IOException e) {
			HAO_LOGGER.error("Failed to deploy craftable trim-template datapack", e);
		}
	}

	/** minecraft:coast_armor_trim_smithing_template → craftable_coast_armor_trim_smithing_template.json */
	private static String recipeFileName(String templateId) {
		return "craftable_" + templateId.substring("minecraft:".length()) + ".json";
	}

	private static boolean writeIfChanged(Path target, String content) throws IOException {
		if (!Files.exists(target) || !Files.readString(target, StandardCharsets.UTF_8).equals(content)) {
			Files.writeString(target, content, StandardCharsets.UTF_8);
			return true;
		}
		return false;
	}

	private static String mcmetaContent() {
		// 四个字段同时写:1.21.x 认 pack_format/supported_formats,26.x 认 min_format/max_format。
		return """
				{
				  "pack": {
				    "description": "Carpet Hao Addition - craftable trim templates",
				    "pack_format": %2$d,
				    "supported_formats": { "min_inclusive": %1$d, "max_inclusive": %2$d },
				    "min_format": [%1$d, 0],
				    "max_format": [%2$d, 0]
				  }
				}
				""".formatted(PACK_FORMAT_MIN, PACK_FORMAT_MAX);
	}

	/** 无本体模板的合成配方:7 钻石 + 1 个 C,产出 1 个模板。 */
	private static String recipeJson(String templateId, String materialJson) {
		return """
				{
				  "type": "minecraft:crafting_shaped",
				  "key": {
				    "#": "minecraft:diamond",
				    "C": %s
				  },
				  "pattern": [
				    "# #",
				    "#C#",
				    "###"
				  ],
				  "result": {
				    "count": 1,
				    "id": "%s"
				  }
				}
				""".formatted(materialJson, templateId);
	}
}

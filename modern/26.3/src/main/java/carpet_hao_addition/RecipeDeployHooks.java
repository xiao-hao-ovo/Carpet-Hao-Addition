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
 * 把切石机“还原”配方自动部署到当前世界的数据包,并按 terracottaUncolor 规则启停。
 * <p>
 * 配方(共 32 条,规则开启时才对玩家可见):
 * - 16 色染色陶瓦 → 原色陶瓦(terracotta);
 * - 16 色釉陶瓦(glazed terracotta)→ 同色染色陶瓦。
 * <p>
 * fabric-loader 不会自动把 mod 的 data/ 当数据包,因此由本类在服务器首个 tick
 * (onServerLoaded)与每次数据包重载(onReload)、规则变化时:
 * 1. 把配方写入 world/datapacks/carpet-hao-addition_terracotta_uncolor/(内容变化才重写);
 * 2. 规则开启 → scanPacks+enable;规则关闭 → disable(切石机不再出现还原配方);
 * 3. 仅状态变化时触发 reload,幂等。
 */
public final class RecipeDeployHooks {
	private static final Logger LOGGER = LoggerFactory.getLogger("carpet-hao-addition");
	private static final String PACK_NAME = "carpet-hao-addition_terracotta_uncolor";
	private static final String PACK_ID = "file/" + PACK_NAME;
	/**
	 * 数据包格式范围,让同一份 pack.mcmeta 在 1.21.x ~ 26.3 都能加载:
	 * <ul>
	 *   <li>1.21.x 读 {@code pack_format} / {@code supported_formats}(字段名如此,数值 48~81);</li>
	 *   <li>26.x 读 {@code min_format} / {@code max_format}(格式号 >81 时**必填**,用 [major, minor]);
	 *       它同时认识另外两个字段(元数据 section 里四个都是可选字段),所以能共存。</li>
	 * </ul>
	 * 值取自各版本 version.json 的 pack_version.data_major:1.21.x = 48 起,26.3 = 121。
	 */
	private static final int PACK_FORMAT_MIN = 48;
	private static final int PACK_FORMAT_MAX = 121;

	private static final String[] COLORS = {
			"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
			"gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
	};

	private RecipeDeployHooks() {
	}

	public static void ensureDeployed(MinecraftServer server) {
		try {
			Path packDir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
			boolean changed = false;

			// 只在"文件不存在"时写,会让旧世界永远带着旧的 pack.mcmeta:
			// 26.x 起格式号 >81 必须带 min_format/max_format,旧格式整个数据包被判非法、
			// 石切机配方静默失效(启动只刷一条 JsonParseException 警告)。所以按内容比对重写。
			Path mcmeta = packDir.resolve("pack.mcmeta");
			changed |= writeIfChanged(mcmeta, mcmetaContent());
			// 配方目录名跨版本不同:1.21 / 1.21.1 用复数 recipes,1.21.2+ 与 26.x 用单数 recipe。
			// 两份都写,各版本各取所需(不认识的目录会被忽略),这样这条规则在 1.21.x ~ 26.3 都能生效。
			for (String recipeDirName : new String[] {"recipe", "recipes"}) {
				Path dir = packDir.resolve("data/carpet-hao-addition/" + recipeDirName);
				Files.createDirectories(dir);
				for (String color : COLORS) {
					changed |= writeIfChanged(dir.resolve(color + "_terracotta_to_terracotta.json"),
							recipeJson("minecraft:" + color + "_terracotta", "minecraft:terracotta"));
					changed |= writeIfChanged(dir.resolve(color + "_glazed_terracotta_to_terracotta.json"),
							recipeJson("minecraft:" + color + "_glazed_terracotta",
									"minecraft:" + color + "_terracotta"));
				}
			}

			boolean wantEnabled = TerracottaUncolorSettings.isEnabled();
			PackRepository packManager = server.getPackRepository();
			packManager.reload();
			boolean enabled = packManager.getSelectedIds().contains(PACK_ID);
			if (wantEnabled && packManager.getPack(PACK_ID) != null && !enabled) {
				packManager.addPack(PACK_ID);
				changed = true;
				LOGGER.info("Enabled datapack '{}' (terracotta/glazed uncolor recipes).", PACK_NAME);
			} else if (!wantEnabled && enabled) {
				packManager.removePack(PACK_ID);
				changed = true;
				LOGGER.info("Disabled datapack '{}' (rule terracottaUncolor is off).", PACK_NAME);
			}

			if (changed) {
				server.reloadResources(packManager.getSelectedIds());
			}
		} catch (IOException e) {
			LOGGER.error("Failed to deploy terracotta-uncolor datapack", e);
		}
	}

	private static boolean writeIfChanged(Path target, String content) throws IOException {
		if (!Files.exists(target) || !Files.readString(target, StandardCharsets.UTF_8).equals(content)) {
			Files.writeString(target, content, StandardCharsets.UTF_8);
			return true;
		}
		return false;
	}

	private static String mcmetaContent() {
		// 四个字段同时写:1.21.x 认 pack_format/supported_formats,26.x 认 min_format/max_format
		// (26.x 的元数据 section 这四个都是可选字段,多余的会被忽略),这样一份数据包跨版本可用。
		return """
				{
				  "pack": {
				    "description": "Carpet Hao Addition - terracotta/glazed uncolor (stonecutter)",
				    "pack_format": %2$d,
				    "supported_formats": { "min_inclusive": %1$d, "max_inclusive": %2$d },
				    "min_format": [%1$d, 0],
				    "max_format": [%2$d, 0]
				  }
				}
				""".formatted(PACK_FORMAT_MIN, PACK_FORMAT_MAX);
	}


	private static String recipeJson(String ingredient, String result) {
		return """
				{
				  "type": "minecraft:stonecutting",
				  "ingredient": "%s",
				  "result": {
				    "id": "%s",
				    "count": 1
				  }
				}
				""".formatted(ingredient, result);
	}
}

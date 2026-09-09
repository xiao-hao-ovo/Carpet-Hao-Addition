package carpet_hao_addition;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

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
	private static final int PACK_FORMAT = 88; // Minecraft 1.21.9–1.21.10 数据包格式

	private static final String[] COLORS = {
			"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
			"gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
	};

	private RecipeDeployHooks() {
	}

	public static void ensureDeployed(MinecraftServer server) {
		try {
			Path packDir = server.getSavePath(WorldSavePath.DATAPACKS).resolve(PACK_NAME);
			boolean changed = false;

			Path mcmeta = packDir.resolve("pack.mcmeta");
			if (!Files.exists(mcmeta)) {
				Files.createDirectories(packDir.resolve("data/carpet-hao-addition/recipe"));
				Files.writeString(mcmeta, mcmetaContent(), StandardCharsets.UTF_8);
				changed = true;
			}
			for (String color : COLORS) {
				Path dir = packDir.resolve("data/carpet-hao-addition/recipe");
				changed |= writeIfChanged(dir.resolve(color + "_terracotta_to_terracotta.json"),
						recipeJson("minecraft:" + color + "_terracotta", "minecraft:terracotta"));
				changed |= writeIfChanged(dir.resolve(color + "_glazed_terracotta_to_terracotta.json"),
						recipeJson("minecraft:" + color + "_glazed_terracotta", "minecraft:" + color + "_terracotta"));
			}

			boolean wantEnabled = TerracottaUncolorSettings.isEnabled();
			ResourcePackManager packManager = server.getDataPackManager();
			packManager.scanPacks();
			boolean enabled = packManager.getEnabledIds().contains(PACK_ID);
			if (wantEnabled && packManager.getProfile(PACK_ID) != null && !enabled) {
				packManager.enable(PACK_ID);
				changed = true;
				LOGGER.info("Enabled datapack '{}' (terracotta/glazed uncolor recipes).", PACK_NAME);
			} else if (!wantEnabled && enabled) {
				packManager.disable(PACK_ID);
				changed = true;
				LOGGER.info("Disabled datapack '{}' (rule terracottaUncolor is off).", PACK_NAME);
			}

			if (changed) {
				server.reloadResources(packManager.getEnabledIds());
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
		return """
				{
				  "pack": {
				    "pack_format": %d,
				    "description": "Carpet Hao Addition - terracotta/glazed uncolor (stonecutter)"
				  }
				}
				""".formatted(PACK_FORMAT);
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

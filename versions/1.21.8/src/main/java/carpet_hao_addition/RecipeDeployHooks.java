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
 * 把 16 条“染色陶瓦→陶瓦”切石配方自动部署到当前世界的数据包并启用。
 * <p>
 * fabric-loader 不会自动把 mod 的 data/ 当数据包(那是 fabric-api 的行为),
 * 因此这里在服务器启动后的首个 tick(onServerLoaded)与每次数据包重载(onReload)
 * 时确保世界内存在该数据包并已启用:
 * 1. 需要时写入 world/datapacks/carpet-hao-addition_terracotta_uncolor/
 *    (pack.mcmeta + 配方 json),配方内容有变化则重写;
 * 2. 让 pack 管理器扫描到该 file pack 并启用;
 * 3. 仅当本次部署发生了变更/首次启用时才触发资源重载,幂等且不反复 reload。
 */
public final class RecipeDeployHooks {
	private static final Logger LOGGER = LoggerFactory.getLogger("carpet-hao-addition");
	private static final String PACK_NAME = "carpet-hao-addition_terracotta_uncolor";
	private static final String PACK_ID = "file/" + PACK_NAME;
	private static final int PACK_FORMAT = 81; // Minecraft 1.21.7–1.21.8 数据包格式

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
				Path target = packDir.resolve("data/carpet-hao-addition/recipe")
						.resolve(color + "_terracotta_to_terracotta.json");
				String expected = recipeJson(color);
				if (!Files.exists(target) || !Files.readString(target, StandardCharsets.UTF_8).equals(expected)) {
					Files.writeString(target, expected, StandardCharsets.UTF_8);
					changed = true;
				}
			}

			ResourcePackManager packManager = server.getDataPackManager();
			packManager.scanPacks();
			if (packManager.getProfile(PACK_ID) != null && !packManager.getEnabledIds().contains(PACK_ID)) {
				packManager.enable(PACK_ID);
				changed = true;
				LOGGER.info("Enabled datapack '{}' with terracotta uncolor recipes.", PACK_NAME);
			}

			if (changed) {
				// 数据包首次部署或内容更新:重载资源使(新)配方生效。
				server.reloadResources(packManager.getEnabledIds());
			}
		} catch (IOException e) {
			LOGGER.error("Failed to deploy terracotta-uncolor datapack", e);
		}
	}

	private static String mcmetaContent() {
		return """
				{
				  "pack": {
				    "pack_format": %d,
				    "description": "Carpet Hao Addition - terracotta uncolor (stonecutter)"
				  }
				}
				""".formatted(PACK_FORMAT);
	}

	private static String recipeJson(String color) {
		return """
				{
				  "type": "minecraft:stonecutting",
				  "ingredient": "minecraft:%s_terracotta",
				  "result": {
				    "id": "minecraft:terracotta",
				    "count": 1
				  }
				}
				""".formatted(color);
	}
}

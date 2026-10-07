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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 把"切石机切铜"配方自动部署到当前世界的数据包,并按 copperStonecuttingRecipes 规则启停。
 * 做法与 {@link RecipeDeployHooks}(陶瓦还原)一致。
 * <p>
 * <b>核心原则:按"当量"转换,绝不产生套利。</b>
 * 每种铜形态有一个当量(取自原版配方,以 1 个铜块 = {@value #BLOCK_VALUE} 为单位):
 * <ul>
 *   <li>铜块 = 8</li>
 *   <li>切制铜块 / 凿制铜块 / 铜格栅 / 楼梯 / 铜门 / 铜活板门 / 铜灯 = 2</li>
 *   <li>切制铜台阶 = 1</li>
 * </ul>
 * 原版配方正好自洽(1 铜块 → 4 切制 = 4×2 ✓;1 铜块 → 8 台阶 = 8×1 ✓;
 * 1 切制 → 2 台阶 = 2×1 ✓)。本类只生成"<b>源当量 &ge; 目标当量且能整除</b>"的方向
 * (产出数 = 源当量 / 目标当量),因此是等值互换 + 单向降级:
 * 铜块 → 二当量组 → 台阶,同当量形态之间 1:1 互转,
 * 不可能出现"切来切去越切越多"的循环。原版已有的方向会跳过。
 * <p>
 * 另有一组"同一种形态在 8 个变体之间 1:1 互换"(涂蜡 ↔ 未涂蜡、各氧化等级),
 * 以及"任意变体的铜块 → 任意其它变体的任意形态"(一步到位),当量都不变,同样不产生套利。
 * <p>
 * fabric-loader 不会自动把 mod 的 data/ 当数据包,因此由本类在服务器首个 tick
 * 与每次数据包重载、规则变化时:
 * 1. 把配方写入 world/datapacks/hao-copper-cut/
 *    (同时删除本目录里不再需要的旧配方,避免规则调整后残留失效配方);
 * 2. 规则开启 → scanPacks+enable;规则关闭 → disable;
 * 3. 仅内容变化时触发 reload,幂等。
 */
public final class CopperStonecuttingDeployHook {
	private static final Logger LOGGER = LoggerFactory.getLogger("carpet-hao-addition");
	private static final String PACK_NAME = "hao-copper-cut";
	private static final String PACK_ID = "file/" + PACK_NAME;
	/** 跨版本格式范围:1.21.x 读 pack_format/supported_formats,26.x 读 min_format/max_format。 */
	private static final int PACK_FORMAT_MIN = 48;
	private static final int PACK_FORMAT_MAX = 121;

	/**
	 * 8 个铜变体的前缀:4 个氧化等级 × 未涂蜡 / 涂蜡。
	 * 每组(前者未涂蜡、后者涂蜡)的第 0 个是"未氧化",后 3 个依次是暴露 / 风化 / 氧化。
	 */
	private static final String[] PREFIXES = {
			"", "exposed_", "weathered_", "oxidized_",
			"waxed_", "waxed_exposed_", "waxed_weathered_", "waxed_oxidized_"
	};

	/**
	 * 除"铜块"外的 8 种形态,物品 ID 一律是 {@code 前缀 + 后缀}。
	 * 顺序与 {@link #SHAPE_VALUES} 的第 1..8 项一一对应。
	 */
	private static final String[] SHAPE_SUFFIXES = {
			"cut_copper", "chiseled_copper", "copper_grate",
			"cut_copper_slab", "cut_copper_stairs",
			"copper_door", "copper_trapdoor", "copper_bulb"
	};

	/** 铜块的当量(以"1 铜块 = 8"为单位,好让所有换算都是整数)。 */
	private static final int BLOCK_VALUE = 8;
	/** 二当量组的当量:一个铜块能切出 4 个。 */
	private static final int MID_VALUE = 2;
	/** 切制铜台阶的当量:一个铜块能切出 8 个。 */
	private static final int SLAB_VALUE = 1;

	/**
	 * 9 种形态的当量,顺序与 {@link #shapeId} 的 {@code shape} 参数一致:
	 * 铜块 / 切制 / 凿制 / 格栅 / 台阶 / 楼梯 / 门 / 活板门 / 灯。
	 * <p>
	 * 门、活板门、灯按"1 铜块 → 4 个"(与原版切石机对其它 2 当量形态一致)折算。
	 */
	private static final int[] SHAPE_VALUES = {
			BLOCK_VALUE,
			MID_VALUE, MID_VALUE, MID_VALUE,
			SLAB_VALUE,
			MID_VALUE, MID_VALUE, MID_VALUE, MID_VALUE,
	};

	/** 形态总数(铜块 + {@link #SHAPE_SUFFIXES});形态索引 0 = 铜块。 */
	private static final int SHAPE_COUNT = SHAPE_SUFFIXES.length + 1;

	private CopperStonecuttingDeployHook() {
	}

	public static void ensureDeployed(MinecraftServer server) {
		try {
			Path packDir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
			boolean changed = false;

			// mcmeta 内容不一致就重写(旧格式不再被新版本接受),并按版本写 recipe / recipes 两份。
			Files.createDirectories(packDir);
			Files.createDirectories(packDir.resolve("data/carpet-hao-addition/recipe"));
			changed |= writeIfChanged(packDir.resolve("pack.mcmeta"), mcmetaContent());
			Path dir = packDir.resolve("data/carpet-hao-addition/recipe");
			Files.createDirectories(dir);

			Map<String, String> recipes = new LinkedHashMap<>();

			// 1) 同一变体内:按当量从"高"切向"低"(产出数 = 源当量 / 目标当量)。
			for (String prefix : PREFIXES) {
				for (int from = 0; from < SHAPE_COUNT; from++) {
					for (int to = 0; to < SHAPE_COUNT; to++) {
						if (from == to) {
							continue;
						}
						int fromValue = SHAPE_VALUES[from];
						int toValue = SHAPE_VALUES[to];
						// 只做"等值或降级、且能整除"的方向:等值时 1:1 互换,
						// 降级时按当量比例;升级方向需要分数个产物,做不了也会被拿来套利。
						if (fromValue < toValue || fromValue % toValue != 0) {
							continue;
						}
						String fromId = shapeId(prefix, from);
						String toId = shapeId(prefix, to);
						if (vanillaAlreadyHas(prefix, fromId, toId)) {
							continue; // 原版切石机已有该方向(比例与本表一致),不重复生成
						}
						hao$add(recipes,
								recipeJson("minecraft:" + fromId, "minecraft:" + toId, fromValue / toValue));
					}
				}
			}

			// 2) 同一种形态在 8 个变体之间 1:1 互换(涂蜡 ↔ 未涂蜡、各氧化等级互切)。
			for (int shape = 0; shape < SHAPE_COUNT; shape++) {
				for (String fromPrefix : PREFIXES) {
					for (String toPrefix : PREFIXES) {
						if (fromPrefix.equals(toPrefix)) {
							continue;
						}
						String fromId = shapeId(fromPrefix, shape);
						String toId = shapeId(toPrefix, shape);
						hao$add(recipes,
								recipeJson("minecraft:" + fromId, "minecraft:" + toId, 1));
					}
				}
			}

			// 3) 任意变体的铜块 → 任意【其它】变体的任意形态(一步到位,不用先转变体再转形态)。
			//    目标当量 ≤ 8,所以产出数 = 8 / 目标当量,依旧是整数,依旧严格等值。
			for (String fromPrefix : PREFIXES) {
				String fromId = blockId(fromPrefix);
				for (String toPrefix : PREFIXES) {
					if (toPrefix.equals(fromPrefix)) {
						continue; // 同变体的各形态已在第 1 组覆盖
					}
					for (int to = 1; to < SHAPE_COUNT; to++) {
						// 目标从 1 开始:目标是"铜块"的情形属于同形态跨变体,第 2 组已覆盖。
						String toId = shapeId(toPrefix, to);
						hao$add(recipes,
								recipeJson("minecraft:" + fromId, "minecraft:" + toId,
										BLOCK_VALUE / SHAPE_VALUES[to]));
					}
				}
			}

			// 写盘:先删掉本目录里不再需要的旧配方(规则改版后不会残留失效配方),再逐个写。
			try (var existing = Files.list(dir)) {
				for (Path old : existing.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
					if (!recipes.containsKey(old.getFileName().toString())) {
						Files.delete(old);
						changed = true;
					}
				}
			}
			for (Map.Entry<String, String> entry : recipes.entrySet()) {
				changed |= writeIfChanged(dir.resolve(entry.getKey()), entry.getValue());
			}

			boolean wantEnabled = CopperStonecuttingSettings.isEnabled();
			PackRepository packManager = server.getPackRepository();
			packManager.reload();
			boolean enabled = packManager.getSelectedIds().contains(PACK_ID);
			if (wantEnabled && packManager.getPack(PACK_ID) != null && !enabled) {
				packManager.addPack(PACK_ID);
				changed = true;
				LOGGER.info("Enabled datapack '{}' ({} copper stonecutting recipes).",
						PACK_NAME, recipes.size());
			} else if (!wantEnabled && enabled) {
				packManager.removePack(PACK_ID);
				changed = true;
				LOGGER.info("Disabled datapack '{}' (rule copperStonecuttingRecipes is off).", PACK_NAME);
			}

			if (changed) {
				server.reloadResources(packManager.getSelectedIds());
			}
		} catch (IOException e) {
			LOGGER.error("Failed to deploy copper-stonecutting datapack", e);
		}
	}

	/**
	 * 某个变体下第 {@code shape} 种形态的物品 ID。
	 * {@code shape == 0} 是"铜块"(ID 规则特殊,见 {@link #blockId}),
	 * 其余依次对应 {@link #SHAPE_SUFFIXES}。
	 */
	private static String shapeId(String prefix, int shape) {
		return shape == 0 ? blockId(prefix) : prefix + SHAPE_SUFFIXES[shape - 1];
	}

	/**
	 * 该前缀下"铜块"的物品 ID。
	 * <p>
	 * 只有**未氧化**的两个前缀("" 与 "waxed_")才带 {@code _block} 后缀:
	 * {@code copper_block} / {@code waxed_copper_block};氧化后是 {@code exposed_copper} 这类。
	 */
	private static String blockId(String prefix) {
		return prefix + "copper" + (prefix.isEmpty() || "waxed_".equals(prefix) ? "_block" : "");
	}

	/**
	 * 原版切石机是否已经有这个方向(比例与本类的当量表一致,所以只需要跳过不重复生成)。
	 * <p>
	 * 原版已有:铜块 → 切制 / 凿制 / 格栅 / 台阶 / 楼梯,以及切制 → 凿制 / 台阶 / 楼梯。
	 */
	private static boolean vanillaAlreadyHas(String prefix, String from, String to) {
		String block = blockId(prefix);
		String cut = prefix + "cut_copper";
		String chiseled = prefix + "chiseled_copper";
		String grate = prefix + "copper_grate";
		String slab = prefix + "cut_copper_slab";
		String stairs = prefix + "cut_copper_stairs";
		if (from.equals(block)) {
			return to.equals(cut) || to.equals(chiseled) || to.equals(grate)
					|| to.equals(slab) || to.equals(stairs);
		}
		if (from.equals(cut)) {
			return to.equals(chiseled) || to.equals(stairs) || to.equals(slab);
		}
		return false;
	}

	/**
	 * 把一条配方放进待写表,文件名用<b>短序号</b>。
	 * <p>
	 * 原先用 "{来源}_to_{目标}.json" 命名,最长 76 个字符;叠上世界的
	 * {@code world/datapacks/<pack>/data/<ns>/recipe/} 与备份插件(Prime Backup 等)的临时目录前缀后,
	 * 整个路径会超过 Windows 的 260 字符上限 —— 备份/回档在清理临时目录时会报 WinError 3 而失败。
	 * 配方内容自带 ingredient/result,文件名取什么都一样,所以压到最短最稳。
	 */
	private static void hao$add(Map<String, String> recipes, String json) {
		recipes.put(String.format("r%04d.json", recipes.size() + 1), json);
	}

	private static boolean writeIfChanged(Path target, String content) throws IOException {
		if (!Files.exists(target) || !Files.readString(target, StandardCharsets.UTF_8).equals(content)) {
			Files.writeString(target, content, StandardCharsets.UTF_8);
			return true;
		}
		return false;
	}

	private static String mcmetaContent() {
		// 四个字段同时写:1.21.x 与 26.x 各取所需,一份数据包跨版本可用。
		return """
				{
				  "pack": {
				    "description": "Carpet Hao Addition - copper stonecutting (equal-value conversion)",
				    "pack_format": %2$d,
				    "supported_formats": { "min_inclusive": %1$d, "max_inclusive": %2$d },
				    "min_format": [%1$d, 0],
				    "max_format": [%2$d, 0]
				  }
				}
				""".formatted(PACK_FORMAT_MIN, PACK_FORMAT_MAX);
	}


	private static String recipeJson(String ingredient, String result, int count) {
		return """
				{
				  "type": "minecraft:stonecutting",
				  "ingredient": "%s",
				  "result": {
				    "id": "%s",
				    "count": %d
				  }
				}
				""".formatted(ingredient, result, count);
	}
}

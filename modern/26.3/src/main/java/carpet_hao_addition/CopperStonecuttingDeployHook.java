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
 * 每种铜形态有一个当量,刻度取"1/8 铜粒"(这样原版的粒 / 锭 / 切石机比例全都落在整数上),
 * 以 1 个铜块 = {@value #BLOCK_VALUE} 为单位。当量一律由原版配方反推:
 * <ul>
 *   <li>铜粒 {@value #NUGGET_VALUE} &lt; 铜锭 {@value #INGOT_VALUE} &lt; 铜块 {@value #BLOCK_VALUE}</li>
 *   <li>切制 / 凿制 / 铜格栅 / 切制楼梯 = 1 铜块 → 4 个(切石机)</li>
 *   <li>切制铜台阶 = 1 铜块 → 8 个(切石机)</li>
 *   <li>铜灯 = 4 铜块 → 4 个,即 1 铜块 1 个(合成)</li>
 *   <li>铜门 = 6 铜锭 → 3 个,铜活板门 = 4 铜锭 → 1 个(合成)</li>
 *   <li>铜栏杆 = 6 铜锭 → 16 个,铜链 = 1 铜锭 + 2 铜粒 → 1 个(合成)</li>
 *   <li>铜箱子 = 8 铜锭 + 木箱 → 1 个,铜火把 = 1 铜粒 → 4 个,铜灯笼 = 8 铜粒 + 1 铜火把 → 1 个</li>
 * </ul>
 * 掺了非铜材料(木箱 / 木棍 / 煤 / 烈焰棒 / 红石)的配方只按铜的部分计价,所以既不会比原版亏,
 * 也不会被拿来套利。铜矛(木棍 + 铜工具材料)不是纯铜产物,不参与。
 * <p>
 * 本类只生成"<b>源当量 &ge; 目标当量</b>"的方向,产出数 = 源当量 / 目标当量(向下取整)。
 * 原版配方里有些比例除不尽(1 铜块 = 4.5 铜门 = 11 铜链 = 2.25 铜活板门),这些方向按向下取整
 * 生成 —— 只会少给几个零头,反向又不生成,所以依旧不可能越切越多。原版切石机已有的方向会跳过。
 * <p>
 * 产出数还会被 {@link #MAX_RESULT_COUNT}(99)卡一道:26.x 的 stonecutting 限制了
 * {@code result.count} 的取值范围,超限的配方会让整个配方注册表重载失败(连原版配方一起消失),
 * 所以超限的方向宁可不生成。目前只有"→ 铜火把"这一类会越界(它的当量太小),
 * 想大量要火把可以分两步:铜块 → 铜台阶 ×8,再 台阶 → 铜火把 ×40。
 * <p>
 * 另有一组"同一种形态在 8 个变体之间 1:1 互换"(涂蜡 ↔ 未涂蜡、各氧化等级互切),
 * 以及"任意变体的铜块 → 任意其它变体的任意形态"(一步到位,不用先转变体再转形态),
 * 当量都不变,同样不产生套利。铜火把没有氧化 / 涂蜡变体,只有 {@code copper_torch} 一个 ID,
 * 这两组里会自动跳过。
 * <p>
 * fabric-loader 不会自动把 mod 的 data/ 当数据包,因此由本类在服务器首个 tick
 * 与每次数据包重载、规则变化时:
 * 1. 把配方写入 world/datapacks/hao-copper-cut/
 *    (同时删除本目录里不再需要的旧配方,避免规则调整后残留失效配方);
 * 2. 规则开启 → scanPacks+enable;规则关闭 → disable;
 * 3. 仅内容变化时触发 reload,幂等。
 */
public final class CopperStonecuttingDeployHook {
	private static final Logger HAO_LOGGER = LoggerFactory.getLogger("carpet-hao-addition");
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
	 * 除"铜块"外的 13 种形态,物品 ID 一律是 {@code 前缀 + 后缀}。
	 * 顺序与 {@link #SHAPE_VALUES} 的第 1..13 项一一对应。
	 */
	private static final String[] SHAPE_SUFFIXES = {
			"cut_copper", "chiseled_copper", "copper_grate",
			"cut_copper_slab", "cut_copper_stairs",
			"copper_door", "copper_trapdoor", "copper_bulb",
			"copper_bars", "copper_chain", "copper_chest",
			"copper_lantern", "copper_torch"
	};

	/** 铜粒的当量。整个当量表以"1/8 铜粒"为单位,好让原版所有比例都是整数。 */
	private static final int NUGGET_VALUE = 8;
	/** 铜锭的当量(1 铜锭 = 9 铜粒)。 */
	private static final int INGOT_VALUE = NUGGET_VALUE * 9;
	/** 铜块的当量(1 铜块 = 9 铜锭 = 81 铜粒)。 */
	private static final int BLOCK_VALUE = INGOT_VALUE * 9;
	/** 切制 / 凿制 / 格栅 / 楼梯的当量:原版切石机 1 铜块 → 4 个。 */
	private static final int CUT_VALUE = BLOCK_VALUE / 4;
	/** 切制铜台阶的当量:原版切石机 1 铜块 → 8 个。 */
	private static final int SLAB_VALUE = BLOCK_VALUE / 8;
	/** 铜门的当量:原版合成 6 铜锭 → 3 个。 */
	private static final int DOOR_VALUE = INGOT_VALUE * 2;
	/** 铜活板门的当量:原版合成 4 铜锭 → 1 个。 */
	private static final int TRAPDOOR_VALUE = INGOT_VALUE * 4;
	/** 铜灯的当量:原版合成 4 铜块 → 4 个,即 1 铜块 1 个。 */
	private static final int BULB_VALUE = BLOCK_VALUE;
	/** 铜栏杆的当量:原版合成 6 铜锭 → 16 个。 */
	private static final int BARS_VALUE = INGOT_VALUE * 6 / 16;
	/** 铜链的当量:原版合成 1 铜锭 + 2 铜粒 → 1 个。 */
	private static final int CHAIN_VALUE = INGOT_VALUE + NUGGET_VALUE * 2;
	/** 铜箱子的当量:原版合成 8 铜锭 + 1 木箱 → 1 个(只计铜)。 */
	private static final int CHEST_VALUE = INGOT_VALUE * 8;
	/** 铜火把的当量:原版合成 1 铜粒(另需木棍 + 煤)→ 4 个(只计铜)。 */
	private static final int TORCH_VALUE = NUGGET_VALUE / 4;
	/** 铜灯笼的当量:原版合成 8 铜粒 + 1 铜火把 → 1 个(只计铜)。 */
	private static final int LANTERN_VALUE = NUGGET_VALUE * 8 + TORCH_VALUE;

	/**
	 * 14 种形态的当量,顺序与 {@link #shapeId} 的 {@code shape} 参数一致:
	 * 铜块 / 切制 / 凿制 / 格栅 / 台阶 / 楼梯 / 门 / 活板门 / 灯 / 栏杆 / 链 / 箱子 / 灯笼 / 火把。
	 */
	private static final int[] SHAPE_VALUES = {
			BLOCK_VALUE,
			CUT_VALUE, CUT_VALUE, CUT_VALUE,
			SLAB_VALUE,
			CUT_VALUE,
			DOOR_VALUE, TRAPDOOR_VALUE, BULB_VALUE,
			BARS_VALUE, CHAIN_VALUE, CHEST_VALUE, LANTERN_VALUE, TORCH_VALUE,
	};

	/** 铜火把的形态后缀:原版没给它做氧化 / 涂蜡变体,只在 {@code ""} 前缀下存在。 */
	private static final String TORCH_SUFFIX = "copper_torch";

	/**
	 * stonecutting 配方 {@code result.count} 的上限。
	 * <p>
	 * <b>26.x 把该字段限制在 [1, 99]</b>(1.21.x 没有这个限制)。一旦写出超限的配方,
	 * 整个 {@code minecraft:recipe} 注册表重载都会失败 —— 不只是这几条配方失效,
	 * 而是连原版配方一起从切石机里消失。所以超限的方向一律不生成,由玩家分两步切
	 * (例如 1 铜块 → 铜台阶 ×8,再 台阶 → 铜火把 ×40 = 320 个,接近原版的 324)。
	 */
	private static final int MAX_RESULT_COUNT = 99;

	/** 形态总数(铜块 + {@link #SHAPE_SUFFIXES});形态索引 0 = 铜块。 */
	private static final int SHAPE_COUNT = SHAPE_SUFFIXES.length + 1;

	private CopperStonecuttingDeployHook() {
	}

	public static void ensureDeployed(MinecraftServer server) {
		try {
			Path packDir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
			boolean changed = false;

			// mcmeta 内容不一致就重写(旧格式不再被新版本接受)。
			Files.createDirectories(packDir);
			Files.createDirectories(packDir.resolve("data/carpet-hao-addition/recipe"));
			changed |= writeIfChanged(packDir.resolve("pack.mcmeta"), mcmetaContent());
			Path dir = packDir.resolve("data/carpet-hao-addition/recipe");
			Files.createDirectories(dir);

			Map<String, String> recipes = new LinkedHashMap<>();

			// 1) 同一变体内:按当量从"高"切向"低"(产出数 = 源当量 / 目标当量,向下取整)。
			for (String prefix : PREFIXES) {
				for (int from = 0; from < SHAPE_COUNT; from++) {
					for (int to = 0; to < SHAPE_COUNT; to++) {
						if (from == to) {
							continue;
						}
						int fromValue = SHAPE_VALUES[from];
						int toValue = SHAPE_VALUES[to];
						// 只做"等值或降级"的方向:升级需要分数个产物,做不了也会被拿来套利。
						if (fromValue < toValue) {
							continue;
						}
						if (!hasVariant(prefix, from) || !hasVariant(prefix, to)) {
							continue; // 铜火把没有氧化 / 涂蜡变体
						}
						String fromId = shapeId(prefix, from);
						String toId = shapeId(prefix, to);
						if (vanillaAlreadyHas(prefix, fromId, toId)) {
							continue; // 原版切石机已有该方向(比例与本表一致),不重复生成
						}
						int count = hao$productCount(fromValue, toValue);
						if (count == 0) {
							continue; // 产出数超过 MAX_RESULT_COUNT:写了会让整个数据包重载失败
						}
						hao$addEntry(recipes,
								recipeJson("minecraft:" + fromId, "minecraft:" + toId, count));
					}
				}
			}

			// 2) 同一种形态在 8 个变体之间 1:1 互换(涂蜡 ↔ 未涂蜡、各氧化等级互切)。
			for (int shape = 0; shape < SHAPE_COUNT; shape++) {
				for (String fromPrefix : PREFIXES) {
					if (!hasVariant(fromPrefix, shape)) {
						continue;
					}
					for (String toPrefix : PREFIXES) {
						if (fromPrefix.equals(toPrefix) || !hasVariant(toPrefix, shape)) {
							continue;
						}
						String fromId = shapeId(fromPrefix, shape);
						String toId = shapeId(toPrefix, shape);
						hao$addEntry(recipes,
								recipeJson("minecraft:" + fromId, "minecraft:" + toId, 1));
					}
				}
			}

			// 3) 任意变体的铜块 → 任意【其它】变体的任意形态(一步到位,不用先转变体再转形态)。
			//    目标当量 ≤ 铜块当量(当量最大的是铜块 / 铜灯,1:1),所以产出数 ≥ 1,依旧只少不多。
			for (String fromPrefix : PREFIXES) {
				String fromId = blockId(fromPrefix);
				for (String toPrefix : PREFIXES) {
					if (toPrefix.equals(fromPrefix)) {
						continue; // 同变体的各形态已在第 1 组覆盖
					}
					for (int to = 1; to < SHAPE_COUNT; to++) {
						// 目标从 1 开始:目标是"铜块"的情形属于同形态跨变体,第 2 组已覆盖。
						if (!hasVariant(toPrefix, to)) {
							continue;
						}
						int count = hao$productCount(BLOCK_VALUE, SHAPE_VALUES[to]);
						if (count == 0) {
							continue; // 产出数超过 MAX_RESULT_COUNT
						}
						String toId = shapeId(toPrefix, to);
						hao$addEntry(recipes,
								recipeJson("minecraft:" + fromId, "minecraft:" + toId, count));
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
				HAO_LOGGER.info("Enabled datapack '{}' ({} copper stonecutting recipes).",
						PACK_NAME, recipes.size());
			} else if (!wantEnabled && enabled) {
				packManager.removePack(PACK_ID);
				changed = true;
				HAO_LOGGER.info("Disabled datapack '{}' (rule copperStonecuttingRecipes is off).", PACK_NAME);
			}

			if (changed) {
				server.reloadResources(packManager.getSelectedIds());
			}
		} catch (IOException e) {
			HAO_LOGGER.error("Failed to deploy copper-stonecutting datapack", e);
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
	 * 该形态在该前缀下是否真实存在。
	 * <p>
	 * 只有铜火把例外:它没有氧化与涂蜡变体,原版只注册了 {@code copper_torch} 一个 ID,
	 * 所以 {@code exposed_copper_torch} 这类 ID 不能生成(否则是无效配方)。
	 */
	private static boolean hasVariant(String prefix, int shape) {
		return shape == 0 || prefix.isEmpty() || !TORCH_SUFFIX.equals(SHAPE_SUFFIXES[shape - 1]);
	}

	/**
	 * 一条配方的产出数(源当量 / 目标当量,向下取整)。
	 * <p>
	 * 调用前必须保证 {@code fromValue >= toValue}。返回 {@code 0} 表示该方向不该生成:
	 * 产出数超过了 {@link #MAX_RESULT_COUNT},写进数据包会让 26.x 的配方注册表整体重载失败。
	 */
	private static int hao$productCount(int fromValue, int toValue) {
		int count = fromValue / toValue;
		return count > MAX_RESULT_COUNT ? 0 : count;
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
	private static void hao$addEntry(Map<String, String> recipes, String json) {
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

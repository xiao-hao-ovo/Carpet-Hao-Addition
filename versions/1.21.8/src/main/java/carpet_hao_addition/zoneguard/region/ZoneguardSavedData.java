package carpet_hao_addition.zoneguard.region;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * haoZoneguard 存档数据:侦测器禁用区域(序号 -&gt; 区域)与权限玩家(uuid -&gt; 名字)。
 * <p>
 * 按当前工程架构放在 versions 层:1.21.8 用 {@link PersistentStateType} + codec 持久化
 * (参考实现的 MC 26.2 SavedDataType 同款体系,此处以 1.21.8 yarn API 适配)。
 */
public class ZoneguardSavedData extends PersistentState {
	public static final Codec<ZoneguardSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, DetectorRegion.CODEC).optionalFieldOf("regions", Map.of()).forGetter(ZoneguardSavedData::regions),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("operators", Map.of()).forGetter(ZoneguardSavedData::operators)
	).apply(instance, ZoneguardSavedData::new));

	// yarn 1.21.8 的 PersistentStateType id 会直接作为数据文件名(<id>.dat)使用,
	// 因此不能包含 ':'(26.2 参考实现用命名空间写法,此处以 1.21.8 行为为准)。
	public static final PersistentStateType<ZoneguardSavedData> TYPE = new PersistentStateType<>(
			"zoneguard_detector_regions",
			ZoneguardSavedData::new,
			CODEC,
			DataFixTypes.LEVEL
	);

	private final Map<String, DetectorRegion> regions = new LinkedHashMap<>();
	private final Map<String, String> operators = new LinkedHashMap<>();
	private transient Map<RegistryKey<World>, Map<Long, List<DetectorRegion>>> regionIndex;
	private transient List<DetectorRegion> largeRegions;

	public ZoneguardSavedData() {
	}

	private ZoneguardSavedData(Map<String, DetectorRegion> regions, Map<String, String> operators) {
		this.regions.putAll(regions);
		this.operators.putAll(operators);
	}

	public static ZoneguardSavedData getData(MinecraftServer haoServer) {
		return haoServer.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
	}

	public Map<String, DetectorRegion> regions() {
		return this.regions;
	}

	public Map<String, String> operators() {
		return this.operators;
	}

	public DetectorRegion setRegion(int id, ServerWorld world, String name, BlockPos first, BlockPos second) {
		DetectorRegion newRegion = DetectorRegion.fromCorners(world.getRegistryKey(), name, first, second);
		this.regions.put(String.valueOf(id), newRegion);
		this.regionIndex = null;
		this.largeRegions = null;
		this.markDirty();
		return newRegion;
	}

	public Optional<DetectorRegion> removeRegion(int id) {
		DetectorRegion removed = this.regions.remove(String.valueOf(id));
		if (removed != null) {
			this.regionIndex = null;
			this.largeRegions = null;
			this.markDirty();
		}
		return Optional.ofNullable(removed);
	}

	public boolean isOperator(UUID uuid) {
		return uuid != null && this.operators.containsKey(uuid.toString());
	}

	public boolean addOperator(GameProfile profile) {
		UUID uuid = profile.getId();
		String name = profile.getName();
		// 名字可能是 null（离线查询、手工构造的 GameProfile）。存进去之后
		// removeOperatorByName 会对着它调 equalsIgnoreCase 直接 NPE，
		// 而且 Codec.STRING 的 map 不接受 null value，保存时会产出无效数据。
		if (uuid == null || name == null) {
			return false;
		}
		String previous = this.operators.put(uuid.toString(), name);
		if (previous == null || !previous.equals(name)) {
			this.markDirty();
		}
		return previous == null;
	}

	public Optional<String> removeOperator(GameProfile profile) {
		if (profile.getId() == null) {
			return Optional.empty();
		}
		String removed = this.operators.remove(profile.getId().toString());
		if (removed != null) {
			this.markDirty();
			return Optional.of(removed);
		}
		return Optional.empty();
	}

	public Optional<String> removeOperatorByName(String name) {
		if (name == null) {
			return Optional.empty();
		}
		// 用 name.equalsIgnoreCase(value) 而不是 value.equalsIgnoreCase(name)：
		// 老存档里可能存着 null 名字（见 addOperator 的注释），反过来调会 NPE。
		Optional<Map.Entry<String, String>> match = this.operators.entrySet().stream()
				.filter(entry -> name.equalsIgnoreCase(entry.getValue()))
				.findFirst();
		if (match.isEmpty()) {
			return Optional.empty();
		}

		this.operators.remove(match.get().getKey());
		this.markDirty();
		return Optional.of(match.get().getValue());
	}

	public boolean disablesObserver(World world, BlockPos pos) {
		ensureRegionIndex();
		RegistryKey<World> dimension = world.getRegistryKey();
		Map<Long, List<DetectorRegion>> byChunk = this.regionIndex.get(dimension);
		if (byChunk != null && containsAny(byChunk.get(chunkKey(pos.getX() >> 4, pos.getZ() >> 4)), dimension, pos)) {
			return true;
		}
		return containsAny(this.largeRegions, dimension, pos);
	}

	private boolean containsAny(List<DetectorRegion> candidates, RegistryKey<World> dimension, BlockPos pos) {
		if (candidates == null) {
			return false;
		}
		for (DetectorRegion region : candidates) {
			if (region.contains(dimension, pos)) {
				return true;
			}
		}
		return false;
	}

	private void ensureRegionIndex() {
		if (this.regionIndex != null) {
			return;
		}
		Map<RegistryKey<World>, Map<Long, List<DetectorRegion>>> index = new HashMap<>();
		List<DetectorRegion> oversized = new ArrayList<>();
		for (DetectorRegion region : this.regions.values()) {
			int minChunkX = region.min().getX() >> 4;
			int maxChunkX = region.max().getX() >> 4;
			int minChunkZ = region.min().getZ() >> 4;
			int maxChunkZ = region.max().getZ() >> 4;
			long chunkCount = ((long) maxChunkX - minChunkX + 1L) * ((long) maxChunkZ - minChunkZ + 1L);
			if (chunkCount > 4096L) {
				oversized.add(region);
				continue;
			}
			Map<Long, List<DetectorRegion>> byChunk = index.computeIfAbsent(region.dimension(), ignored -> new HashMap<>());
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
					byChunk.computeIfAbsent(chunkKey(chunkX, chunkZ), ignored -> new ArrayList<>()).add(region);
				}
			}
		}
		this.regionIndex = index;
		this.largeRegions = oversized;
	}

	private static long chunkKey(int chunkX, int chunkZ) {
		return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
	}
}

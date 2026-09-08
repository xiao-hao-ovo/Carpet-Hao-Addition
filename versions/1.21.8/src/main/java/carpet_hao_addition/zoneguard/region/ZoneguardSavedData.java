package carpet_hao_addition.zoneguard.region;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * zoneguard 存档数据:侦测器禁用区域(序号 -&gt; 区域)与权限玩家(uuid -&gt; 名字)。
 * <p>
 * 按当前工程架构放在 versions 层:1.21.8 用 {@link PersistentStateType} + codec 持久化
 * (参考实现的 MC 26.2 SavedDataType 同款体系,此处以 1.21.8 yarn API 适配)。
 */
public class ZoneguardSavedData extends PersistentState {
	public static final Codec<ZoneguardSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, DetectorRegion.CODEC).optionalFieldOf("regions", Map.of()).forGetter(ZoneguardSavedData::regions),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("operators", Map.of()).forGetter(ZoneguardSavedData::operators)
	).apply(instance, ZoneguardSavedData::new));

	public static final PersistentStateType<ZoneguardSavedData> TYPE = new PersistentStateType<>(
			"zoneguard:detector_regions",
			ZoneguardSavedData::new,
			CODEC,
			DataFixTypes.LEVEL
	);

	private final Map<String, DetectorRegion> regions = new LinkedHashMap<>();
	private final Map<String, String> operators = new LinkedHashMap<>();

	public ZoneguardSavedData() {
	}

	private ZoneguardSavedData(Map<String, DetectorRegion> regions, Map<String, String> operators) {
		this.regions.putAll(regions);
		this.operators.putAll(operators);
	}

	public static ZoneguardSavedData get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
	}

	public Map<String, DetectorRegion> regions() {
		return this.regions;
	}

	public Map<String, String> operators() {
		return this.operators;
	}

	public DetectorRegion setRegion(int id, ServerWorld world, BlockPos first, BlockPos second) {
		DetectorRegion newRegion = DetectorRegion.fromCorners(world.getRegistryKey(), first, second);
		this.regions.put(String.valueOf(id), newRegion);
		this.markDirty();
		return newRegion;
	}

	public Optional<DetectorRegion> removeRegion(int id) {
		DetectorRegion removed = this.regions.remove(String.valueOf(id));
		if (removed != null) {
			this.markDirty();
		}
		return Optional.ofNullable(removed);
	}

	public boolean isOperator(UUID uuid) {
		return uuid != null && this.operators.containsKey(uuid.toString());
	}

	public boolean addOperator(GameProfile profile) {
		UUID uuid = profile.getId();
		if (uuid == null) {
			return false;
		}
		String previous = this.operators.put(uuid.toString(), profile.getName());
		if (previous == null || !previous.equals(profile.getName())) {
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
		Optional<Map.Entry<String, String>> match = this.operators.entrySet().stream()
				.filter(entry -> entry.getValue().equalsIgnoreCase(name))
				.findFirst();
		if (match.isEmpty()) {
			return Optional.empty();
		}

		this.operators.remove(match.get().getKey());
		this.markDirty();
		return Optional.of(match.get().getValue());
	}

	public boolean disablesObserver(World world, BlockPos pos) {
		return this.regions.values().stream().anyMatch(region -> region.contains(world.getRegistryKey(), pos));
	}
}

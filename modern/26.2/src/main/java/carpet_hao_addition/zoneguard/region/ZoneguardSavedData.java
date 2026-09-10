package carpet_hao_addition.zoneguard.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/**
 * zoneguard 存档数据:侦测器禁用区域(序号 -&gt; 区域)与权限玩家(uuid -&gt; 名字)。
 * <p>
 * 按当前工程架构放在 versions 层:1.21.8 用 {@link SavedDataType} + codec 持久化
 * (参考实现的 MC 26.2 SavedDataType 同款体系,此处以 1.21.8 yarn API 适配)。
 */
public class ZoneguardSavedData extends SavedData {
	public static final Codec<ZoneguardSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, DetectorRegion.CODEC).optionalFieldOf("regions", Map.of()).forGetter(ZoneguardSavedData::regions),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("operators", Map.of()).forGetter(ZoneguardSavedData::operators)
	).apply(instance, ZoneguardSavedData::new));

	// 1.21.10 yarn 以 NameAndId 表示在线/配置玩家(替代 GameProfile 用于名单操作)。
	// SavedDataType id 直接作为文件名,不能含 ':'。
	public static final SavedDataType<ZoneguardSavedData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath("carpet_hao_addition", "zoneguard_detector_regions"),
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
		return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
	}

	public Map<String, DetectorRegion> regions() {
		return this.regions;
	}

	public Map<String, String> operators() {
		return this.operators;
	}

	public DetectorRegion setRegion(int id, ServerLevel world, BlockPos first, BlockPos second) {
		DetectorRegion newRegion = DetectorRegion.fromCorners(world.dimension(), first, second);
		this.regions.put(String.valueOf(id), newRegion);
		this.setDirty();
		return newRegion;
	}

	public Optional<DetectorRegion> removeRegion(int id) {
		DetectorRegion removed = this.regions.remove(String.valueOf(id));
		if (removed != null) {
			this.setDirty();
		}
		return Optional.ofNullable(removed);
	}

	public boolean isOperator(UUID uuid) {
		return uuid != null && this.operators.containsKey(uuid.toString());
	}

	public boolean addOperator(NameAndId entry) {
		UUID uuid = entry.id();
		if (uuid == null) {
			return false;
		}
		String previous = this.operators.put(uuid.toString(), entry.name());
		if (previous == null || !previous.equals(entry.name())) {
			this.setDirty();
		}
		return previous == null;
	}

	public Optional<String> removeOperator(NameAndId entry) {
		UUID uuid = entry.id();
		if (uuid == null) {
			return Optional.empty();
		}
		String removed = this.operators.remove(uuid.toString());
		if (removed != null) {
			this.setDirty();
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
		this.setDirty();
		return Optional.of(match.get().getValue());
	}

	public boolean disablesObserver(Level world, BlockPos pos) {
		return this.regions.values().stream().anyMatch(region -> region.contains(world.dimension(), pos));
	}
}

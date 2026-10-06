package carpet_hao_addition;

import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.ByteArrayData;
import fi.dy.masa.malilib.util.data.tag.ByteData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.DoubleData;
import fi.dy.masa.malilib.util.data.tag.FloatData;
import fi.dy.masa.malilib.util.data.tag.IntArrayData;
import fi.dy.masa.malilib.util.data.tag.IntData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.LongArrayData;
import fi.dy.masa.malilib.util.data.tag.LongData;
import fi.dy.masa.malilib.util.data.tag.ShortData;
import fi.dy.masa.malilib.util.data.tag.StringData;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;

/**
 * malilib 的 NBT 抽象({@code CompoundData} / {@code BaseData} 等)→ 原版 {@link NbtCompound} 的转换器。
 * <p>
 * <b>为什么需要它:</b>litematica 从 0.26 起改用 malilib 自研的 {@code fi.dy.masa.malilib.util.data.tag}
 * 结构保存投影实体的 NBT,不再直接给原版 {@code NbtCompound} ——
 * {@code EntityInfo.nbt()} 返回的是 {@code CompoundData}。两套结构之间**没有现成转换方法**
 * ({@code NbtIo} 在 1.21.11 也只有按文件/流压缩读的入口,没有从 {@code DataInput} 直接读的版本),
 * 所以只能按类型逐个搬。
 * <p>
 * 这个类只存在于 1.21.11 层:更早的版本层里 litematica 还是直接用原版 NBT,用不到它。
 */
public final class MalilibNbtConverter {
	private MalilibNbtConverter() {
	}

	/** 整体转换;{@code data} 为 null 时给出空 compound。 */
	public static NbtCompound toVanilla(CompoundData data) {
		NbtCompound out = new NbtCompound();
		if (data == null) {
			return out;
		}
		for (String key : data.getKeys()) {
			NbtElement value = toElement(data.getData(key).orElse(null));
			if (value != null) {
				out.put(key, value);
			}
		}
		return out;
	}

	/** 单个标签转换;遇到没覆盖的类型返回 null(调用方会跳过该键,而不是塞进错误的值)。 */
	private static NbtElement toElement(BaseData data) {
		if (data instanceof StringData value) {
			return NbtString.of(value.getString());
		}
		if (data instanceof IntData value) {
			return NbtInt.of(value.getInt());
		}
		if (data instanceof DoubleData value) {
			return NbtDouble.of(value.getDouble());
		}
		if (data instanceof FloatData value) {
			return NbtFloat.of(value.getFloat());
		}
		if (data instanceof ByteData value) {
			return NbtByte.of(value.getByte());
		}
		if (data instanceof ShortData value) {
			return NbtShort.of(value.getShort());
		}
		if (data instanceof LongData value) {
			return NbtLong.of(value.getLong());
		}
		if (data instanceof ByteArrayData value) {
			return new NbtByteArray(value.getByteArray());
		}
		if (data instanceof IntArrayData value) {
			return new NbtIntArray(value.getIntArray());
		}
		if (data instanceof LongArrayData value) {
			return new NbtLongArray(value.getLongArray());
		}
		if (data instanceof CompoundData value) {
			return toVanilla(value);
		}
		if (data instanceof ListData list) {
			NbtList out = new NbtList();
			for (int i = 0; i < list.size(); i++) {
				NbtElement element = toElement(list.get(i));
				if (element != null) {
					out.add(element);
				}
			}
			return out;
		}
		return null;
	}
}

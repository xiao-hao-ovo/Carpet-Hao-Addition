package carpet_hao_addition.easyplace;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.Direction;

import java.util.function.Function;

/**
 * 投影放置的编解码工具集 —— 供 {@link PlacementRules} 组装每个方块的协议编解码。
 *
 * <p>设计取舍：不为每个方块写一个类，而是把「状态类型」抽象成少量可复用的编解码器工厂。
 * 同类状态（水平朝向、开关、层数……）只写一遍，方块只负责说明「我由哪几种状态组成」。
 *
 * <p><b>协议位布局是与投影端约定的</b>（例如楼梯的 FACING 落在低 3 位、水平朝向按
 * N/E/S/W = 0..3），因此位段本身不能改动，只能改变代码的组织方式。
 */
public final class PlacementCodecs {

	private PlacementCodecs() {
	}

	/** 不编解码：该方块的放置完全交给投影原生处理。 */
	public static PlacementCodec none() {
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				return 0;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				return state;
			}
		};
	}

	/**
	 * 水平朝向（紧凑型）：低 2 位直接存 N/E/S/W = 0..3，不占「未编码」标记位。
	 * 用于营火、比较器、水平朝向方块这类「本协议通道只为朝向而开」的方块 ——
	 * 它们一定能拿到朝向，不需要区分「有没有编码」。
	 */
	public static PlacementCodec facing4() {
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				Property<Direction> prop = horizontalProperty(state);
				if (prop == null) {
					return 0;
				}
				return horizontalIndex(state.getValue(prop)) & 0b11;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				Property<Direction> prop = horizontalProperty(state);
				if (prop == null) {
					return state;
				}
				Direction facing = byHorizontalIndex(bits & 0b11);
				return prop.getPossibleValues().contains(facing) ? state.setValue(prop, facing) : state;
			}
		};
	}

	/**
	 * 水平朝向（带标记型）：低 3 位存 idx+1，0 表示「本次没有编码朝向」。
	 * 用于钟、墙面按钮、拉杆这类「朝向与其他属性共用位域」的方块。
	 */
	public static PlacementCodec facing4Marked() {
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				Property<Direction> prop = horizontalProperty(state);
				if (prop == null) {
					return 0;
				}
				return (horizontalIndex(state.getValue(prop)) + 1) & 0b111;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				int index = (bits & 0b111) - 1;
				Property<Direction> prop = horizontalProperty(state);
				if (prop == null || index < 0 || index > 3) {
					return state;
				}
				Direction facing = byHorizontalIndex(index);
				return prop.getPossibleValues().contains(facing) ? state.setValue(prop, facing) : state;
			}
		};
	}

	/**
	 * 六向朝向：按 {@link Direction#ordinal()} 顺序 +1 写低 3 位。
	 * 用于命令方块、漏斗、活塞这类可以朝上/下的方块。
	 */
	public static PlacementCodec allFacing() {
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				Property<Direction> prop = anyDirectionProperty(state);
				if (prop == null) {
					return 0;
				}
				return (state.getValue(prop).get3DDataValue() + 1) & 0b111;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				int index = (bits & 0b111) - 1;
				Property<Direction> prop = anyDirectionProperty(state);
				if (prop == null || index < 0 || index > 5) {
					return state;
				}
				Direction facing = Direction.from3DDataValue(index);
				return prop.getPossibleValues().contains(facing) ? state.setValue(prop, facing) : state;
			}
		};
	}

	/**
	 * 朝向按 {@link Direction#ordinal()} 原样存放 —— 既不 +1，也不折叠成水平四向。
	 * 这是在跟投影端对齐：告示牌那类墙挂方块约定的就是 NORTH=2 … WEST=5。
	 * （跟 {@link #facing4()} 的 0..3 不是一回事，别混用。）
	 */
	public static PlacementCodec facingByOrdinal(EnumProperty<Direction> property, int shift, int width) {
		int mask = (1 << width) - 1;
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				if (!state.hasProperty(property)) {
					return 0;
				}
				return (state.getValue(property).ordinal() & mask) << shift;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				if (!state.hasProperty(property)) {
					return state;
				}
				int index = (bits >>> shift) & mask;
				Direction[] directions = Direction.values();
				if (index >= directions.length) {
					return state;
				}
				Direction facing = directions[index];
				return property.getPossibleValues().contains(facing) ? state.setValue(property, facing) : state;
			}
		};
	}

	/** 枚举属性按 ordinal 编码到 [shift, shift+width) 位。 */
	public static <T extends Enum<T> & StringRepresentable> PlacementCodec enumProp(
			EnumProperty<T> property, int shift, int width, int offset) {
		int mask = (1 << width) - 1;
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				if (!state.hasProperty(property)) {
					return 0;
				}
				int ordinal = state.getValue(property).ordinal() - offset;
				return (bits & ~(mask << shift)) | ((ordinal & mask) << shift);
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				if (!state.hasProperty(property)) {
					return state;
				}
				int ordinal = ((bits >>> shift) & mask) + offset;
				return applyOrdinal(property, state, ordinal);
			}
		};
	}

	/** 整数属性，取低 {@code width} 位；{@code modulo > 0} 时解码结果再取模。 */
	public static PlacementCodec intProp(IntegerProperty property, int width, int modulo) {
		int mask = (1 << width) - 1;
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				return state.hasProperty(property) ? (state.getValue(property) & mask) : 0;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				if (!state.hasProperty(property)) {
					return state;
				}
				int value = bits & mask;
				if (modulo > 0) {
					value %= modulo;
				}
				int lo = property.getPossibleValues().get(0);
				int hi = property.getPossibleValues().get(property.getPossibleValues().size() - 1);
				return state.setValue(property, Math.max(lo, Math.min(value, hi)));
			}
		};
	}

	/** 布尔开关，编码到第 {@code shift} 位。 */
	public static PlacementCodec boolProp(BooleanProperty property, int shift) {
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				if (!state.hasProperty(property)) {
					return 0;
				}
				return state.getValue(property) ? (1 << shift) : 0;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				if (!state.hasProperty(property)) {
					return state;
				}
				return state.setValue(property, ((bits >>> shift) & 1) == 1);
			}
		};
	}

	/** 线性叠加多个编解码器：编码按顺序写入同一位域，解码按顺序应用。 */
	public static PlacementCodec stack(PlacementCodec... parts) {
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				int acc = bits;
				for (PlacementCodec part : parts) {
					acc |= part.encode(state, 0);
				}
				return acc;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				BlockState acc = state;
				for (PlacementCodec part : parts) {
					BlockState next = part.decode(bits, acc, context);
					if (next != null) {
						acc = next;
					}
				}
				return acc;
			}
		};
	}

	/**
	 * 堆叠类方块：一次只能加一层（雪层、海泡菜、海龟蛋、蜡烛、花床），
	 * 或者一次只能长一个方向（藤蔓、发光地衣）。协议值随「第几次点击」变化。
	 *
	 * @param decoder     解码：把协议还原成该次点击之后应有的状态
	 * @param counter     还差几次点击
	 * @param stepBits    第 N 次点击使用的协议值
	 */
	public static PlacementCodec stepped(Decoder decoder, StepCounter counter, StepBits stepBits) {
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				// 堆叠类不进「一次性编码」通道：协议值由 stepBits 逐次给出
				return 0;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				return decoder.apply(bits, state, context);
			}

			@Override
			public boolean multiStage() {
				return true;
			}

			@Override
			public int missingSteps(BlockState schematic, BlockState client) {
				return counter.count(schematic, client);
			}

			@Override
			public int stepBits(BlockState schematic, BlockState client, int index) {
				return stepBits.bits(schematic, client, index);
			}
		};
	}

	@FunctionalInterface
	public interface StepCounter {
		int count(BlockState schematic, BlockState client);
	}

	@FunctionalInterface
	public interface StepBits {
		int bits(BlockState schematic, BlockState client, int index);
	}

	/** 完全自定义（少数形态特殊的方块用）。 */
	public static PlacementCodec custom(Function<BlockState, Integer> encoder, Decoder decoder) {
		return new PlacementCodec() {
			@Override
			public int encode(BlockState state, int bits) {
				Integer v = encoder.apply(state);
				return v == null ? 0 : v;
			}

			@Override
			public BlockState decode(int bits, BlockState state, BlockPlaceContext context) {
				return decoder.apply(bits, state, context);
			}
		};
	}

	@FunctionalInterface
	public interface Decoder {
		BlockState apply(int bits, BlockState state, BlockPlaceContext context);
	}

	// ==================== 内部工具 ====================

	/** 水平索引：N/E/S/W = 0..3（Direction 的 ordinal 依次为 DOWN,UP,NORTH,EAST,SOUTH,WEST）。 */
	static int horizontalIndex(Direction direction) {
		return direction.ordinal() - 2;
	}

	static Direction byHorizontalIndex(int index) {
		return ordinals()[index + 2];
	}

	private static Direction[] ordinals() {
		return Direction.values();
	}

	private static Property<Direction> horizontalProperty(BlockState state) {
		Property<Direction> prop = anyDirectionProperty(state);
		if (prop == null) {
			return null;
		}
		for (Direction d : prop.getPossibleValues()) {
			if (!d.getAxis().isHorizontal()) {
				return null;
			}
		}
		return prop;
	}

	@SuppressWarnings("unchecked")
	private static Property<Direction> anyDirectionProperty(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (Direction.class.isAssignableFrom(property.getValueClass())) {
				return (Property<Direction>) property;
			}
		}
		return null;
	}

	private static <T extends Enum<T> & StringRepresentable> BlockState applyOrdinal(
			EnumProperty<T> property, BlockState state, int ordinal) {
		var values = property.getPossibleValues();
		if (values.isEmpty()) {
			return state;
		}
		return state.setValue(property, values.get(Math.floorMod(ordinal, values.size())));
	}
}

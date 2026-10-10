package carpet_hao_addition.easyplace;

import carpet_hao_addition.ProjectionPlacementSettings;

import net.minecraft.block.BannerBlock;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.SeaPickleBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BulbBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.DetectorRailBlock;
import net.minecraft.block.FlowerbedBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.Orientation;
import net.minecraft.block.enums.StairShape;
import net.minecraft.block.enums.WallShape;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import static carpet_hao_addition.easyplace.PlacementCodecs.allFacing;
import static carpet_hao_addition.easyplace.PlacementCodecs.boolProp;
import static carpet_hao_addition.easyplace.PlacementCodecs.custom;
import static carpet_hao_addition.easyplace.PlacementCodecs.enumProp;
import static carpet_hao_addition.easyplace.PlacementCodecs.facingByOrdinal;
import static carpet_hao_addition.easyplace.PlacementCodecs.facing4;
import static carpet_hao_addition.easyplace.PlacementCodecs.facing4Marked;
import static carpet_hao_addition.easyplace.PlacementCodecs.intProp;
import static carpet_hao_addition.easyplace.PlacementCodecs.stackOf;
import static carpet_hao_addition.easyplace.PlacementCodecs.stepped;

/**
 * 「方块 -> 协议编解码」规则表。
 *
 * <p>每种方块只需说明它由哪几种状态构成，具体编解码交给 {@link PlacementCodecs}。
 * 若方块未登记，则认为它的放置由投影原生流程处理，本模组不插手
 * （这也是「不接管投影原有行为」的默认姿态）。
 *
 * <p><b>位的含义由投影端协议决定，不能改</b>：例如台阶 FACING 只占低 2 位、
 * 满档红石线的 SHAPE 占低 4 位。这里登记的值必须与之一致，否则投影会放错。
 *
 * <p>表分两张，因为协议里跑着两条互不重叠的位通道：
 * <ul>
 *   <li>{@code RULES} —— 方块状态{@link PlacementCodec}，绝大多数方块走这条；</li>
 *   <li>{@code ITEM_DATA} —— 物品数据{@link ItemDataCodec}，只服务告示牌 / 合成器 /
 *       信标这几种「要还的东西不在方块状态里」的方块。</li>
 * </ul>
 */
public final class PlacementRules {

	private static final Map<Class<? extends Block>, PlacementCodec> STATE_CODECS = new HashMap<>();

	/** 「方块 -> 物品数据编解码」规则表。 */
	private static final Map<Class<? extends Block>, ItemDataCodec> ITEM_DATA = new HashMap<>();

	/** 红石线的四个连接属性。 */
	private static final EnumProperty<WireConnection>[] WIRE_CONNECTIONS = new EnumProperty[]{
			Properties.NORTH_WIRE_CONNECTION, Properties.EAST_WIRE_CONNECTION,
			Properties.SOUTH_WIRE_CONNECTION, Properties.WEST_WIRE_CONNECTION};

	/** 藤蔓的方向表：去掉 DOWN。索引与原实现一致。 */
	private static final Direction[] VINE_DIRECTIONS =
			java.util.Arrays.stream(Direction.values()).filter(d -> d != Direction.DOWN).toArray(Direction[]::new);


	private PlacementRules() {
	}

	static {
		// ---------- 旋转类（16 向，值 +1 存放；0 表示「没编码」） ----------

		// 旗帜：站立变体有 ROTATION，墙挂变体没有，编码器内部会跳过
		stateCodec(BannerBlock.class, custom(
				state -> state.contains(Properties.ROTATION)
						? ((state.get(Properties.ROTATION) + 1) & 0b1111)
						: 0,
				(bits, state, ctx) -> {
					int rotation = (bits & 0b1111) - 1;
					return state.contains(Properties.ROTATION)
							? state.with(Properties.ROTATION, Math.max(0, rotation))
							: state;
				}));

		// 头颅 / 玩家头颅：同上，墙挂变体绕过
		stateCodec(SkullBlock.class, custom(
				state -> state.contains(Properties.ROTATION)
						? ((state.get(Properties.ROTATION) + 1) & 0b1111)
						: 0,
				(bits, state, ctx) -> {
					int rotation = (bits & 0b1111) - 1;
					return state.contains(Properties.ROTATION)
							? state.with(Properties.ROTATION, Math.max(0, rotation))
							: state;
				}));

		// ---------- 告示牌系列 ----------

		// 站立变体：旋转角直存低 4 位
		stateCodec(SignBlock.class, intProp(SignBlock.ROTATION, 4, 0));
		// 墙挂变体：朝向按 ordinal 直存（投影端约定 NORTH=2 … WEST=5）
		stateCodec(WallSignBlock.class, facingByOrdinal(Properties.HORIZONTAL_FACING, 0, 3));
		// 悬挂告示牌的站立变体多一个「已挂到方块上」标记（bit4）
		stateCodec(HangingSignBlock.class, stackOf(
				intProp(HangingSignBlock.ROTATION, 4, 0),
				boolProp(HangingSignBlock.ATTACHED, 4)));
		stateCodec(WallHangingSignBlock.class, facingByOrdinal(Properties.HORIZONTAL_FACING, 0, 3));

		// 这四种方块除状态外还带着一块手持物数据：文字颜色 / 发光 / 打蜡。
		// 两家告示牌的方块实体 id 不一样，所以各配一个实例。
		SignItemData signData = new SignItemData("minecraft:sign");
		SignItemData hangingSignData = new SignItemData("minecraft:hanging_sign");
		itemData(SignBlock.class, signData);
		itemData(WallSignBlock.class, signData);
		itemData(HangingSignBlock.class, hangingSignData);
		itemData(WallHangingSignBlock.class, hangingSignData);
		// 合成器：9 个槽的禁用位图；信标：主 / 副效果
		itemData(CrafterBlock.class, new CrafterItemData());
		itemData(BeaconBlock.class, new BeaconItemData());

		// ---------- 朝向类 ----------

		// 营火：水平朝向占低 2 位 + 点燃标记占 bit4。
		// 注意此处不登记 HorizontalFacingBlock —— 活板门/门/栅栏门都继承它，
		// 按基类接管会覆盖投影原生对这些方块的轻松放置（曾导致活板门 half/open 丢失）。
		stateCodec(CampfireBlock.class, stackOf(
				facing4(),
				boolProp(CampfireBlock.LIT, 4)));

		// 比较器：水平朝向占低 2 位 + 减法模式占 bit2
		stateCodec(ComparatorBlock.class, stackOf(
				facing4(),
				enumProp(ComparatorBlock.MODE, 2, 1, 0)));

		// 命令方块：六向朝向（低 3 位，值 +1）+ 条件模式（bit4）
		stateCodec(CommandBlock.class, stackOf(
				allFacing(),
				boolProp(Properties.CONDITIONAL, 4)));

		// 漏斗：六向朝向（低 3 位）+ 禁用标记（bit3：禁用才置位）
		stateCodec(HopperBlock.class, stackOf(
				allFacing(),
				custom(state -> state.contains(HopperBlock.ENABLED)
								&& !state.get(HopperBlock.ENABLED) ? 0b1000 : 0,
						(bits, state, ctx) -> state.contains(HopperBlock.ENABLED)
								? state.with(HopperBlock.ENABLED, (bits & 0b1000) == 0)
								: state)));

		// 拼图方块 / 合成器：朝向用 ORIENTATION 的 ordinal（低 4 位，值 +1，共 12 种）
		stateCodec(JigsawBlock.class, enumProp(JigsawBlock.ORIENTATION, 0, 4, -1));
		stateCodec(CrafterBlock.class, enumProp(Properties.ORIENTATION, 0, 4, -1));

		// 钟：水平朝向 + 挂载方式（bit4-5）
		stateCodec(BellBlock.class, stackOf(
				facing4Marked(),
				enumProp(BellBlock.ATTACHMENT, 4, 2, 0)));

		// 拉杆 / 墙面按钮：水平朝向 + 贴面（bit4-5）[+ 拉杆的开关状态 bit6]
		stateCodec(WallMountedBlock.class, stackOf(
				facing4Marked(),
				enumProp(WallMountedBlock.FACE, 4, 2, 0)));
		stateCodec(LeverBlock.class, stackOf(
				facing4Marked(),
				enumProp(WallMountedBlock.FACE, 4, 2, 0),
				boolProp(LeverBlock.POWERED, 6)));

		// ---------- 台阶与楼梯 ----------

		// 楼梯：朝向低 2 位、上下半 bit2、形状 bit3-5（与投影约定一致，不可改）
		stateCodec(StairsBlock.class, stackOf(
				custom(state -> state.contains(Properties.HORIZONTAL_FACING)
								? (state.get(Properties.HORIZONTAL_FACING).ordinal() - 2) & 0b11
								: 0,
						(bits, state, ctx) -> {
							if (!state.contains(Properties.HORIZONTAL_FACING)) {
								return state;
							}
							Direction facing = Direction.values()[((bits & 0b11) + 2) % 6];
							return state.with(Properties.HORIZONTAL_FACING, facing);
						}),
				enumProp(StairsBlock.HALF, 2, 1, 0),
				enumProp(StairsBlock.SHAPE, 3, 3, 0)));

		// ---------- 档位 / 数值类 ----------

		// 音符盒：音高 0-24，占低 5 位
		stateCodec(NoteBlock.class, intProp(NoteBlock.NOTE, 5, 25));

		// 光照方块：亮度 0-15，占低 4 位
		stateCodec(LightBlock.class, intProp(Properties.LEVEL_15, 4, 0));

		// 结构方块：模式（保存/加载/角落/数据），占低 2 位
		stateCodec(StructureBlock.class, enumProp(StructureBlock.MODE, 0, 2, 0));

		// 可堆肥：层数 0-8，占低 4 位；仅当规则取 with_composter_level 时才编解码，
		// 否则整项返回 0（等于「按原版放置」）
		stateCodec(ComposterBlock.class, custom(
				state -> ProjectionPlacementSettings.composterLevelEnabled()
						&& state.contains(ComposterBlock.LEVEL)
						? (state.get(ComposterBlock.LEVEL) & 0b1111)
						: 0,
				(bits, state, ctx) -> {
					if (!ProjectionPlacementSettings.composterLevelEnabled()
							|| !state.contains(ComposterBlock.LEVEL)) {
						return state;
					}
					int level = Math.min(bits & 0b1111, ComposterBlock.MAX_LEVEL);
					return state.with(ComposterBlock.LEVEL, level);
				}));

		// 中继器：朝向低 2 位（值 -2）、延迟 bit2-3（值 -1）、锁存 bit4、通电 bit5
		stateCodec(RepeaterBlock.class, custom(
				state -> {
					int bits = 0;
					if (state.contains(Properties.HORIZONTAL_FACING)) {
						bits |= (state.get(Properties.HORIZONTAL_FACING).ordinal() - 2) & 0b11;
					}
					if (state.contains(RepeaterBlock.DELAY)) {
						bits |= ((state.get(RepeaterBlock.DELAY) - 1) & 0b11) << 2;
					}
					if (state.contains(RepeaterBlock.LOCKED) && state.get(RepeaterBlock.LOCKED)) {
						bits |= 0b0001_0000;
					}
					if (state.contains(Properties.POWERED) && state.get(Properties.POWERED)) {
						bits |= 0b0010_0000;
					}
					return bits;
				},
				(bits, state, ctx) -> {
					BlockState out = state;
					if (state.contains(Properties.HORIZONTAL_FACING)) {
						Direction facing = Direction.values()[((bits & 0b11) + 2) % 6];
						out = out.with(Properties.HORIZONTAL_FACING, facing);
					}
					if (state.contains(RepeaterBlock.DELAY)) {
						out = out.with(RepeaterBlock.DELAY, ((bits >>> 2) & 0b11) + 1);
					}
					boolean locked = (bits & 0b0001_0000) != 0;
					if (state.contains(RepeaterBlock.LOCKED)) {
						out = out.with(RepeaterBlock.LOCKED, locked);
					}
					if (locked && state.contains(Properties.POWERED)) {
						out = out.with(Properties.POWERED, (bits & 0b0010_0000) != 0);
					}
					return out;
				}));

		// ---------- 开关类 ----------

		// 铜灯：点亮 bit0、通电 bit1（与投影端一致，不可对调）
		stateCodec(BulbBlock.class, stackOf(
				boolProp(BulbBlock.LIT, 0),
				boolProp(BulbBlock.POWERED, 1)));

		// 红石灯：点亮 bit4
		stateCodec(RedstoneLampBlock.class, boolProp(RedstoneLampBlock.LIT, 4));

		// 阳光探测器：反向 bit0
		stateCodec(DaylightDetectorBlock.class, boolProp(DaylightDetectorBlock.INVERTED, 0));

		// 探测铁轨：通电 bit0
		stateCodec(DetectorRailBlock.class, boolProp(DetectorRailBlock.POWERED, 0));

		// 灯笼：悬挂 bit0
		stateCodec(LanternBlock.class, boolProp(LanternBlock.HANGING, 0));

		// ---------- 位域类 ----------

		// 巨型蘑菇：N/E/S/W/UP/DOWN 依次占 bit0-5（与投影端一致，不可改序）
		stateCodec(MushroomBlock.class, stackOf(
				boolProp(MushroomBlock.NORTH, 0),
				boolProp(MushroomBlock.EAST, 1),
				boolProp(MushroomBlock.SOUTH, 2),
				boolProp(MushroomBlock.WEST, 3),
				boolProp(MushroomBlock.UP, 4),
				boolProp(MushroomBlock.DOWN, 5)));

		// 墙：四向 WallShape 各占 2 位、柱高 bit8
		stateCodec(WallBlock.class, stackOf(
				enumProp(WallBlock.NORTH_SHAPE, 0, 2, 0),
				enumProp(WallBlock.EAST_SHAPE, 2, 2, 0),
				enumProp(WallBlock.SOUTH_SHAPE, 4, 2, 0),
				enumProp(WallBlock.WEST_SHAPE, 6, 2, 0),
				boolProp(WallBlock.UP, 8)));

		// 红石线：只有「孤立点」这一种特殊形态需要还原（bit0）。
		// 注意连接属性是 EnumProperty<WireConnection>（NONE / SIDE / UP），不是布尔量。
		stateCodec(RedstoneWireBlock.class, custom(
				state -> isIsolatedDot(state) ? 0b0001 : 0,
				(bits, state, ctx) -> {
					if ((bits & 0b0001) == 0) {
						return state;
					}
					BlockState out = state;
					for (var prop : WIRE_CONNECTIONS) {
						if (out.contains(prop)) {
							out = out.with(prop, WireConnection.NONE);
						}
					}
					return out;
				}));

		// 铁轨：形状低 4 位；bit4 标记「不要自动改形状」
		stateCodec(RailBlock.class, stackOf(
				enumProp(RailBlock.SHAPE, 0, 4, 0),
				custom(state -> 0b0001_0000,
						(bits, state, ctx) -> {
							if ((bits & 0b0001_0000) != 0) {
								ProjectionPlacement.noteFlag(
										ProjectionPlacement.RAIL_KEEP_SHAPE);
							}
							return state;
						})));

		// ---------- 堆叠类（需要连续点几次才叠到目标） ----------

		// 雪层：层数 1-8 占低 3 位；雪是「一层层加」的
		stateCodec(SnowBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.contains(SnowBlock.LAYERS)) {
						return state;
					}
					// 一次只叠一层(原版行为)，这里只挡「已经比目标还多」——
					// 差的次数交给外层循环反复点。
					int target = (bits & 0b0111) + 1;
					return state.get(SnowBlock.LAYERS) > target ? null : state;
				},
				(schem, client) -> missing(schem, client, SnowBlock.class, SnowBlock.LAYERS),
				(schem, client, index) -> (schem.get(SnowBlock.LAYERS) - 1) & 0b0111));

		// 海泡菜：1-4 个占低 2 位
		stateCodec(SeaPickleBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.contains(SeaPickleBlock.PICKLES)) {
						return state;
					}
					int target = (bits & 0b0011) + 1;
					return state.get(SeaPickleBlock.PICKLES) > target ? null : state;
				},
				(schem, client) -> missing(schem, client, SeaPickleBlock.class, SeaPickleBlock.PICKLES),
				(schem, client, index) -> (schem.get(SeaPickleBlock.PICKLES) - 1) & 0b0011));

		// 海龟蛋：1-4 个占低 2 位
		stateCodec(TurtleEggBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.contains(TurtleEggBlock.EGGS)) {
						return state;
					}
					int target = (bits & 0b0011) + 1;
					return state.get(TurtleEggBlock.EGGS) > target ? null : state;
				},
				(schem, client) -> missing(schem, client, TurtleEggBlock.class, TurtleEggBlock.EGGS),
				(schem, client, index) -> (schem.get(TurtleEggBlock.EGGS) - 1) & 0b0011));

		// 蜡烛：个数 1-4 占低 2 位，点亮标记占 bit2（注意不是 bit0）
		stateCodec(CandleBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.contains(CandleBlock.CANDLES)) {
						return state;
					}
					int candles = (bits & 0b0011) + 1;
					if (state.get(CandleBlock.CANDLES) > candles) {
						return null;
					}
					BlockState out = state.with(CandleBlock.CANDLES, candles);
					if (state.contains(CandleBlock.LIT) && (bits & 0b0100) != 0) {
						out = out.with(CandleBlock.LIT, true);
					}
					return out;
				},
				(schem, client) -> missing(schem, client, CandleBlock.class, CandleBlock.CANDLES),
				(schem, client, index) -> ((schem.get(CandleBlock.CANDLES) - 1) & 0b0011)
						| (schem.get(CandleBlock.LIT) ? 0b0100 : 0)));

		// 花床：朝向占 bit2-3，数量 1-4 占低 2 位（位序与原实现一致，不可对调）
		stateCodec(FlowerbedBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.contains(FlowerbedBlock.FLOWER_AMOUNT)) {
						return state;
					}
					int maxAmount = (bits & 0b0011) + 1;
					BlockState out = state;
					BlockState at = ctx.getWorld().getBlockState(
							PlacementBitTools.clickedPos(ctx));
					if (!(at.getBlock() instanceof FlowerbedBlock)) {
						int facingOrdinal = ((bits & 0b1100) >>> 2) + 2;
						if (state.contains(FlowerbedBlock.FACING)) {
							out = out.with(FlowerbedBlock.FACING, Direction.values()[facingOrdinal]);
						}
					}
					return out.get(FlowerbedBlock.FLOWER_AMOUNT) > maxAmount ? null : out;
				},
				(schem, client) -> missing(schem, client, FlowerbedBlock.class, FlowerbedBlock.FLOWER_AMOUNT),
				(schem, client, index) -> (((schem.get(FlowerbedBlock.FACING).ordinal() - 2) & 0b11) << 2)
						| ((schem.get(FlowerbedBlock.FLOWER_AMOUNT) - 1) & 0b11)));

		// 藤蔓：方向表是「去掉 DOWN」的 5 个（UP/N/E/S/W，索引 0..4），一次长一个方向
		stateCodec(VineBlock.class, stepped(
				(bits, state, ctx) -> {
					int index = bits & 0b0111;
					if (index >= VINE_DIRECTIONS.length) {
						return null;
					}
					Direction direction = VINE_DIRECTIONS[index];
					BlockState at = ctx.getWorld().getBlockState(
							PlacementBitTools.clickedPos(ctx));
					boolean isVine = state.getBlock() instanceof VineBlock;
					BooleanProperty prop = VineBlock.FACING_PROPERTIES.get(direction);
					if (prop == null) {
						return null;
					}
					if (isVine && state.contains(prop) && state.get(prop)) {
						return null;
					}
					BlockState base = isVine ? state : net.minecraft.block.Blocks.VINE.getDefaultState();
					return base.contains(prop) ? base.with(prop, true) : null;
				},
				(schem, client) -> missingDirections(schem, client, VINE_DIRECTIONS, PlacementRules::vineHas).size(),
				(schem, client, index) -> {
					List<Integer> need = missingDirections(schem, client, VINE_DIRECTIONS, PlacementRules::vineHas);
					return index < need.size() ? need.get(index) & 0b0111 : 0b0111;
				}));

		// 信标本身没有可还原的方块状态（要还原的是手持物上的效果数据）
		stateCodec(BeaconBlock.class, PlacementCodecs.none());
	}

	// ==================== 查询 ====================

	/**
	 * 取某个方块的编解码器：沿类继承链向上找，找不到返回 {@code null}
	 * （表示该方块的放置由投影原生流程处理）。
	 */
	public static PlacementCodec codec(Block block) {
		for (Class<?> type = block.getClass(); type != null; type = type.getSuperclass()) {
			PlacementCodec found = STATE_CODECS.get(type);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** 本表是否登记过该方块（含继承链）。 */
	public static boolean covered(Block block) {
		return codec(block) != null;
	}

	/**
	 * 取某个方块的物品数据编解码器；没登记返回 {@code null}
	 * （表示该方块的物品数据不由本模组还原）。
	 */
	public static ItemDataCodec itemData(Block block) {
		for (Class<?> type = block.getClass(); type != null; type = type.getSuperclass()) {
			ItemDataCodec found = ITEM_DATA.get(type);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static void stateCodec(Class<? extends Block> type, PlacementCodec codec) {
		STATE_CODECS.put(type, codec);
	}

	private static void itemData(Class<? extends Block> type, ItemDataCodec codec) {
		ITEM_DATA.put(type, codec);
	}

	/** 距离目标还差几次点击（当前层数 → 目标层数）。 */
	private static <B extends Block> int missing(BlockState schematic, BlockState client,
	                                             Class<B> blockType, net.minecraft.state.property.IntProperty property) {
		int current = blockType.isInstance(client.getBlock()) && client.contains(property) ? client.get(property) : 0;
		int target = schematic.contains(property) ? schematic.get(property) : 0;
		return Math.max(0, target - current);
	}

	/** 列出「投影有、客户端还没有」的方向索引。 */
	private static List<Integer> missingDirections(BlockState schematic, BlockState client,
	                                               Direction[] directions, BiPredicate<BlockState, Direction> has) {
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < directions.length; i++) {
			if (has.test(schematic, directions[i]) && !has.test(client, directions[i])) {
				out.add(i);
			}
		}
		return out;
	}

	private static boolean vineHas(BlockState state, Direction direction) {
		if (!(state.getBlock() instanceof VineBlock)) {
			return false;
		}
		BooleanProperty prop = VineBlock.FACING_PROPERTIES.get(direction);
		return prop != null && state.contains(prop) && state.get(prop);
	}

	/** 红石线是否为「不连任何方向」的孤立点。 */
	private static boolean isIsolatedDot(BlockState state) {
		if (!state.contains(Properties.NORTH_WIRE_CONNECTION)) {
			return false;
		}
		for (var prop : WIRE_CONNECTIONS) {
			if (state.contains(prop) && state.get(prop) != WireConnection.NONE) {
				return false;
			}
		}
		return true;
	}
}

package carpet_hao_addition.easyplace;

import carpet_hao_addition.BetterEasyPlaceProtocolSettings;

import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;

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
import static carpet_hao_addition.easyplace.PlacementCodecs.stack;
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

	private static final Map<Class<? extends Block>, PlacementCodec> RULES = new HashMap<>();

	/** 「方块 -> 物品数据编解码」规则表。 */
	private static final Map<Class<? extends Block>, ItemDataCodec> ITEM_DATA = new HashMap<>();

	/** 红石线的四个连接属性。 */
	private static final EnumProperty<RedstoneSide>[] WIRE_CONNECTIONS = new EnumProperty[]{
			RedstoneWireBlock.NORTH, RedstoneWireBlock.EAST,
			RedstoneWireBlock.SOUTH, RedstoneWireBlock.WEST};

	/** 藤蔓的方向表：去掉 DOWN。索引与原实现一致。 */
	private static final Direction[] VINE_DIRECTIONS =
			java.util.Arrays.stream(Direction.values()).filter(d -> d != Direction.DOWN).toArray(Direction[]::new);


	private PlacementRules() {
	}

	static {
		// ---------- 旋转类（16 向，值 +1 存放；0 表示「没编码」） ----------

		// 旗帜：站立变体有 ROTATION，墙挂变体没有，编码器内部会跳过
		rule(BannerBlock.class, custom(
				state -> state.hasProperty(BannerBlock.ROTATION)
						? ((state.getValue(BannerBlock.ROTATION) + 1) & 0b1111)
						: 0,
				(bits, state, ctx) -> {
					int rotation = (bits & 0b1111) - 1;
					return state.hasProperty(BannerBlock.ROTATION)
							? state.setValue(BannerBlock.ROTATION, Math.max(0, rotation))
							: state;
				}));

		// 头颅 / 玩家头颅：同上，墙挂变体绕过
		rule(SkullBlock.class, custom(
				state -> state.hasProperty(SkullBlock.ROTATION)
						? ((state.getValue(SkullBlock.ROTATION) + 1) & 0b1111)
						: 0,
				(bits, state, ctx) -> {
					int rotation = (bits & 0b1111) - 1;
					return state.hasProperty(SkullBlock.ROTATION)
							? state.setValue(SkullBlock.ROTATION, Math.max(0, rotation))
							: state;
				}));

		// ---------- 告示牌系列 ----------

		// 站立变体：旋转角直存低 4 位
		rule(SignBlock.class, intProp(StandingSignBlock.ROTATION, 4, 0));
		// 墙挂变体：朝向按 ordinal 直存（投影端约定 NORTH=2 … WEST=5）
		rule(WallSignBlock.class, facingByOrdinal(BlockStateProperties.HORIZONTAL_FACING, 0, 3));
		// 悬挂告示牌的站立变体多一个「已挂到方块上」标记（bit4）
		rule(CeilingHangingSignBlock.class, stack(
				intProp(CeilingHangingSignBlock.ROTATION, 4, 0),
				boolProp(CeilingHangingSignBlock.ATTACHED, 4)));
		rule(WallHangingSignBlock.class, facingByOrdinal(BlockStateProperties.HORIZONTAL_FACING, 0, 3));

		// 这四种方块除状态外还带着一块手持物数据：文字颜色 / 发光 / 打蜡。
		// 两家告示牌的方块实体 id 不一样，所以各配一个实例。
		SignItemData signData = new SignItemData("minecraft:sign");
		SignItemData hangingSignData = new SignItemData("minecraft:hanging_sign");
		itemData(SignBlock.class, signData);
		itemData(WallSignBlock.class, signData);
		itemData(CeilingHangingSignBlock.class, hangingSignData);
		itemData(WallHangingSignBlock.class, hangingSignData);
		// 合成器：9 个槽的禁用位图；信标：主 / 副效果
		itemData(CrafterBlock.class, new CrafterItemData());
		itemData(BeaconBlock.class, new BeaconItemData());

		// ---------- 朝向类 ----------

		// 营火：水平朝向占低 2 位 + 点燃标记占 bit4。
		// 注意此处不登记 HorizontalFacingBlock —— 活板门/门/栅栏门都继承它，
		// 按基类接管会覆盖投影原生对这些方块的轻松放置（曾导致活板门 half/open 丢失）。
		rule(CampfireBlock.class, stack(
				facing4(),
				boolProp(CampfireBlock.LIT, 4)));

		// 比较器：水平朝向占低 2 位 + 减法模式占 bit2
		rule(ComparatorBlock.class, stack(
				facing4(),
				enumProp(ComparatorBlock.MODE, 2, 1, 0)));

		// 命令方块：六向朝向（低 3 位，值 +1）+ 条件模式（bit4）
		rule(CommandBlock.class, stack(
				allFacing(),
				boolProp(BlockStateProperties.CONDITIONAL, 4)));

		// 漏斗：六向朝向（低 3 位）+ 禁用标记（bit3：禁用才置位）
		rule(HopperBlock.class, stack(
				allFacing(),
				custom(state -> state.hasProperty(HopperBlock.ENABLED)
								&& !state.getValue(HopperBlock.ENABLED) ? 0b1000 : 0,
						(bits, state, ctx) -> state.hasProperty(HopperBlock.ENABLED)
								? state.setValue(HopperBlock.ENABLED, (bits & 0b1000) == 0)
								: state)));

		// 拼图方块 / 合成器：朝向用 ORIENTATION 的 ordinal（低 4 位，值 +1，共 12 种）
		rule(JigsawBlock.class, enumProp(JigsawBlock.ORIENTATION, 0, 4, -1));
		rule(CrafterBlock.class, enumProp(BlockStateProperties.ORIENTATION, 0, 4, -1));

		// 钟：水平朝向 + 挂载方式（bit4-5）
		rule(BellBlock.class, stack(
				facing4Marked(),
				enumProp(BellBlock.ATTACHMENT, 4, 2, 0)));

		// 拉杆 / 墙面按钮：水平朝向 + 贴面（bit4-5）[+ 拉杆的开关状态 bit6]
		rule(FaceAttachedHorizontalDirectionalBlock.class, stack(
				facing4Marked(),
				enumProp(FaceAttachedHorizontalDirectionalBlock.FACE, 4, 2, 0)));
		rule(LeverBlock.class, stack(
				facing4Marked(),
				enumProp(FaceAttachedHorizontalDirectionalBlock.FACE, 4, 2, 0),
				boolProp(LeverBlock.POWERED, 6)));

		// ---------- 台阶与楼梯 ----------

		// 楼梯：朝向低 2 位、上下半 bit2、形状 bit3-5（与投影约定一致，不可改）
		rule(StairBlock.class, stack(
				custom(state -> state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
								? (state.getValue(BlockStateProperties.HORIZONTAL_FACING).ordinal() - 2) & 0b11
								: 0,
						(bits, state, ctx) -> {
							if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
								return state;
							}
							Direction facing = Direction.values()[((bits & 0b11) + 2) % 6];
							return state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
						}),
				enumProp(StairBlock.HALF, 2, 1, 0),
				enumProp(StairBlock.SHAPE, 3, 3, 0)));

		// ---------- 档位 / 数值类 ----------

		// 音符盒：音高 0-24，占低 5 位
		rule(NoteBlock.class, intProp(NoteBlock.NOTE, 5, 25));

		// 光照方块：亮度 0-15，占低 4 位
		rule(LightBlock.class, intProp(LightBlock.LEVEL, 4, 0));

		// 结构方块：模式（保存/加载/角落/数据），占低 2 位
		rule(StructureBlock.class, enumProp(StructureBlock.MODE, 0, 2, 0));

		// 可堆肥：层数 0-8，占低 4 位；仅当规则取 with_composter_level 时才编解码，
		// 否则整项返回 0（等于「按原版放置」）
		rule(ComposterBlock.class, custom(
				state -> BetterEasyPlaceProtocolSettings.composterLevelEnabled()
						&& state.hasProperty(ComposterBlock.LEVEL)
						? (state.getValue(ComposterBlock.LEVEL) & 0b1111)
						: 0,
				(bits, state, ctx) -> {
					if (!BetterEasyPlaceProtocolSettings.composterLevelEnabled()
							|| !state.hasProperty(ComposterBlock.LEVEL)) {
						return state;
					}
					int level = Math.min(bits & 0b1111, ComposterBlock.MAX_LEVEL);
					return state.setValue(ComposterBlock.LEVEL, level);
				}));

		// 中继器：朝向低 2 位（值 -2）、延迟 bit2-3（值 -1）、锁存 bit4、通电 bit5
		rule(RepeaterBlock.class, custom(
				state -> {
					int bits = 0;
					if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
						bits |= (state.getValue(BlockStateProperties.HORIZONTAL_FACING).ordinal() - 2) & 0b11;
					}
					if (state.hasProperty(RepeaterBlock.DELAY)) {
						bits |= ((state.getValue(RepeaterBlock.DELAY) - 1) & 0b11) << 2;
					}
					if (state.hasProperty(RepeaterBlock.LOCKED) && state.getValue(RepeaterBlock.LOCKED)) {
						bits |= 0b0001_0000;
					}
					if (state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED)) {
						bits |= 0b0010_0000;
					}
					return bits;
				},
				(bits, state, ctx) -> {
					BlockState out = state;
					if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
						Direction facing = Direction.values()[((bits & 0b11) + 2) % 6];
						out = out.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
					}
					if (state.hasProperty(RepeaterBlock.DELAY)) {
						out = out.setValue(RepeaterBlock.DELAY, ((bits >>> 2) & 0b11) + 1);
					}
					boolean locked = (bits & 0b0001_0000) != 0;
					if (state.hasProperty(RepeaterBlock.LOCKED)) {
						out = out.setValue(RepeaterBlock.LOCKED, locked);
					}
					if (locked && state.hasProperty(BlockStateProperties.POWERED)) {
						out = out.setValue(BlockStateProperties.POWERED, (bits & 0b0010_0000) != 0);
					}
					return out;
				}));

		// ---------- 开关类 ----------

		// 铜灯：点亮 bit0、通电 bit1（与投影端一致，不可对调）
		rule(CopperBulbBlock.class, stack(
				boolProp(CopperBulbBlock.LIT, 0),
				boolProp(CopperBulbBlock.POWERED, 1)));

		// 红石灯：点亮 bit4
		rule(RedstoneLampBlock.class, boolProp(RedstoneLampBlock.LIT, 4));

		// 阳光探测器：反向 bit0
		rule(DaylightDetectorBlock.class, boolProp(DaylightDetectorBlock.INVERTED, 0));

		// 探测铁轨：通电 bit0
		rule(DetectorRailBlock.class, boolProp(DetectorRailBlock.POWERED, 0));

		// 灯笼：悬挂 bit0
		rule(LanternBlock.class, boolProp(LanternBlock.HANGING, 0));

		// ---------- 位域类 ----------

		// 巨型蘑菇：N/E/S/W/UP/DOWN 依次占 bit0-5（与投影端一致，不可改序）
		rule(HugeMushroomBlock.class, stack(
				boolProp(HugeMushroomBlock.NORTH, 0),
				boolProp(HugeMushroomBlock.EAST, 1),
				boolProp(HugeMushroomBlock.SOUTH, 2),
				boolProp(HugeMushroomBlock.WEST, 3),
				boolProp(HugeMushroomBlock.UP, 4),
				boolProp(HugeMushroomBlock.DOWN, 5)));

		// 墙：四向 WallSide 各占 2 位、柱高 bit8
		rule(WallBlock.class, stack(
				enumProp(WallBlock.NORTH, 0, 2, 0),
				enumProp(WallBlock.EAST, 2, 2, 0),
				enumProp(WallBlock.SOUTH, 4, 2, 0),
				enumProp(WallBlock.WEST, 6, 2, 0),
				boolProp(WallBlock.UP, 8)));

		// 红石线：只有「孤立点」这一种特殊形态需要还原（bit0）。
		// 注意连接属性是 EnumProperty<RedstoneSide>（NONE / SIDE / UP），不是布尔量。
		rule(RedstoneWireBlock.class, custom(
				state -> isIsolatedDot(state) ? 0b0001 : 0,
				(bits, state, ctx) -> {
					if ((bits & 0b0001) == 0) {
						return state;
					}
					BlockState out = state;
					for (var prop : WIRE_CONNECTIONS) {
						if (out.hasProperty(prop)) {
							out = out.setValue(prop, RedstoneSide.NONE);
						}
					}
					return out;
				}));

		// 铁轨：形状低 4 位；bit4 标记「不要自动改形状」
		rule(RailBlock.class, stack(
				enumProp(RailBlock.SHAPE, 0, 4, 0),
				custom(state -> 0b0001_0000,
						(bits, state, ctx) -> {
							if ((bits & 0b0001_0000) != 0) {
								ProjectionPlacement.setPlaceFlag(
										ProjectionPlacement.RAIL_KEEP_SHAPE);
							}
							return state;
						})));

		// ---------- 堆叠类（需要连续点几次才叠到目标） ----------

		// 雪层：层数 1-8 占低 3 位；雪是「一层层加」的
		rule(SnowLayerBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.hasProperty(SnowLayerBlock.LAYERS)) {
						return state;
					}
					// 一次只叠一层(原版行为)，这里只挡「已经比目标还多」——
					// 差的次数交给外层循环反复点。
					int target = (bits & 0b0111) + 1;
					return state.getValue(SnowLayerBlock.LAYERS) > target ? null : state;
				},
				(schem, client) -> missing(schem, client, SnowLayerBlock.class, SnowLayerBlock.LAYERS),
				(schem, client, index) -> (schem.getValue(SnowLayerBlock.LAYERS) - 1) & 0b0111));

		// 海泡菜：1-4 个占低 2 位
		rule(SeaPickleBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.hasProperty(SeaPickleBlock.PICKLES)) {
						return state;
					}
					int target = (bits & 0b0011) + 1;
					return state.getValue(SeaPickleBlock.PICKLES) > target ? null : state;
				},
				(schem, client) -> missing(schem, client, SeaPickleBlock.class, SeaPickleBlock.PICKLES),
				(schem, client, index) -> (schem.getValue(SeaPickleBlock.PICKLES) - 1) & 0b0011));

		// 海龟蛋：1-4 个占低 2 位
		rule(TurtleEggBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.hasProperty(TurtleEggBlock.EGGS)) {
						return state;
					}
					int target = (bits & 0b0011) + 1;
					return state.getValue(TurtleEggBlock.EGGS) > target ? null : state;
				},
				(schem, client) -> missing(schem, client, TurtleEggBlock.class, TurtleEggBlock.EGGS),
				(schem, client, index) -> (schem.getValue(TurtleEggBlock.EGGS) - 1) & 0b0011));

		// 蜡烛：个数 1-4 占低 2 位，点亮标记占 bit2（注意不是 bit0）
		rule(CandleBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.hasProperty(CandleBlock.CANDLES)) {
						return state;
					}
					int candles = (bits & 0b0011) + 1;
					if (state.getValue(CandleBlock.CANDLES) > candles) {
						return null;
					}
					BlockState out = state.setValue(CandleBlock.CANDLES, candles);
					if (state.hasProperty(CandleBlock.LIT) && (bits & 0b0100) != 0) {
						out = out.setValue(CandleBlock.LIT, true);
					}
					return out;
				},
				(schem, client) -> missing(schem, client, CandleBlock.class, CandleBlock.CANDLES),
				(schem, client, index) -> ((schem.getValue(CandleBlock.CANDLES) - 1) & 0b0011)
						| (schem.getValue(CandleBlock.LIT) ? 0b0100 : 0)));

		// 花床：朝向占 bit2-3，数量 1-4 占低 2 位（位序与原实现一致，不可对调）
		rule(FlowerBedBlock.class, stepped(
				(bits, state, ctx) -> {
					if (!state.hasProperty(FlowerBedBlock.AMOUNT)) {
						return state;
					}
					int maxAmount = (bits & 0b0011) + 1;
					BlockState out = state;
					BlockState at = ctx.getLevel().getBlockState(
							PlacementBitTools.clickedPos(ctx));
					if (!(at.getBlock() instanceof FlowerBedBlock)) {
						int facingOrdinal = ((bits & 0b1100) >>> 2) + 2;
						if (state.hasProperty(FlowerBedBlock.FACING)) {
							out = out.setValue(FlowerBedBlock.FACING, Direction.values()[facingOrdinal]);
						}
					}
					return out.getValue(FlowerBedBlock.AMOUNT) > maxAmount ? null : out;
				},
				(schem, client) -> missing(schem, client, FlowerBedBlock.class, FlowerBedBlock.AMOUNT),
				(schem, client, index) -> (((schem.getValue(FlowerBedBlock.FACING).ordinal() - 2) & 0b11) << 2)
						| ((schem.getValue(FlowerBedBlock.AMOUNT) - 1) & 0b11)));

		// 藤蔓：方向表是「去掉 DOWN」的 5 个（UP/N/E/S/W，索引 0..4），一次长一个方向
		rule(VineBlock.class, stepped(
				(bits, state, ctx) -> {
					int index = bits & 0b0111;
					if (index >= VINE_DIRECTIONS.length) {
						return null;
					}
					Direction direction = VINE_DIRECTIONS[index];
					BlockState at = ctx.getLevel().getBlockState(
							PlacementBitTools.clickedPos(ctx));
					boolean isVine = state.getBlock() instanceof VineBlock;
					BooleanProperty prop = VineBlock.getPropertyForFace(direction);
					if (prop == null) {
						return null;
					}
					if (isVine && state.hasProperty(prop) && state.getValue(prop)) {
						return null;
					}
					BlockState base = isVine ? state : net.minecraft.world.level.block.Blocks.VINE.defaultBlockState();
					return base.hasProperty(prop) ? base.setValue(prop, true) : null;
				},
				(schem, client) -> missingDirections(schem, client, VINE_DIRECTIONS, PlacementRules::vineHas).size(),
				(schem, client, index) -> {
					List<Integer> need = missingDirections(schem, client, VINE_DIRECTIONS, PlacementRules::vineHas);
					return index < need.size() ? need.get(index) & 0b0111 : 0b0111;
				}));

		// 发光地衣等「多面方块」：方向表是完整的 6 向（索引 0..5），一次铺一个面
		rule(MultifaceBlock.class, stepped(
				(bits, state, ctx) -> {
					int index = bits & 0b0111;
					if (index >= VINE_DIRECTIONS.length + 1) {
						return null;
					}
					Direction direction = Direction.values()[index];
					BlockState at = ctx.getLevel().getBlockState(
							PlacementBitTools.clickedPos(ctx));
					BlockState base = state.getBlock() instanceof MultifaceBlock
							? state
							: at.getBlock() instanceof MultifaceBlock
									? at
									: net.minecraft.world.level.block.Blocks.GLOW_LICHEN.defaultBlockState();
					var prop = MultifaceBlock.getFaceProperty(direction);
					return prop == null || !base.hasProperty(prop) ? null : base.setValue(prop, true);
				},
				(schem, client) -> missingDirections(schem, client, Direction.values(), PlacementRules::multifaceHas).size(),
				(schem, client, index) -> {
					List<Integer> need = missingDirections(schem, client, Direction.values(), PlacementRules::multifaceHas);
					return index < need.size() ? need.get(index) & 0b0111 : 0b0111;
				}));

		// 信标本身没有可还原的方块状态（要还原的是手持物上的效果数据）
		rule(BeaconBlock.class, PlacementCodecs.none());
	}

	// ==================== 查询 ====================

	/**
	 * 取某个方块的编解码器：沿类继承链向上找，找不到返回 {@code null}
	 * （表示该方块的放置由投影原生流程处理）。
	 */
	public static PlacementCodec codec(Block block) {
		for (Class<?> type = block.getClass(); type != null; type = type.getSuperclass()) {
			PlacementCodec found = RULES.get(type);
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

	private static void rule(Class<? extends Block> type, PlacementCodec codec) {
		RULES.put(type, codec);
	}

	private static void itemData(Class<? extends Block> type, ItemDataCodec codec) {
		ITEM_DATA.put(type, codec);
	}

	/** 距离目标还差几次点击（当前层数 → 目标层数）。 */
	private static <B extends Block> int missing(BlockState schematic, BlockState client,
	                                             Class<B> blockType, net.minecraft.world.level.block.state.properties.IntegerProperty property) {
		int current = blockType.isInstance(client.getBlock()) && client.hasProperty(property) ? client.getValue(property) : 0;
		int target = schematic.hasProperty(property) ? schematic.getValue(property) : 0;
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
		BooleanProperty prop = VineBlock.getPropertyForFace(direction);
		return prop != null && state.hasProperty(prop) && state.getValue(prop);
	}

	private static boolean multifaceHas(BlockState state, Direction direction) {
		return state.getBlock() instanceof MultifaceBlock && MultifaceBlock.hasFace(state, direction);
	}

	/** 红石线是否为「不连任何方向」的孤立点。 */
	private static boolean isIsolatedDot(BlockState state) {
		if (!state.hasProperty(RedstoneWireBlock.NORTH)) {
			return false;
		}
		for (var prop : WIRE_CONNECTIONS) {
			if (state.hasProperty(prop) && state.getValue(prop) != RedstoneSide.NONE) {
				return false;
			}
		}
		return true;
	}
}

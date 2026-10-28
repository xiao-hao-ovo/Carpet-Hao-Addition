package carpet_hao_addition.easyplace;

import carpet_hao_addition.ProjectionPlacementSettings;
import carpet_hao_addition.HaoDebug;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

/**
 * 投影「精准放置」的对外门面。
 *
 * <p>投影在客户端会按目标状态伪造一个 hitVec —— 把协议位藏进 z 的小数位；
 * 服务端再从 hitVec 里把位取回来，拼出投影真正想要的状态。本类只干接线这一件事：
 * 取位 → 交给 {@link PlacementRules} 里登记的编解码器 → 回填。
 *
 * <p>分工：
 * <ul>
 *   <li>位怎么塞进 hitVec、怎么取出来 —— {@link PlacementBitTools}；</li>
 *   <li>方块 ↔ 位怎么换算 —— {@link PlacementRules} 配 {@link PlacementCodecs}；</li>
 *   <li>「这次放置的目标在哪、有什么标记」—— {@link PlacementWindow}。</li>
 * </ul>
 */
public final class ProjectionPlacement {

	/** 铁轨：别让原版按邻居自动改形状。 */
	public static final long RAIL_KEEP_SHAPE = 1L;
	private ProjectionPlacement() {
	}

	public static boolean ruleEnabled() {
		return ProjectionPlacementSettings.isEnabled();
	}

	// ==================== 放置窗口（转发 PlacementWindow） ====================

	public static boolean isProjected() {
		return PlacementWindow.isOpen();
	}

	public static void markProjectedState(boolean value) {
		PlacementWindow.setOpen(value);
	}

	public static void noteWindowFlags(long value) {
		PlacementWindow.setFlags(value);
	}

	public static boolean flagsContain(long flag) {
		return PlacementWindow.hasFlag(flag);
	}

	public static void noteFlag(long flag) {
		PlacementWindow.raiseFlag(flag);
	}

	/**
	 * 这次放置的铁轨是否要**保持投影给的形状**，而不是让原版重算弯曲。
	 *
	 * <p>投影协议在铁轨形状位上方留了一个 bit 表示「别自动改形状」（见
	 * {@code PlacementRules} 的 RailBlock 解码器）。以前这个 bit 只是被记进 flag，
	 * 全仓没有任何读方 —— 于是照投影铺弯曲铁轨时，原版会把每节都重算成直轨。</p>
	 *
	 * <p>判据是「正好是这次的落点」。{@code placeTarget()} 记的是
	 * {@code ItemPlacementContext.getBlockPos()}，也就是方块真正落在哪一格，
	 * 不是被点击的那一格。只认这一格是因为旁边的既有铁轨要按自己的情况更新 ——
	 * 它们若是投影放过的，会由 {@link RailShapeMemory} 单独还原。</p>
	 */
	public static boolean keepRailShapeAt(BlockPos pos) {
		return flagsContain(RAIL_KEEP_SHAPE) && placeTarget().equals(pos);
	}

	/**
	 * 投影给的这个状态在当前位置**立得住**吗 —— 立不住就别放。
	 *
	 * <p>目前只校验坡轨：坡顶那一格必须有实心顶面（判据与原版
	 * {@code AbstractRailBlock.shouldDropRail} 用的 {@code Block.hasTopRim} 一致）。
	 * 坡顶没方块时，原版会把坡轨重算成平轨 —— 那样放出来的就不是投影的样子了。
	 * 与其放错，不如这一格直接放弃，让玩家先把支撑方块补上。</p>
	 *
	 * <p>非铁轨、非坡轨一律返回 {@code true}：直轨和弯轨只要求脚下有方块，
	 * 那一条原版已在 {@code canPlaceAt} 里查过。</p>
	 */
	public static boolean stateCanExist(BlockState state, BlockView world, BlockPos pos) {
		if (!(state.getBlock() instanceof AbstractRailBlock rail)
				|| !state.contains(rail.getShapeProperty())) {
			// 不是铁轨，或是没声明 SHAPE 的第三方铁轨 —— 不归这里管
			return true;
		}
		Direction top = switch (state.get(rail.getShapeProperty())) {
			case ASCENDING_EAST -> Direction.EAST;
			case ASCENDING_WEST -> Direction.WEST;
			case ASCENDING_NORTH -> Direction.NORTH;
			case ASCENDING_SOUTH -> Direction.SOUTH;
			default -> null;
		};
		return top == null || Block.hasTopRim(world, pos.offset(top));
	}

	public static BlockPos placeTarget() {
		return PlacementWindow.targetPos();
	}

	public static void assignPlaceTargetPos(BlockPos pos) {
		PlacementWindow.setTargetPos(pos);
	}

	// ==================== 方块状态 ====================

	/**
	 * 服务端还原状态：拿 {@code baseState}（投影原生已经算好的那份）当底，
	 * 再用协议位补上投影额外要求的部分。
	 *
	 * @param baseState 可以传 {@code null}，此时按 {@code block} 现算一个
	 * @return 还原后的状态；规则没开时返回 {@code null}
	 */
	public static BlockState decodeSchematicState(Block block, ItemPlacementContext context, BlockState baseState) {
		if (!ruleEnabled()) {
			return null;
		}
		if (baseState == null) {
			baseState = block.getPlacementState(context);
			if (baseState == null) {
				baseState = block.getDefaultState();
			}
		}
		return restore(context, baseState);
	}

	/** 解码入口的共用部分：取位 → 查规则表还原 → 补上只服务端需要的修正。 */
	private static BlockState restore(ItemPlacementContext context, BlockState baseState) {
		double hitZ = PlacementBitTools.hitOffsetZ(context.getHitPos(),
				PlacementBitTools.clickedPos(context));
		if (!PlacementBitTools.isProtocolHit(hitZ)) {
			return baseState;
		}
		int bits = PlacementBitTools.readBits(hitZ);
		PlacementCodec codec = PlacementRules.codec(baseState.getBlock());
		if (codec == null) {
			return baseState;
		}
		BlockState restored = codec.unpack(bits, baseState, context);
		if (restored != null) {
			baseState = restored;
		}
		if (baseState.getBlock() instanceof SculkShriekerBlock) {
			// 投影造出来的尖叫器不该因为被放下来就招出监守者
			baseState = baseState.with(SculkShriekerBlock.CAN_SUMMON, false);
		}
		return baseState;
	}

	// ==================== 物品数据 ====================

	/** 服务端真正放置前调用：把协议位写回手持物。返回 {@code null} 表示不用管。 */
	public static ItemStack applySchematicItemData(ItemStack stack, ItemPlacementContext context) {
		if (!ruleEnabled()) {
			return null;
		}
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return null;
		}
		ItemDataCodec codec = PlacementRules.itemData(blockItem.getBlock());
		if (codec == null) {
			return null;
		}
		double hitZ = PlacementBitTools.hitOffsetZ(context.getHitPos(),
				PlacementBitTools.clickedPos(context));
		if (!PlacementBitTools.isProtocolHit(hitZ)) {
			return null;
		}
		int bits = PlacementBitTools.readBits(hitZ);
		ItemStack restored = codec.decodeStack(bits, stack);
		if (HaoDebug.ENABLED) {
			HaoDebug.log("[hao-easyplace] 服务端解码 值=" + bits + " 物品=" + stack.getItem()
					+ " 还原后BlockEntityTag=" + (restored.get(DataComponentTypes.BLOCK_ENTITY_DATA) != null));
		}
		return restored;
	}

	/** 客户端编码：投影世界的方块实体拿得到时，直接从它取物品数据位。 */
	public static int encodeBlockEntityBits(BlockEntity blockEntity) {
		if (blockEntity == null) {
			return 0;
		}
		ItemDataCodec codec = PlacementRules.itemData(blockEntity.getCachedState().getBlock());
		return codec == null ? 0 : codec.encodeBlockEntity(blockEntity);
	}
}

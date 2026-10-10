package carpet_hao_addition.easyplace;

import carpet_hao_addition.ProjectionPlacementSettings;
import carpet_hao_addition.HaoDebug;
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

	public static boolean enabled() {
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

	public static BlockPos placeTarget() {
		return PlacementWindow.targetPos();
	}

	public static void assignPlaceTargetPos(BlockPos pos) {
		PlacementWindow.setTargetPos(pos);
	}

	public static Block placeTargetBlock() {
		return PlacementWindow.targetBlock();
	}

	public static void assignPlaceTargetBlock(Block block) {
		PlacementWindow.setTargetBlock(block);
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
		if (!enabled()) {
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

	/**
	 * 同 {@link #decodeSchematicState}，但用于「站着 / 挂墙」二选一的方块
	 * （灯笼、告示牌这类）：先按点击的面挑出基础状态，再还原。
	 */
	public static BlockState decodeHangingState(Block standingBlock, Block wallBlock,
														   ItemPlacementContext context) {
		if (!enabled()) {
			return null;
		}
		BlockState baseState = null;
		if (context.getSide().getAxis() != Direction.Axis.Y) {
			baseState = wallBlock.getPlacementState(context);
		}
		if (baseState == null) {
			baseState = standingBlock.getPlacementState(context);
		}
		if (baseState == null) {
			return null;
		}
		return restore(context, baseState);
	}

	/** 两条解码入口共用的部分：取位 → 查规则表还原 → 补上只服务端需要的修正。 */
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
		BlockState restored = codec.decode(bits, baseState, context);
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
		if (!enabled()) {
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
		HaoDebug.log("[hao-easyplace] 服务端解码 值=" + bits + " 物品=" + stack.getItem()
				+ " 还原后BlockEntityTag=" + (restored.get(DataComponentTypes.BLOCK_ENTITY_DATA) != null));
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

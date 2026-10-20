package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * haoProjectionPlacement：照投影放铁轨时**保持投影给的形状**，不让原版把它重算掉。
 *
 * <p>投影协议在铁轨形状位（低 4 位）上方留了一个 bit 表示「别自动改形状」，
 * {@code PlacementRules.railShapeCodec} 会把它记进 {@code RAIL_KEEP_SHAPE}。
 * 但在这之前全仓没有任何一处读这个 flag —— 半成品：协议位收下了，行为上没生效。
 * 结果是照投影铺弯曲段（转弯、上坡）时，每节放下都被原版重算，
 * 弯轨被拉直、坡轨被压平，铺出来的形状和投影不符。</p>
 *
 * <p>注入点选 {@code updateCurves}：{@code onBlockAdded}（放置）与
 * {@code neighborUpdate}（邻居变化）最后都会走到它，而它内部经
 * {@code RailPlacementHelper} 会直接 {@code World.setBlockState} 把重算结果写回世界。
 * 在这里返回原 state 就等于「这次不改形状」。</p>
 *
 * <p><b>但「保持投影形状」不能凌驾于物理可行性。</b>
 * 原版遇到坡顶没有方块的坡轨会把它降级成平轨（见 {@code shouldDropRail} 的支撑要求）。
 * 如果无条件保持投影形状，就会放出浮空坡轨 —— 而之后任何一次邻居更新又会让它掉下来。
 * 所以这里用 {@link #hao$shapeIsSupported} 做与原版一致的支撑检查：
 * 物理上立不住时放行，让原版照旧降级。</p>
 */
@Mixin(value = AbstractRailBlock.class, priority = 900)
public abstract class AbstractRailBlock_keepShapeMixin {
	@Inject(method = "updateCurves", at = @At("HEAD"), cancellable = true)
	private void hao$keepProjectedRailShape(BlockState state, World world, BlockPos pos, boolean force,
			CallbackInfoReturnable<BlockState> cir) {
		if (ProjectionPlacement.keepRailShapeAt(pos) && hao$shapeIsSupported(state, world, pos)) {
			cir.setReturnValue(state);
		}
	}

	/**
	 * 这一节坡轨在物理上立得住吗？
	 *
	 * <p>判据与原版 {@code AbstractRailBlock.shouldDropRail} 一致：坡顶那一格要有实心顶面。
	 * 非坡轨（直轨、弯轨）一律返回 {@code true} —— 它们只要求坡脚下方有方块，
	 * 那一条原版已在 {@code canPlaceAt} 里查过。</p>
	 */
	private static boolean hao$shapeIsSupported(BlockState state, World world, BlockPos pos) {
		if (!(state.getBlock() instanceof AbstractRailBlock rail)) {
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
}

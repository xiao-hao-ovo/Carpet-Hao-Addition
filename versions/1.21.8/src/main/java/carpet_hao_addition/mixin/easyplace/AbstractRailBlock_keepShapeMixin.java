package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
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
 * <p><b>关于 {@code stateCanExist} 那一半判断：</b>它在这里实际是<b>防御性冗余</b>。
 * {@code updateCurves} 只在 {@code onBlockAdded}（放置瞬间）被调用（javap 确认：
 * {@code AbstractRailBlock} 与 {@code DetectorRailBlock} 各一处），而那一步之前
 * {@code ProjectionBlockItemMixin} 已经用同一个 {@code stateCanExist} 把立不住的放置拒掉了。
 * 保留它只是为了 {@code updateCurves} 的调用面随后续版本扩大时不至于失守 ——
 * <b>不要指望它挡住「放下之后坡顶方块被挖」</b>：那种情况走的是
 * {@code neighborUpdate → shouldDropRail}（原版掉落逻辑），根本不经过这里。</p>
 */
@Mixin(value = AbstractRailBlock.class, priority = 900)
public abstract class AbstractRailBlock_keepShapeMixin {
	@Inject(method = "updateCurves", at = @At("HEAD"), cancellable = true)
	private void hao$keepProjectedRailShape(BlockState state, World world, BlockPos pos, boolean force,
			CallbackInfoReturnable<BlockState> cir) {
		if (ProjectionPlacement.keepRailShapeAt(pos)
				&& ProjectionPlacement.stateCanExist(state, world, pos)) {
			cir.setReturnValue(state);
		}
	}
}

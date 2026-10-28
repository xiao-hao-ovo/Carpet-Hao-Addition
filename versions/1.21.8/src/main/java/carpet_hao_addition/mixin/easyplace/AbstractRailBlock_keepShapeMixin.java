package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * haoProjectionPlacement：照投影放铁轨时**保持投影给的形状**，不让原版把它重算掉。
 *
 * <p>投影协议在铁轨形状位（低 4 位）上方留了一个 bit 表示「别自动改形状」，
 * {@code PlacementRules.railShapeCodec} 会把它记进 {@code RAIL_KEEP_SHAPE}。
 * 但在这之前全仓没有任何一处读这个 flag — 半成品：协议位收下了，行为上没生效。
 * 结果是照投影铺弯曲段（转弯、上坡）时，每节放下都被原版重算，
 * 弯轨被拉直、坡轨被压平，铺出来的形状和投影不符。</p>
 *
 * <p>注入点选 {@code updateCurves}：{@code onBlockAdded}（放置）与
 * {@code neighborUpdate}（邻居变化）最后都会走到它，而它内部经
 * {@code RailPlacementHelper} 会直接 {@code World.setBlockState} 把重算结果写回世界。
 * 在这里返回原 state 就等于「这次不改形状」。</p>
 *
 * <p><b>为什么还要补一发 {@code World.updateNeighbor}：</b>原版 {@code updateCurves}
 * 末尾并不是只有「重算形状」——它还会在 {@code forbidCurves}（充能铁轨、探测铁轨的
 * 构造参数，它们不允许弯曲）成立时对自己这一格发一次邻居更新，即
 * {@code world.updateNeighbor(state, pos, this, null, force)}。而
 * {@code PoweredRailBlock} 计算 {@code POWERED} 的唯一入口
 * （{@code neighborUpdate → updateBlockState(BlockState, World, BlockPos, Block)}）
 * 就在这次回调里，放置瞬间卷进来只此一条路：
 * <ul>
 *   <li>{@code getPlacementState} 只写 {@code SHAPE} 与 {@code WATERLOGGED}，从不写 {@code POWERED}；</li>
 *   <li>{@code RailPlacementHelper} 全程只有形状逻辑，没有 {@code POWERED}；</li>
 *   <li>投影协议也只带形状位（低 4 位 + 「别改形状」bit），{@code POWERED} 不在协议里。</li>
 * </ul>
 * 于是整段 HEAD 早退会连这条自更新一起吞掉 —— 照投影放下的充能铁轨保持未充能，
 * 非要等旁边有方块变化时才被补上（玩家看到的现象：放完不亮，拿充能方块碰一下才亮）。
 * 这里在保持形状的同时，把原版这一步原样补回去：<b>不硬编码 {@code POWERED}，
 * 仍然交给原版的充能判定</b>（红石强度 + 沿轨传递 8 格）。</p>
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
	/** 原版构造参数：{@code true} 的铁轨（充能/探测）不允许弯曲。 */
	@Shadow
	@Final
	private boolean forbidCurves;

	@Inject(method = "updateCurves", at = @At("HEAD"), cancellable = true)
	private void hao$keepProjectedRailShape(BlockState state, World world, BlockPos pos, boolean force,
			CallbackInfoReturnable<BlockState> cir) {
		if (ProjectionPlacement.keepRailShapeAt(pos)
				&& ProjectionPlacement.stateCanExist(state, world, pos)) {
			cir.setReturnValue(state);
			if (this.forbidCurves) {
				// 补回原版 updateCurves 末尾那一次「对自己这一格的邻居更新」：
				// 充能铁轨的 POWERED 只在这里算（见类注释）。
				world.updateNeighbor(state, pos, (Block) (Object) this, null, force);
			}
		}
	}
}

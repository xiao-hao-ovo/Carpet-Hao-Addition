package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import carpet_hao_addition.easyplace.RailShapeMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

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
 * 26.x 早先只写这个 flag、全仓没有读方 —— 半成品：协议位收下了，行为上没生效。
 * 结果是照投影铺弯曲段（转弯、上坡）时，每节放下都被原版重算，
 * 弯轨被拉直、坡轨被压平，铺出来的形状和投影不符。</p>
 *
 * <p>注入点选 {@code BaseRailBlock#updateState(BlockState, Level, BlockPos, boolean)}
 * —— 这是 1.21.8 里 {@code updateCurves} 在 26.x 的对应方法：{@code onPlace}（放置）与
 * {@code neighborChanged}（邻居变化）最后都会走到它，而它内部经 {@code updateDir}
 * 直接算出重算结果并写回世界。在这里返回原 state 就等于「这次不改形状」。</p>
 *
 * <p><b>关于 {@code stateCanExist} 那一半判断：</b>它在这里是<b>防御性冗余</b>。
 * {@code updateState} 在放置瞬间被调用之前，{@code ProjectionBlockItemMixin} 已经用同一个
 * {@code stateCanExist} 把立不住的放置拒掉了。保留它只是为了调用面随后续版本扩大时不至于失守
 * —— <b>不要指望它挡住「放下之后坡顶方块被挖」</b>：那种情况走的是原版掉落逻辑，不经过这里。</p>
 */
@Mixin(value = BaseRailBlock.class, priority = 900)
public abstract class BaseRailBlock_keepShapeMixin {
	/** 原版构造参数：{@code true} 的铁轨（充能/探测）不允许弯曲，对应 1.21.8 的 forbidCurves。 */
	@Shadow
	@Final
	private boolean isStraight;

	@Inject(
			method = "updateState(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/level/block/state/BlockState;",
			at = @At("HEAD"),
			cancellable = true)
	private void hao$keepProjectedRailShape(BlockState state, Level world, BlockPos pos, boolean force,
			CallbackInfoReturnable<BlockState> cir) {
		if (ProjectionPlacement.keepRailShapeAt(pos)
				&& ProjectionPlacement.stateCanExist(state, world, pos)) {
			// 刚放下的这一格:按投影来,并且记下投影要的形状,
			// 供后面邻居被重算时还原(见 RailShapeMemory 的类注释)。
			RailShapeMemory.remember(world, pos, state);
			cir.setReturnValue(state);
			if (this.isStraight) {
				// 补回原版 updateState 末尾那一次「对自己这一格的邻居更新」：
				// 充能铁轨的 POWERED 只在这里算（见类注释）。
				world.neighborChanged(state, pos, (Block) (Object) this, null, force);
			}
			return;
		}
		RailShape wanted = RailShapeMemory.expected(world, pos, state.getBlock());
		if (wanted != null) {
			// 这一格也是投影放的，只是现在被邻居带着重算。只还原形状 ——
			// 别的属性（动力铁轨的 POWERED 等）以当前状态为准，不能一起抹回去。
			cir.setReturnValue(state.setValue(((BaseRailBlock) state.getBlock()).getShapeProperty(), wanted));
		}
	}
}
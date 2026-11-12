package carpet_hao_addition.mixin;

import carpet_hao_addition.EnhancedWorldEaterSettings;
import carpet_hao_addition.WorldEaterProMaxSettings;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 「只开 haoEnhancedWorldEaterProMax」时，含水方块被炸掉后把水放回。
 * <p>
 * 三条规则的组合语义（与用户约定）：
 * <ul>
 *   <li>只开 {@code enhancedWorldEater}：按 AMS 原版方式，不碰含水方块（水的流体抗性仍是 100，
 *       含水方块整体炸不动）；</li>
 *   <li>只开 {@code haoEnhancedWorldEaterProMax}：{@code FluidState_breakWaterloggedMixin} 把水的
 *       流体抗性归零，含水方块可以被炸掉；这里在方块被移除后把水放回，所以只炸掉方块、水保留；</li>
 *   <li>两条同时开启：同样能炸掉含水方块，但<b>不</b>放回水，方块和水一起消失。</li>
 * </ul>
 * 钩子选 {@code ExplosionImpl.destroyBlocks}:它是 1.21.8 里真正遍历并移除被炸方块的唯一入口，
 * {@code BlockState.onExploded} 就在这个循环里调用。
 */
@Mixin(ExplosionImpl.class)
public abstract class ExplosionImpl_keepWaterMixin {
	@Shadow
	@Final
	private ServerWorld world;

	/**
	 * 每次 {@code destroyBlocks} 的「原本含水的格」栈。
	 * <p>
	 * 用栈而不是单个字段，是为了嵌套爆炸（一个爆炸的方块移除逻辑里再触发爆炸）时也能正确配对；
	 * 正常爆炸不嵌套，栈里始终只有一个元素。
	 */
	@Unique
	private static final ThreadLocal<Deque<List<BlockPos>>> hao$waterloggedStack =
			ThreadLocal.withInitial(ArrayDeque::new);

	@Inject(method = "destroyBlocks", at = @At("HEAD"))
	private void hao$recordWaterlogged(List<BlockPos> positions, CallbackInfo ci) {
		List<BlockPos> waterlogged = new ArrayList<>();
		// 只有「ProMax 开、enhancedWorldEater 关」才保留水；两条同开时水随方块一起消失。
		if (WorldEaterProMaxSettings.isEnabled() && !EnhancedWorldEaterSettings.isEnabled()) {
			for (BlockPos pos : positions) {
				BlockState state = world.getBlockState(pos);
				if (state.contains(Properties.WATERLOGGED)
						&& Boolean.TRUE.equals(state.get(Properties.WATERLOGGED))) {
					waterlogged.add(pos.toImmutable());
				}
			}
		}
		hao$waterloggedStack.get().push(waterlogged);
	}

	@Inject(method = "destroyBlocks", at = @At("RETURN"))
	private void hao$restoreWaterlogged(CallbackInfo ci) {
		Deque<List<BlockPos>> stack = hao$waterloggedStack.get();
		if (stack.isEmpty()) {
			return;
		}
		List<BlockPos> waterlogged = stack.pop();
		for (BlockPos pos : waterlogged) {
			// 只补「被炸成空气」的格;已经被邻近流体填上的格不再覆盖。
			if (!world.getBlockState(pos).isAir()) {
				continue;
			}
			world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
			// 排一次流体 tick,让放回的水按流体规则流动(否则是一格不会更新的死水)。
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}
	}
}
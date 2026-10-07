package carpet_hao_addition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * useDyeOnShulkerBox(染料染色 / 仙人掌洗色)两个 mixin 共用的逻辑。
 * <p>
 * <b>为什么放在普通类里而不是 mixin 里:</b>Mixin 要求 mixin 类中除 {@code @Inject}/{@code @Shadow}
 * 等特殊方法外的普通方法必须是 {@code private},而两个 mixin 都要复用这段逻辑,
 * 所以抽到这个普通类里,由双方静态调用。
 */
public final class ShulkerBoxContentHelper {
	private ShulkerBoxContentHelper() {
	}

	/**
	 * 该颜色的潜影盒方块。
	 * <p>
	 * 26.2 / 26.3 把 16 个 {@code Blocks.XXX_SHULKER_BOX} 常量收进了 {@code Blocks.DYED_SHULKER_BOX}
	 * 这个颜色集合,26.1.2 还是老样子(16 个独立常量),所以这一处按层各写一份,
	 * 两个实现的返回值完全等价。
	 */
	public static Block coloredShulkerBox(DyeColor color) {
		return Blocks.DYED_SHULKER_BOX.pick(color);
	}

	/**
	 * 把 {@code pos} 上的潜影盒换成 {@code targetBlock}(保留朝向),并把原方块实体的内容搬过去:
	 * <ul>
	 *   <li><b>盒内物品</b> —— 用 {@code Container} 的 {@code getItem/setItem} 逐格搬;</li>
	 *   <li><b>自定义名称</b> —— 见 {@link #copyCustomName}。</li>
	 * </ul>
	 * 之所以不整体搬数据:26.x 的 {@code BlockEntity} 存取走的是 {@code ValueInput/ValueOutput}
	 * 与组件(而非裸 {@code CompoundTag}),逐格搬 + 单独搬名称已经足够稳。
	 *
	 * @return 是否成功换掉(换失败时调用方应放弃消耗物品)
	 */
	public static boolean recolor(ServerLevel level, BlockPos pos, BlockState oldState, Block targetBlock) {
		// 先取旧方块实体:换方块会重建 BlockEntity,旧引用必须先拿到手。
		BlockEntity oldEntity = level.getBlockEntity(pos);
		BlockState newState = targetBlock.defaultBlockState()
				.setValue(ShulkerBoxBlock.FACING, oldState.getValue(ShulkerBoxBlock.FACING));
		if (!level.setBlock(pos, newState, Block.UPDATE_ALL)) {
			return false;
		}
		if (oldEntity instanceof ShulkerBoxBlockEntity oldShulker
				&& level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity newShulker) {
			for (int slot = 0; slot < oldShulker.getContainerSize() && slot < newShulker.getContainerSize(); slot++) {
				newShulker.setItem(slot, oldShulker.getItem(slot));
			}
			copyCustomName(oldEntity, newShulker);
			newShulker.setChanged();
		}
		return true;
	}

	/**
	 * 把"自定义名称"复制到新方块实体上。
	 * <p>
	 * <b>名称不在方块实体自己的字段里,而是子类贡献的组件:</b>它存在
	 * {@code BaseContainerBlockEntity} 的 {@code name} 字段中,数据组件只是这个字段对外的投影。所以:
	 * <ul>
	 *   <li><b>读</b>必须用 {@link BlockEntity#collectComponents()} —— 它会先放上组件缓存、
	 *       再调用子类的 {@code collectImplicitComponents} 把名称贡献进来;直接用
	 *       {@code components()} 只拿到组件缓存,**读不到名称**;</li>
	 *   <li><b>写</b>必须用 {@link BlockEntity#applyComponents(DataComponentMap, DataComponentPatch)} ——
	 *       它会调用子类的 {@code applyImplicitComponents} 把名称**写回字段**;
	 *       而 {@code setComponents()} 只改组件缓存,**名称不会落到字段上,于是"改名丢失"**。</li>
	 * </ul>
	 */
	public static void copyCustomName(BlockEntity from, BlockEntity to) {
		Component name = from.collectComponents().get(DataComponents.CUSTOM_NAME);
		if (name == null) {
			return;
		}
		DataComponentMap.Builder builder = DataComponentMap.builder().addAll(to.collectComponents());
		to.applyComponents(builder.set(DataComponents.CUSTOM_NAME, name).build(), DataComponentPatch.EMPTY);
	}
}

package carpet_hao_addition;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

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
	 * 把 {@code pos} 上的潜影盒换成 {@code targetBlock}(保留朝向),并把原方块实体的内容搬过去:
	 * <ul>
	 *   <li><b>盒内物品</b> —— 用 {@code Inventory} 的 {@code getStack/setStack} 逐格搬;</li>
	 *   <li><b>自定义名称</b> —— 见 {@link #copyCustomName}。</li>
	 * </ul>
	 * 之所以不整体搬 NBT:1.21.8 的 {@code BlockEntity.read} 要的是 {@code ReadView} 而非
	 * {@code NbtCompound},参数类型对不上,而逐格搬已经足够稳。
	 *
	 * @return 是否成功换掉(换失败时调用方应放弃消耗物品)
	 */
	public static boolean recolor(ServerWorld world, BlockPos pos, BlockState oldState, Block targetBlock) {
		// 先取旧方块实体:换方块会重建 BlockEntity,旧引用必须先拿到手。
		BlockEntity oldEntity = world.getBlockEntity(pos);
		BlockState newState = targetBlock.getDefaultState()
				.with(ShulkerBoxBlock.FACING, oldState.get(ShulkerBoxBlock.FACING));
		if (!world.setBlockState(pos, newState, Block.NOTIFY_ALL)) {
			return false;
		}
		if (oldEntity instanceof ShulkerBoxBlockEntity oldShulker
				&& world.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity newShulker) {
			for (int slot = 0; slot < oldShulker.size() && slot < newShulker.size(); slot++) {
				newShulker.setStack(slot, oldShulker.getStack(slot));
			}
			copyCustomName(oldEntity, newShulker);
			newShulker.markDirty();
		}
		return true;
	}

	/**
	 * 把"自定义名称"复制到新方块实体上。
	 * <p>
	 * <b>1.21 起名称不在 {@code BlockEntity} 里:</b>它存在子类
	 * {@code LockableContainerBlockEntity} 的 {@code customName} **字段**中,
	 * 数据组件只是这个字段对外的投影。所以:
	 * <ul>
	 *   <li><b>读</b>必须用 {@link BlockEntity#createComponentMap()} —— 它会先放上组件缓存、
	 *       再调用子类的 {@code addComponents()} 把名称贡献进来;直接用
	 *       {@code getComponents()} 只拿到组件缓存,**读不到名称**;</li>
	 *   <li><b>写</b>必须用 {@link BlockEntity#readComponents(ComponentMap, ComponentChanges)} ——
	 *       它会调用子类的 {@code readComponents(ComponentsAccess)} 把名称**写回字段**;
	 *       而 {@code setComponents()} 只改组件缓存,**名称不会落到字段上,于是"改名丢失"**。</li>
	 * </ul>
	 * 另外旧版的 {@code getCustomName()}/{@code setCustomName(Text)} 在 {@code BlockEntity} 上
	 * 已于 1.21 移除(它们随名称一起挪进了子类)。
	 */
	public static void copyCustomName(BlockEntity from, BlockEntity to) {
		Text name = from.createComponentMap().get(DataComponentTypes.CUSTOM_NAME);
		if (name == null) {
			return;
		}
		ComponentMap.Builder builder = ComponentMap.builder().addAll(to.createComponentMap());
		to.readComponents(builder.add(DataComponentTypes.CUSTOM_NAME, name).build(),
				ComponentChanges.EMPTY);
	}
}

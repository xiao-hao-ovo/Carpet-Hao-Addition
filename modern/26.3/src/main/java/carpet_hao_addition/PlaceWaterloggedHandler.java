package carpet_hao_addition;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * easyPlaceWaterlogged 的注册与结算(与 1.21.8 版行为一致,Mojang 名)。
 * <p>
 * 分工:客户端只表明「此刻正在轻松放置照投影施工」以及「哪些格该补什么」(见
 * {@link carpet_hao_addition.mixin.WorldUtils_easyPlaceWaterloggedMixin}),<b>放置与扣材料都由服务端做</b>。
 * <p>
 * 四类东西在投影里出现,但都没有"直接可放"的物品形式,原版与 Litematica 都不会正确落下:
 * <ul>
 *   <li><b>水源 / 气泡柱</b> —— 服务端直接放一格水源,扣 1 个冰;</li>
 *   <li><b>源岩浆</b> —— 服务端直接放一格源岩浆,扣 1 个岩浆块;</li>
 *   <li><b>装岩浆的炼药锅</b> —— 炼药锅走正常放置流程(Litematica 用一个炼药锅物品放下
 *       {@code cauldron}),服务端只在方块落下后把那一格换成装岩浆的炼药锅,再扣 1 个岩浆块;</li>
 *   <li><b>火 / 灵魂火 / 下界传送门</b> —— 服务端点火,优先扣火焰弹 1 个、否则扣打火石 1 点耐久。</li>
 * </ul>
 * <b>含水方块补水为什么用"每 tick 轮询"而不是监听方块放置事件</b>:方块放置事件靠注入
 * {@code BlockItem.place},而该方法很容易被其它扩展做取消型注入,一旦被取消我们的注入就整段跳过,
 * 表现为"偶尔不补水"。改成:客户端把待补水格报过来,服务端每 tick 检查这些格 —— 方块一落上去
 * (可含水且还没含水)就补,与"谁在什么时候放、走的哪条路径"完全无关。
 */
public final class PlaceWaterloggedHandler {
	/** 待补水格:玩家 UUID → (格子 → 请求时刻)。服务端每 tick 检查这些格,方块落下即补。 */
	private static final Map<UUID, Map<BlockPos, Long>> PENDING_WATERLOG = new HashMap<>();
	/** 待灌岩浆的炼药锅格:玩家 UUID → (格子 → 请求时刻)。方块(炼药锅)落下后灌岩浆。 */
	private static final Map<UUID, Map<BlockPos, Long>> PENDING_LAVA_CAULDRON = new HashMap<>();
	/** 待处理记录的存活时长(服务端刻)。 */
	private static final long PENDING_TTL = 40L;

	private static boolean payloadRegistered;
	private static boolean receiverRegistered;
	private static boolean tickRegistered;

	private PlaceWaterloggedHandler() {
	}

	/** 注册 C2S payload 类型。客户端与服务端都要调用(幂等)。 */
	public static void registerPayloadType() {
		if (payloadRegistered) {
			return;
		}
		payloadRegistered = true;
		// 26.x:PayloadTypeRegistry.playC2S() 变成了 serverboundPlay()
		PayloadTypeRegistry.serverboundPlay().register(PlaceWaterloggedPayload.ID, PlaceWaterloggedPayload.CODEC);
	}

	/** 注册服务端接收端与每 tick 轮询(仅服务端调用,幂等)。 */
	public static void registerServerReceiver() {
		registerPayloadType();
		registerTickHandler();
		if (receiverRegistered) {
			return;
		}
		receiverRegistered = true;
		ServerPlayNetworking.registerGlobalReceiver(PlaceWaterloggedPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				if (!EasyPlaceWaterloggedSettings.isEnabled() || !payload.active()) {
					PENDING_WATERLOG.remove(player.getUUID());
					PENDING_LAVA_CAULDRON.remove(player.getUUID());
					return;
				}
				if (!(player.level() instanceof ServerLevel world)) {
					return;
				}
				long now = world.getGameTime();
				switch (payload.kind()) {
					case PlaceWaterloggedPayload.KIND_WATERLOG -> {
						// 只记下"待补水格",随后由每 tick 轮询在方块落下时补。
						markPending(PENDING_WATERLOG, player, payload.positions(), now);
						prunePending(PENDING_WATERLOG, now);
					}
					case PlaceWaterloggedPayload.KIND_LAVA_CAULDRON -> {
						// 只记下"待灌岩浆的炼药锅格",炼药锅本身交给正常放置流程。
						markPending(PENDING_LAVA_CAULDRON, player, payload.positions(), now);
						prunePending(PENDING_LAVA_CAULDRON, now);
					}
					default -> {
						for (BlockPos pos : payload.positions()) {
							placeRequested(player, world, pos, payload.kind());
						}
					}
				}
			});
		});
	}

	/** 注册每 tick 检查(幂等)。 */
	public static void registerTickHandler() {
		if (tickRegistered) {
			return;
		}
		tickRegistered = true;
		ServerTickEvents.END_SERVER_TICK.register(PlaceWaterloggedHandler::onServerTick);
	}

	/**
	 * 每 tick 检查待处理格:
	 * <ol>
	 *   <li>待灌岩浆的炼药锅格 —— 一旦那格是(空)炼药锅,就换成装岩浆的炼药锅并扣 1 个岩浆块;</li>
	 *   <li>待补水格 —— 一旦那格放上了可含水却没含水的方块,就补上含水并扣 1 个冰。</li>
	 * </ol>
	 */
	private static void onServerTick(MinecraftServer server) {
		if (PENDING_WATERLOG.isEmpty() && PENDING_LAVA_CAULDRON.isEmpty()) {
			return;
		}
		// 每 tick 只处理一格:之前攒下多格待处理时不会"一次性全补上"。
		processPending(server, PENDING_LAVA_CAULDRON, PlaceWaterloggedHandler::tryFillCauldron, true);
		processPending(server, PENDING_WATERLOG, PlaceWaterloggedHandler::tryWaterlog, true);
	}

	private interface PendingAction {
		/** @return true 表示这一格已处理完,可以从名单里移除。 */
		boolean apply(ServerPlayer player, ServerLevel world, BlockPos pos);
	}

	private static void processPending(MinecraftServer server, Map<UUID, Map<BlockPos, Long>> table,
			PendingAction action, boolean onePerTick) {
		if (table.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Map<BlockPos, Long>>> it = table.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Map<BlockPos, Long>> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !(player.level() instanceof ServerLevel world)) {
				it.remove();
				continue;
			}
			// 触发条件不满足时(例如要求站立而玩家正蹲着)暂停处理:记录先留着,等条件满足或自动过期。
			if (!EasyPlaceWaterloggedSettings.triggerAllowed(player.isShiftKeyDown())) {
				continue;
			}
			long now = world.getGameTime();
			Iterator<Map.Entry<BlockPos, Long>> pendingIterator = entry.getValue().entrySet().iterator();
			while (pendingIterator.hasNext()) {
				Map.Entry<BlockPos, Long> pending = pendingIterator.next();
				if (now - pending.getValue() > PENDING_TTL) {
					pendingIterator.remove();
					continue;
				}
				if (action.apply(player, world, pending.getKey())) {
					pendingIterator.remove();
					if (onePerTick) {
						return; // 本 tick 已处理一格,剩下的留到下一 tick
					}
				}
			}
			if (entry.getValue().isEmpty()) {
				it.remove();
			}
		}
	}

	/** 该格已经是(空)炼药锅 → 换成装岩浆的炼药锅,扣 1 个岩浆块。 */
	private static boolean tryFillCauldron(ServerPlayer player, ServerLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.is(Blocks.LAVA_CAULDRON)) {
			return true;
		}
		if (state.is(Blocks.CAULDRON)) {
			if (!consumeOne(player, Items.MAGMA_BLOCK)) {
				return false; // 暂时没有岩浆块:留着,等有材料或过期
			}
			world.setBlock(pos, Blocks.LAVA_CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
			return true;
		}
		// 落的是别的方块(不是炼药锅)→ 这次目标作废;还是空气则继续等它落下。
		return !state.isAir();
	}

	/** 该格已放上"可含水却没含水"的方块 → 补上含水,扣 1 个冰。 */
	private static boolean tryWaterlog(ServerPlayer player, ServerLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) {
			return false; // 还没放上方块:继续等
		}
		if (!state.hasProperty(BlockStateProperties.WATERLOGGED)
				|| Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED))) {
			return true; // 这个方块不用补水 / 已经含水 → 这次目标结束
		}
		if (!consumeOne(player, Items.ICE)) {
			return false; // 暂时没有冰:留着,等有材料或过期
		}
		world.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_ALL);
		// 排一次流体 tick,保证含的水按流体规则更新(灵魂沙上生成气泡柱)。
		world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		return true;
	}

	/** 按客户端请求的类型直接放置:水源 / 源岩浆 / 点火。 */
	private static void placeRequested(ServerPlayer player, ServerLevel world, BlockPos pos, int kind) {
		switch (kind) {
			case PlaceWaterloggedPayload.KIND_WATER ->
					placeFluid(player, world, pos, Fluids.WATER, Blocks.WATER, Items.ICE);
			case PlaceWaterloggedPayload.KIND_LAVA ->
					placeFluid(player, world, pos, Fluids.LAVA, Blocks.LAVA, Items.MAGMA_BLOCK);
			case PlaceWaterloggedPayload.KIND_IGNITE -> ignite(player, world, pos);
			default -> {
			}
		}
	}

	/** 放一格流体源:逐格校验(距离、位置可替换、不能已是同种源流体),并扣掉 1 个材料。 */
	private static void placeFluid(ServerPlayer player, ServerLevel world, BlockPos pos,
			Fluid fluid, Block block, Item cost) {
		if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
			return;
		}
		FluidState current = world.getFluidState(pos);
		if (!current.isEmpty() && current.isSource()) {
			// 已经是源流体,不必再放。
			return;
		}
		BlockState state = world.getBlockState(pos);
		// 流动流体格要允许被源覆盖:否则流体一旦流到投影里的源格,那格就永远补不上源。
		boolean flowingFluid = !current.isEmpty();
		if (!state.isAir() && !state.canBeReplaced() && !flowingFluid) {
			return;
		}
		if (!consumeOne(player, cost)) {
			return;
		}
		world.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
		// 排流体 tick:否则放下的是"死水/死岩浆"(不流动,灵魂沙上也不生成气泡柱)。
		world.scheduleTick(pos, fluid, fluid.getTickDelay(world));
	}

	/**
	 * 在指定位置点火:优先消耗火焰弹(1 个),其次打火石(1 点耐久)。
	 * <p>
	 * 采用原版点火方式({@link BaseFireBlock#getState} 决定状态),因此在灵魂沙/灵魂土上会点出
	 * 灵魂火,在合法的黑曜石框架内会由原版逻辑转成下界传送门。
	 */
	private static void ignite(ServerPlayer player, ServerLevel world, BlockPos pos) {
		if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
			return;
		}
		BlockState current = world.getBlockState(pos);
		if (current.is(Blocks.FIRE) || current.is(Blocks.SOUL_FIRE) || current.is(Blocks.NETHER_PORTAL)) {
			return; // 已经点着了
		}
		if (!current.isAir() && !current.canBeReplaced()) {
			return; // 该格已经有别的东西
		}
		if (!consumeFireSource(player)) {
			return; // 既没有火焰弹也没有打火石
		}
		world.setBlock(pos, BaseFireBlock.getState(world, pos), Block.UPDATE_ALL);
	}

	/** 点火消耗:优先火焰弹(消耗 1 个),其次打火石(消耗 1 点耐久);两者都可从潜影盒取。 */
	private static boolean consumeFireSource(ServerPlayer player) {
		return consumeOne(player, Items.FIRE_CHARGE) || damageOne(player, Items.FLINT_AND_STEEL);
	}

	private static void markPending(Map<UUID, Map<BlockPos, Long>> table, ServerPlayer player,
			List<BlockPos> positions, long now) {
		Map<BlockPos, Long> pending = table.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
		for (BlockPos pos : positions) {
			pending.put(pos, now);
		}
	}

	/** 清理过期的待处理记录。 */
	private static void prunePending(Map<UUID, Map<BlockPos, Long>> table, long now) {
		for (Map<BlockPos, Long> pending : table.values()) {
			pending.entrySet().removeIf(entry -> now - entry.getValue() > PENDING_TTL);
		}
	}

	/**
	 * 从背包(含快捷栏/副手)里扣掉 1 个指定物品;背包里没有时,<b>按规则取值从背包里的潜影盒取料</b>。
	 * <p>
	 * 潜影盒取货不依赖 QuickShulker(它是纯客户端 mod):这里直接读写潜影盒的 container 组件,
	 * 取出后会正常进背包,再由扣除逻辑扣掉;改完的物品由服务端同步给客户端。
	 */
	private static boolean consumeOne(ServerPlayer player, Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty() || !stack.is(item)) {
				continue;
			}
			stack.shrink(1);
			if (stack.isEmpty()) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
			player.getInventory().setChanged();
			player.containerMenu.broadcastChanges();
			return true;
		}
		return switch (EasyPlaceWaterloggedSettings.shulkerMode()) {
			// 方案一:直接在潜影盒里扣除,材料不拿到背包。
			case DIRECT -> consumeFromShulker(player, item);
			// 方案二:先把材料取到背包,材料留在背包里(不再从背包扣掉)。
			case TAKE -> takeFromShulker(player, item);
			case FALSE -> false;
		};
	}

	/**
	 * 方案二:从背包里的潜影盒中【取出整个堆叠】放进背包(等价于 QuickShulker 的自动取货)。
	 * <p>
	 * 一个堆叠不会超过该物品的一组,所以"盒内该物品有一组及以上就取一组、不满一组就全部取出"
	 * 天然成立;背包放不下时只放入能放下的部分,剩下的仍留在潜影盒里(不丢失)。
	 */
	private static boolean takeFromShulker(ServerPlayer player, Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack boxStack = player.getInventory().getItem(slot);
			if (boxStack.isEmpty() || !(boxStack.getItem() instanceof BlockItem blockItem)
					|| !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
				continue;
			}
			ItemContainerContents container = boxStack.get(DataComponents.CONTAINER);
			if (container == null) {
				continue;
			}
			List<ItemStack> contents = new ArrayList<>();
			container.itemCopies().forEach(stack -> contents.add(stack.copy()));
			for (int i = 0; i < contents.size(); i++) {
				ItemStack inner = contents.get(i);
				if (inner.isEmpty() || !inner.is(item)) {
					continue;
				}
				ItemStack taken = inner.copy();
				// Inventory.add 会把放进去的部分从 taken 里扣掉,剩下的仍留在 taken 中。
				player.getInventory().add(taken);
				if (taken.getCount() == inner.getCount()) {
					continue; // 一个都没放进去(背包满了):不动这个潜影盒
				}
				contents.set(i, taken.isEmpty() ? ItemStack.EMPTY : taken);
				boxStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
				player.getInventory().setChanged();
				player.containerMenu.broadcastChanges();
				return true;
			}
		}
		return false;
	}

	/** 对背包(含快捷栏/副手)里该物品消耗 1 点耐久;背包里没有时,按规则取值从潜影盒取料后消耗。 */
	private static boolean damageOne(ServerPlayer player, Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && stack.is(item)) {
				stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
				player.getInventory().setChanged();
				player.containerMenu.broadcastChanges();
				return true;
			}
		}
		return switch (EasyPlaceWaterloggedSettings.shulkerMode()) {
			// 方案一:直接在潜影盒里消耗 1 点耐久。
			case DIRECT -> damageInShulker(player, item);
			// 方案二:先取到背包,再在背包里消耗 1 点耐久(用完留在背包)。
			case TAKE -> takeFromShulker(player, item) && damageOne(player, item);
			case FALSE -> false;
		};
	}

	/** 方案一:直接在潜影盒里扣掉 1 个指定物品(不把材料拿到背包)。 */
	private static boolean consumeFromShulker(ServerPlayer player, Item item) {
		return editShulkerContents(player, item, inner -> inner.shrink(1));
	}

	/** 方案一:直接在潜影盒里给该物品消耗 1 点耐久。 */
	private static boolean damageInShulker(ServerPlayer player, Item item) {
		return editShulkerContents(player, item,
				inner -> inner.hurtAndBreak(1, player, EquipmentSlot.MAINHAND));
	}

	/** 在背包里的潜影盒内容中就地修改该物品(找到即执行 action、写回组件并返回 true)。 */
	private static boolean editShulkerContents(ServerPlayer player, Item item, Consumer<ItemStack> action) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack boxStack = player.getInventory().getItem(slot);
			if (boxStack.isEmpty() || !(boxStack.getItem() instanceof BlockItem blockItem)
					|| !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
				continue;
			}
			ItemContainerContents container = boxStack.get(DataComponents.CONTAINER);
			if (container == null) {
				continue;
			}
			List<ItemStack> contents = new ArrayList<>();
			container.itemCopies().forEach(stack -> contents.add(stack.copy()));
			boolean changed = false;
			for (ItemStack inner : contents) {
				if (!inner.isEmpty() && inner.is(item)) {
					action.accept(inner);
					changed = true;
					break;
				}
			}
			if (!changed) {
				continue;
			}
			boxStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
			player.getInventory().setChanged();
			player.containerMenu.broadcastChanges();
			return true;
		}
		return false;
	}
}

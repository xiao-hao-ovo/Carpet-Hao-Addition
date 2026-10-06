package carpet_hao_addition;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

/**
 * rocketShulker 规则的服务端实现。
 * <p>
 * 每 tick 检查在线玩家:目标格空出来(烟花用光)时,就从背包里名称正好是 {@code rocket} 的潜影盒中
 * 取一组(最多 64 个)烟花火箭放进去;盒子里的烟花拿完后会自动尝试下一个火箭盒。
 * 目标格由命令设置:{@code /rocketShulker offhand}(副手,默认)或
 * {@code /rocketShulker mainhand <1-9>}(主手快捷栏第 N 格),设置只存在内存里。
 * 目标格拿着别的物品(不是烟花)时不会去动它。
 * <p>
 * 只通过潜影盒的 container 组件读写,不依赖任何客户端 mod。
 */
public final class RocketShulkerHandler {
	private static boolean tickRegistered;

	private RocketShulkerHandler() {
	}

	/** 注册每 tick 检查(幂等)。 */
	public static void registerTickHandler() {
		if (tickRegistered) {
			return;
		}
		tickRegistered = true;
		ServerTickEvents.END_SERVER_TICK.register(RocketShulkerHandler::hao$onServerTick);
	}

	private static void hao$onServerTick(MinecraftServer server) {
		if (!RocketShulkerSettings.isEnabled()) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (RocketShulkerSettings.target(player.getUuid()) == RocketShulkerSettings.Target.OFFHAND) {
				hao$refillOffhand(player);
			} else {
				hao$refillHotbar(player);
			}
		}
	}

	/** offhand 模式:副手空了就从"火箭盒"里补一组烟花;副手有东西时不动。 */
	private static void hao$refillOffhand(ServerPlayerEntity player) {
		if (!player.getOffHandStack().isEmpty()) {
			return; // 副手还拿着东西:烟花没用完就不打扰,别的物品更不能抢
		}
		PlayerInventory inventory = player.getInventory();
		ItemStack taken = hao$takeFromRocketBoxes(inventory, RocketShulkerSettings.REFILL_AMOUNT);
		if (taken.isEmpty()) {
			return;
		}
		// setStackInHand 会把副手内容同步给客户端,再标脏背包确保服务端数据一致。
		player.setStackInHand(Hand.OFF_HAND, taken);
		inventory.markDirty();
		player.currentScreenHandler.syncState();
	}

	/** mainhand 模式:把烟花补到快捷栏指定的那一格(1-9 → 索引 0-8)。 */
	private static void hao$refillHotbar(ServerPlayerEntity player) {
		int index = RocketShulkerSettings.hotbarSlot(player.getUuid()) - 1;
		PlayerInventory inventory = player.getInventory();
		if (!inventory.getStack(index).isEmpty()) {
			return; // 那一格还有东西(没用完的烟花或别的物品):不动它
		}
		ItemStack taken = hao$takeFromRocketBoxes(inventory, RocketShulkerSettings.REFILL_AMOUNT);
		if (taken.isEmpty()) {
			return;
		}
		inventory.setStack(index, taken);
		inventory.markDirty();
		player.currentScreenHandler.syncState();
	}

	/** 依次从背包里的"火箭盒"取出至多 amount 个烟花火箭,返回取出的堆叠(可能为空)。 */
	private static ItemStack hao$takeFromRocketBoxes(PlayerInventory inventory, int amount) {
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack boxStack = inventory.getStack(slot);
			if (!hao$isRocketShulker(boxStack)) {
				continue;
			}
			ItemStack taken = hao$takeFireworks(boxStack, amount);
			if (!taken.isEmpty()) {
				return taken;
			}
		}
		return ItemStack.EMPTY;
	}

	/** 自定义名称正好是 "rocket" 的潜影盒。 */
	private static boolean hao$isRocketShulker(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)
				|| !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
			return false;
		}
		Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
		return customName != null && RocketShulkerSettings.SHULKER_NAME.equals(customName.getString());
	}

	/** 从潜影盒里取出至多 amount 个烟花火箭:返回取出的堆叠(可能为空),剩余部分写回盒内。 */
	private static ItemStack hao$takeFireworks(ItemStack boxStack, int amount) {
		ContainerComponent container = boxStack.get(DataComponentTypes.CONTAINER);
		if (container == null) {
			return ItemStack.EMPTY;
		}
		List<ItemStack> contents = new ArrayList<>();
		container.stream().forEach(s -> contents.add(s.copy()));
		for (int i = 0; i < contents.size(); i++) {
			ItemStack inner = contents.get(i);
			if (inner.isEmpty() || !inner.isOf(Items.FIREWORK_ROCKET)) {
				continue;
			}
			int takenCount = Math.min(amount, inner.getCount());
			ItemStack taken = inner.copyWithCount(takenCount);
			inner.decrement(takenCount);
			contents.set(i, inner.isEmpty() ? ItemStack.EMPTY : inner);
			boxStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(contents));
			return taken;
		}
		return ItemStack.EMPTY;
	}
}

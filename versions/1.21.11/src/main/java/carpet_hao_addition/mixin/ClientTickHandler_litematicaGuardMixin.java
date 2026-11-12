package carpet_hao_addition.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.scheduler.ClientTickHandler;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ConcurrentModificationException;

/**
 * 给 Litematica 的客户端 tick 处理加一道护栏(仅客户端)。
 * <p>
 * 1.21.8 的对应实现是 {@code @Redirect} 指向
 * {@code SchematicPlacementManager.processQueuedChunks()}，把「这一轮排队区块处理」包起来。
 * 但 Litematica 0.26.16 移除了 {@code processQueuedChunks} —— 排队改由
 * {@code LinkedBlockingQueue<PlacementManagerTask> pendingTasks} 承载，处理逻辑进了
 * {@code SchematicPlacementManager.onClientTick}；原来的注入点已不存在。
 * </p>
 * <p>
 * 这里改为用 {@code @WrapMethod} 包住 {@code ClientTickHandler.onClientTick} 整体：
 * 注入点与 1.21.8 处于同一层（都在 Litematica 的客户端 tick 入口），语义等价 ——
 * 捕获 {@link ConcurrentModificationException} 并跳过本轮，其它异常照常抛出，
 * 不掩盖真正的问题。{@code pendingTasks} 本身是线程安全队列，未处理完的任务下个 tick 会继续。
 * </p>
 */
@Mixin(ClientTickHandler.class)
public abstract class ClientTickHandler_litematicaGuardMixin {
	private static final Logger HAO_LOGGER = LoggerFactory.getLogger("carpet-hao-addition");
	/** 抑制刷屏:同一条警告最少间隔 10 秒。 */
	private static long hao$lastWarnMillis = Long.MIN_VALUE;

	@WrapMethod(method = "onClientTick")
	private void hao$guardOnClientTick(MinecraftClient client, Operation<Void> original) {
		try {
			original.call(client);
		} catch (ConcurrentModificationException e) {
			long now = System.currentTimeMillis();
			if (hao$lastWarnMillis == Long.MIN_VALUE || now - hao$lastWarnMillis >= 10_000L) {
				hao$lastWarnMillis = now;
				HAO_LOGGER.warn("[hao-guard] 跳过 Litematica 本轮 tick 处理(它自己的集合被并发修改): {}", e.toString());
			}
		}
	}
}

package carpet_hao_addition.mixin;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.scheduler.ClientTickHandler;

import java.util.ConcurrentModificationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 给 Litematica 的「排队区块处理」加一道护栏(仅客户端)。
 * <p>
 * Litematica 0.23.x 的 {@code SchematicPlacementManager.processQueuedChunks()} 在遍历
 * {@code chunkRebuildQueue} 的过程中会调用 {@code unloadSchematicChunk/loadChunk},
 * 而这些调用又会经 {@code onClientChunkLoad/markChunkForRebuild} 往同一个集合里加东西 ——
 * 迭代器随后 {@code remove()} 就抛 {@code ConcurrentModificationException}:
 *
 * <pre>
 * java.util.ConcurrentModificationException
 *     at java.util.HashMap$HashIterator.remove(HashMap.java:1620)
 *     at fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.processQueuedChunks(SchematicPlacementManager.java:206)
 *     at fi.dy.masa.litematica.scheduler.ClientTickHandler.onClientTick(ClientTickHandler.java:45)
 * </pre>
 *
 * 这个异常会从客户端 tick 里冒出来直接把游戏打崩(本机 2026-09-06 起反复出现,与本模组无关)。
 * <p>
 * 处理方式:只把「这一次排队处理」包起来,捕获该异常并跳过。{@code chunkRebuildQueue} 本身是完好的,
 * 只是这一轮没处理完,下一个 tick 会接着处理,所以表现为「偶尔掉一两帧渲染更新」而不是崩溃。
 * 其它异常一律原样抛出,不掩盖真正的问题。
 */
@Mixin(ClientTickHandler.class)
public abstract class ClientTickHandler_litematicaGuardMixin {
	private static final Logger HAO_LOGGER = LoggerFactory.getLogger("carpet-hao-addition");
	/** 抑制刷屏:同一条警告最少间隔 10 秒。 */
	private static long hao$lastWarnMillis = Long.MIN_VALUE;

	@Redirect(method = "onClientTick",
			at = @At(value = "INVOKE",
					target = "Lfi/dy/masa/litematica/schematic/placement/SchematicPlacementManager;processQueuedChunks()V"),
			require = 0)
	private void hao$guardProcessQueuedChunks(SchematicPlacementManager manager) {
		try {
			manager.processQueuedChunks();
		} catch (ConcurrentModificationException e) {
			long now = System.currentTimeMillis();
			if (hao$lastWarnMillis == Long.MIN_VALUE || now - hao$lastWarnMillis >= 10_000L) {
				hao$lastWarnMillis = now;
				HAO_LOGGER.warn("[hao-guard] 跳过 Litematica 本轮排队区块处理(它自己的集合被并发修改): {}", e.toString());
			}
		}
	}
}

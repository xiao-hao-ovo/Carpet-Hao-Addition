package carpet_hao_addition.zoneguard.region;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.ticks.ScheduledTick;

/**
 * 清除区域后恢复需要种子更新的侦测器电路。
 * <p>
 * 与参考实现(MC 26.2)逻辑等价:只扫描已加载区块内"面对面"的侦测器对并为其
 * 各计划一次方块刻,避免孤立侦测器被清除命令触发脉冲,也避免同一对被计数两次。
 * 按当前工程架构位于 versions 层(依赖 1.21.8 yarn 的 chunk/section/tick API)。
 */
public final class RegionObserverRefresh {
	private RegionObserverRefresh() {
	}

	public static int startLoadedFaceToFacePairs(ServerLevel world, DetectorRegion region) {
		int startedPairs = 0;
		int minChunkX = region.min().getX() >> 4;
		int maxChunkX = region.max().getX() >> 4;
		int minChunkZ = region.min().getZ() >> 4;
		int maxChunkZ = region.max().getZ() >> 4;

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				LevelChunk chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}

				startedPairs += startPairsInChunk(world, region, chunk);
			}
		}

		return startedPairs;
	}

	private static int startPairsInChunk(ServerLevel world, DetectorRegion region, LevelChunk chunk) {
		int startedPairs = 0;
		int chunkStartX = chunk.getPos().getMinBlockX();
		int chunkStartZ = chunk.getPos().getMinBlockZ();
		// sectionArray[0] 对应区块底部的 section;用绝对 section 坐标遍历,再换算数组索引
		int bottomSection = SectionPos.blockToSectionCoord(chunk.getMinY());
		LevelChunkSection[] sections = chunk.getSections();
		int topSection = bottomSection + sections.length - 1;
		int minSection = Math.max(SectionPos.blockToSectionCoord(region.min().getY()), bottomSection);
		int maxSection = Math.min(SectionPos.blockToSectionCoord(region.max().getY()), topSection);

		for (int sectionIndex = minSection; sectionIndex <= maxSection; sectionIndex++) {
			LevelChunkSection section = sections[sectionIndex - bottomSection];
			if (section.hasOnlyAir() || !section.getStates().maybeHas(state -> state.is(Blocks.OBSERVER))) {
				continue;
			}

			// sectionIndex << 4 == 该 section 底部的绝对 y(世界高度可为负时同样成立)
			int sectionBaseY = sectionIndex << 4;
			int minY = Math.max(region.min().getY(), sectionBaseY);
			int maxY = Math.min(region.max().getY(), sectionBaseY + 15);
			int minX = Math.max(region.min().getX(), chunkStartX);
			int maxX = Math.min(region.max().getX(), chunkStartX + 15);
			int minZ = Math.max(region.min().getZ(), chunkStartZ);
			int maxZ = Math.min(region.max().getZ(), chunkStartZ + 15);

			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					for (int x = minX; x <= maxX; x++) {
						BlockPos pos = new BlockPos(x, y, z);
						if (startPairIfNeeded(world, region, pos)) {
							startedPairs++;
						}
					}
				}
			}
		}

		return startedPairs;
	}

	private static boolean startPairIfNeeded(ServerLevel world, DetectorRegion region, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (!state.is(Blocks.OBSERVER)) {
			return false;
		}

		Direction facing = state.getValue(DirectionalBlock.FACING);
		BlockPos otherPos = pos.relative(facing);
		// The scanned pos is guaranteed to be in a loaded chunk (outer loop only visits
		// loaded chunks), but the partner may sit in a neighbouring chunk: guard with the
		// chunk coordinates — isChunkLoaded expects a CHUNK position, not a block position.
		if (!region.contains(world.dimension(), otherPos)
				|| !world.getChunkSource().hasChunk(otherPos.getX() >> 4, otherPos.getZ() >> 4)) {
			return false;
		}

		BlockState otherState = world.getBlockState(otherPos);
		if (!otherState.is(Blocks.OBSERVER) || otherState.getValue(DirectionalBlock.FACING) != facing.getOpposite()) {
			return false;
		}

		// 扫描到第二个侦测器时避免把同一对重复计数与调度
		if (pos.asLong() > otherPos.asLong()) {
			return false;
		}

		world.getBlockTicks().schedule(ScheduledTick.probe(Blocks.OBSERVER, pos));
		world.getBlockTicks().schedule(ScheduledTick.probe(Blocks.OBSERVER, otherPos));
		return true;
	}
}

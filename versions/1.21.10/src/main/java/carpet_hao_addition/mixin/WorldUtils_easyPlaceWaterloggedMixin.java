package carpet_hao_addition.mixin;

import carpet_hao_addition.EasyPlaceWaterloggedSettings;
import carpet_hao_addition.PlaceWaterloggedHandler;
import carpet_hao_addition.PlaceWaterloggedPayload;

import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * easyPlaceWaterlogged(客户端):告诉服务端「正在轻松放置」以及「哪些位置该放水源」。
 * <p>
 * 两种活儿:
 * <ul>
 *   <li><b>含水方块补水</b>:只发一个活跃标记。位置交给服务端从放置事件里拿 —— 客户端拿不到
 *       可靠坐标(射线命中位置在快速放置时会过期,Litematica 内部位置不对外开放),
 *       而服务端处理放置包时本来就知道方块落在哪。</li>
 *   <li><b>纯水源</b>:水不能作为物品放置,Litematica 遇到投影里的水什么都不会做,不存在
 *       "放置事件"。所以这里在玩家目标点附近扫一小片区域,把「投影里是水源、实际还不是水」
 *       的格子收集起来报给服务端,由服务端逐格放水并扣冰。</li>
 * </ul>
 * 钩子选 {@code WorldUtils.easyPlaceBlockChecksCancel}:它是轻松放置逐个位置都会调用的判定,
 * 只要它在跑就说明玩家正在用轻松放置。
 */
@Mixin(WorldUtils.class)
public abstract class WorldUtils_easyPlaceWaterloggedMixin {
	/** 上次发包所在的客户端 tick,用于每 tick 限一次。 */
	@Unique
	private static long hao$lastSentTick = Long.MIN_VALUE;
	/**
	 * 最近请求过放水的位置 → 请求时的 tick。
	 * <p>
	 * 用来避免"同一片水源被重复放":流体的更新要下一 tick 才生效,而相邻水源之间可能被
	 * 挡水方块隔开(水不会流过去),于是同一 tick 内相邻的另一个水源位置仍然"看起来没水",
	 * 会被再放一次,多出来的水因为流不走就积在原地。
	 */
	@Unique
	private static final java.util.Map<BlockPos, Long> hao$recentWaterRequests = new java.util.HashMap<>();
	/** 位置请求后的保护时长(客户端 tick)。 */
	@Unique
	private static final long HAO_REQUEST_TTL = 40L;

	@Inject(method = "easyPlaceBlockChecksCancel", at = @At("HEAD"))
	private static void hao$sendEasyPlaceState(BlockState desiredState, BlockState currentState,
			PlayerEntity player, HitResult hitResult, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!EasyPlaceWaterloggedSettings.isEnabled()) {
			return;
		}
		if (player == null || !player.getEntityWorld().isClient()) {
			return;
		}
		if (!hao$hasIce(player)) {
			return;
		}
		boolean needsWaterlog = desiredState != null && desiredState.contains(Properties.WATERLOGGED)
				&& Boolean.TRUE.equals(desiredState.get(Properties.WATERLOGGED));
		boolean desiredIsWater = desiredState != null && desiredState.isOf(Blocks.WATER);
		if (!needsWaterlog && !desiredIsWater) {
			return;
		}
		long tick = player.getEntityWorld().getTime();
		if (hao$lastSentTick == tick) {
			return;
		}
		hao$lastSentTick = tick;

		List<BlockPos> waterPositions = hao$collectWaterPositions(player, hitResult);
		PlaceWaterloggedHandler.registerPayloadType();
		ClientPlayNetworking.send(new PlaceWaterloggedPayload(true, waterPositions));
	}

	/**
	 * 取本次操作要放水的那一格。只考虑两个候选:射线命中点所在格,以及命中方块沿命中面的外侧格
	 * (原版放置机制就是"点一个面,放到面外侧"),不扫描周边区域 —— 扫描会把周围本来不该动的位置
	 * 也放上水,属于误放。
	 * <p>
	 * 两个候选都会用<b>投影数据</b>复核:只有投影里确实是水源、且实际位置还不是水的才采用,
	 * 所以候选算错时只会漏放,不会放错地方。
	 */
	@Unique
	private static List<BlockPos> hao$collectWaterPositions(PlayerEntity player, HitResult hitResult) {
		List<BlockPos> result = new ArrayList<>();
		if (hitResult == null) {
			return result;
		}
		WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
		if (schematicWorld == null) {
			return result;
		}
		// 一次操作最多只放一格水:候选按优先级逐个试,命中第一个就停。
		// (早期版本把候选全试一遍,快速放置时两个候选恰好都是水源,就会一次放出两格。)
		BlockPos primary = BlockPos.ofFloored(hitResult.getPos());
		if (hao$shouldPlaceWater(player, schematicWorld, primary)) {
			result.add(primary);
			return result;
		}
		if (hitResult instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
			BlockPos secondary = blockHit.getBlockPos().offset(blockHit.getSide());
			if (!secondary.equals(primary) && hao$shouldPlaceWater(player, schematicWorld, secondary)) {
				result.add(secondary);
			}
		}
		return result;
	}

	/** 该位置是否应当补一格水源:投影里必须是<b>水源</b>,且实际位置还没有水。 */
	@Unique
	private static boolean hao$shouldPlaceWater(PlayerEntity player, WorldSchematic schematicWorld, BlockPos pos) {
		BlockState desired = schematicWorld.getBlockState(pos);
		// 只放水源:投影里的流动水不用管,它们会被相邻水源自然填满,逐个去放既多余又浪费冰。
		if (!desired.isOf(Blocks.WATER) || !desired.getFluidState().isStill()) {
			return false;
		}
		// 实际位置已经和水有关就不必放。
		FluidState fluid = player.getEntityWorld().getFluidState(pos);
		if (!fluid.isEmpty()) {
			return false;
		}
		// 刚请求过就不再重复:流体的更新要下一 tick 才生效,否则相邻水源会被放两次。
		long now = player.getEntityWorld().getTime();
		Long last = hao$recentWaterRequests.get(pos);
		if (last != null && now - last <= HAO_REQUEST_TTL) {
			return false;
		}
		hao$recentWaterRequests.put(pos, now);
		hao$pruneWaterRequests(now);
		return true;
	}

	/** 清理过期的请求记录。 */
	@Unique
	private static void hao$pruneWaterRequests(long now) {
		hao$recentWaterRequests.entrySet().removeIf(e -> now - e.getValue() > HAO_REQUEST_TTL);
	}


	@Unique
	private static boolean hao$hasIce(PlayerEntity player) {
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack s = player.getInventory().getStack(slot);
			if (!s.isEmpty() && s.isOf(Items.ICE)) {
				return true;
			}
		}
		return false;
	}
}

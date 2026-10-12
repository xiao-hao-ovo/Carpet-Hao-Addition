package carpet_hao_addition.mixin.easyplace;

import carpet_hao_addition.easyplace.ProjectionPlacement;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * easy place 鏀鹃摐鐏?1.21.x 鐨?Yarn 鍚嶆槸 {@link CopperBulbBlock})鏃朵笉璁╁畠缈昏浆 lit 鐘舵€併€? * <p>
 * 1.21.x 鐨勬柟娉曞悕涓?26.x 涓嶅悓:{@code onPlace} / {@code neighborChanged} 鍦?Yarn 閲屽垎鍒彨
 * {@code onBlockAdded} / {@code neighborUpdate},鑰屼笖 {@code CopperBulbBlock} 鑷繁**閮芥病瑕嗗啓**
 * (瀹冨彧瑕嗗啓浜?{@code update},26.x 閲屽搴旂殑鏂规硶鍙?{@code checkAndFlip})銆? * {@code @Inject} 鍙鐩爣绫昏嚜宸卞０鏄庣殑鏂规硶,鎵€浠ュ彧娉ㄥ叆 {@code update} 杩欎竴澶勫嵆鍙?鈥斺€? * 鏀剧疆(onBlockAdded)涓庨偦灞呮洿鏂?neighborUpdate)鏈€缁堥兘浼氳蛋瀹冦€? */
@Mixin(value = CopperBulbBlock.class, priority = 900)
public abstract class CopperBulbPlacementMixin {
	@Inject(method = "checkAndFlip", at = @At("HEAD"), cancellable = true)
	private void hao$cancelFlip(BlockState state, ServerLevel world, BlockPos pos, CallbackInfo ci) {
		if (ProjectionPlacement.isProjected()) {
			ci.cancel();
		}
	}
}

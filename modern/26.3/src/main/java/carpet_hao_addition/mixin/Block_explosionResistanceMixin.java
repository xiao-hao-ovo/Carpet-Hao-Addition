package carpet_hao_addition.mixin;

import carpet_hao_addition.WorldEaterProMaxSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes high-resistance blocks destructible like AMS enhancedWorldEater. */
@Mixin(Block.class)
public abstract class Block_explosionResistanceMixin {
	@Inject(method = "getExplosionResistance", at = @At("RETURN"), cancellable = true)
	private void hao$capHighResistance(CallbackInfoReturnable<Float> cir) {
		if (!WorldEaterProMaxSettings.isEnabled() || cir.getReturnValue() < 17.0F) {
			return;
		}
		Block self = (Block) (Object) this;
		if (self == Blocks.BEDROCK || self == Blocks.ANVIL || self == Blocks.CHIPPED_ANVIL
				|| self == Blocks.DAMAGED_ANVIL || self == Blocks.END_PORTAL_FRAME
				|| self == Blocks.END_PORTAL || self == Blocks.END_GATEWAY) {
			return;
		}
		cir.setReturnValue(WorldEaterProMaxSettings.resistanceCap());
	}
}

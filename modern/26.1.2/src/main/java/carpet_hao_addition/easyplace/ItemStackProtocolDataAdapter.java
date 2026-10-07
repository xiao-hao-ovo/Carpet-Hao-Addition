package carpet_hao_addition.easyplace;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ItemStackProtocolDataAdapter {
    int hao$toProtocolValueAddition(ItemStack fromStack);

    @NotNull
    ItemStack hao$fromProtocolValueAddition(int extraProtocolValue, ItemStack fromStack);
}

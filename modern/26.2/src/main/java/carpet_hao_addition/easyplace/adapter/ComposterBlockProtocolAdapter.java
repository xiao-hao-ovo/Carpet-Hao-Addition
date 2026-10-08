package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.BetterEasyPlaceProtocolSettings;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 堆肥桶:协议里带堆肥层数。
 * <p>
 * 层数不是默认还原的 —— 只有 {@code haoBetterEasyPlaceProtocol} 取值为 {@code with_composter_level}
 * 时才编层数;取 {@code true}(或规则关闭)时什么都不编、也不改状态,堆肥桶完全交给原版放置。
 */
public class ComposterBlockProtocolAdapter implements BlockProtocolStateAdapter {
    public static final ComposterBlockProtocolAdapter INSTANCE = new ComposterBlockProtocolAdapter();

    public ComposterBlockProtocolAdapter() {
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        if (!BetterEasyPlaceProtocolSettings.composterLevelEnabled()) {
            return 0; // 不还原层数:什么都不编,服务端就按原版放
        }
        return fromState.getValue(ComposterBlock.LEVEL) & 0b0000_1111;
    }

    @Override
    public @Nullable BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, BlockPlaceContext context) {
        if (!BetterEasyPlaceProtocolSettings.composterLevelEnabled()) {
            return fromState; // 不还原层数:保持原版放置结果
        }
        int level = Math.min(extraProtocolValue & 0b0000_1111, ComposterBlock.MAX_LEVEL);
        return fromState.setValue(ComposterBlock.LEVEL, level);
    }

    @Override
    public @NotNull ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

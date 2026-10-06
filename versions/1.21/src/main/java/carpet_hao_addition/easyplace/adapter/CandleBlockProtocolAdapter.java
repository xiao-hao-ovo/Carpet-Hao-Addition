package carpet_hao_addition.easyplace.adapter;

import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.MultiStageBlockProtocolStateAdapter;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.BlockState;

public class CandleBlockProtocolAdapter implements MultiStageBlockProtocolStateAdapter, BlockProtocolStateAdapter {
    public static final CandleBlockProtocolAdapter INSTANCE = new CandleBlockProtocolAdapter();

    public CandleBlockProtocolAdapter() {
    }

    @Override
    public void hao$setLoopCount(LoopContext ctx) {
        boolean isCandle = ctx.stateClient.getBlock() instanceof CandleBlock;
        int curCandles = isCandle ? ctx.stateClient.get(CandleBlock.CANDLES) : 0;
        int targetCandles = ctx.stateSchematic.get(CandleBlock.CANDLES);

        if (targetCandles > curCandles) {
            ctx.loopCount = targetCandles - curCandles;
        } else {
            ctx.loopCount = 0;
        }
    }

    @Override
    public int hao$toProtocolValueLoop(LoopContext ctx) {
        int candles = (ctx.stateSchematic.get(CandleBlock.CANDLES) - 1) & 0b0011;
        boolean lit = ctx.stateSchematic.get(CandleBlock.LIT);
        int litBit = lit ? 0b0100 : 0b0000;
        return candles | litBit;
    }

    @Override
    public BlockState hao$fromProtocolValue(int extraProtocolValue, BlockState fromState, ItemPlacementContext context) {
        int candles = (extraProtocolValue & 0b0011) + 1;
        boolean lit = ((extraProtocolValue >> 2) & 0b1) == 0b1;
        if (fromState.get(CandleBlock.CANDLES) > candles) {
            return null;
        }
        return fromState.with(CandleBlock.CANDLES, candles).with(CandleBlock.LIT, lit);
    }

    @Override
    public int hao$toProtocolValue(int protocolValue, BlockState fromState) {
        return 0;
    }

    @Override
    public ProtocolType hao$getProtocolType() {
        return ProtocolType.ADDED;
    }
}

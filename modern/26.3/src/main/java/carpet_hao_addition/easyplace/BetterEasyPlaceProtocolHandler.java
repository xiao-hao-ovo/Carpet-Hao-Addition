package carpet_hao_addition.easyplace;

import carpet_hao_addition.BetterEasyPlaceProtocolSettings;
import carpet_hao_addition.easyplace.adapter.*;
import carpet_hao_addition.mixin.easyplace.BeaconBlockEntityAccessor;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.level.block.FlowerBedBlock;

import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.decodeProtocolValueFromHitDim;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.getRelativeHitZ;
import static carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper.isProtocol;

public class BetterEasyPlaceProtocolHandler {
    public static final long EASY_PLACE_RAIL_BLOCK_NO_SHAPE_UPDATE = 1L;
    public static final long EASY_PLACE_PISTON_NO_UPDATE = 1L << 1;
    public static final long EASY_PLACE_PISTON_PLACE_HEAD = 1L << 3;

    private static final Map<Class<? extends Block>, BlockProtocolStateAdapter> ADAPTERS = new HashMap<>();

    static {
        register(BannerBlock.class, new BannerBlockProtocolAdapter());
        register(BeaconBlock.class, new BeaconBlockProtocolAdapter());
        register(BellBlock.class, new BellBlockProtocolAdapter());
        register(CampfireBlock.class, new CampfireBlockProtocolAdapter());
        register(CandleBlock.class, new CandleBlockProtocolAdapter());
        register(CeilingHangingSignBlock.class, new CeilingHangingSignBlockProtocolAdapter());
        register(CommandBlock.class, new CommandBlockProtocolAdapter());
        register(DaylightDetectorBlock.class, new DaylightDetectorBlockProtocolAdapter());
        register(FaceAttachedHorizontalDirectionalBlock.class, new FaceAttachedHorizontalDirectionalBlockProtocolAdapter());
        register(HugeMushroomBlock.class, new HugeMushroomBlockProtocolAdapter());
        register(JigsawBlock.class, new JigsawBlockProtocolAdapter());
        register(LanternBlock.class, new LanternBlockProtocolAdapter());
        register(LeverBlock.class, new LeverBlockProtocolAdapter());
        register(LightBlock.class, new LightBlockProtocolAdapter());
        register(MultifaceBlock.class, new MultifaceBlockProtocolAdapter());
        register(NoteBlock.class, new NoteBlockProtocolAdapter());
        register(RedstoneWireBlock.class, new RedStoneWireBlockProtocolAdapter());
        register(RedstoneLampBlock.class, new RedstoneLampBlockProtocolAdapter());
        register(SeaPickleBlock.class, new SeaPickleBlockProtocolAdapter());
        register(SkullBlock.class, new SkullBlockProtocolAdapter());
        register(SnowLayerBlock.class, new SnowLayerBlockProtocolAdapter());
        register(StairBlock.class, new StairBlockProtocolAdapter());
        register(SignBlock.class, new StandingSignBlockProtocolAdapter());
        register(WallHangingSignBlock.class, new CeilingHangingSignBlockProtocolAdapter());
        register(CeilingHangingSignBlock.class, new  CeilingHangingSignBlockProtocolAdapter());
        register(StructureBlock.class, new StructureBlockProtocolAdapter());
        register(TurtleEggBlock.class, new TurtleEggBlockProtocolAdapter());
        register(VineBlock.class, new VineBlockProtocolAdapter());
        register(CopperBulbBlock.class, new CopperBulbBlockProtocolAdapter());
        register(CrafterBlock.class, new CrafterBlockProtocolAdapter());
        register(FlowerBedBlock.class, new FlowerBedBlockProtocolAdapter());
        register(WallBlock.class, new WallBlockProtocolAdapter());
        // 这 7 类以前只在 26.1.2/26.2 注册过,26.3 与 1.21.x 都漏了 → 铁轨/活塞/中继器/比较器/
        // 漏斗/探测铁轨/堆肥桶的状态全都还原不出来(典型现象:投影里 3 档的中继器放出来变 1 档)。
        register(RailBlock.class, new RailBlockProtocolAdapter());
        register(DetectorRailBlock.class, new DetectorRailBlockProtocolAdapter());
        register(PistonBaseBlock.class, new PistonBaseBlockProtocolAdapter());
        register(RepeaterBlock.class, new RepeaterBlockProtocolAdapter());
        register(ComparatorBlock.class, new ComparatorBlockProtocolAdapter());
        register(HopperBlock.class, new HopperBlockProtocolAdapter());
        register(ComposterBlock.class, new ComposterBlockProtocolAdapter());
    }

    private static boolean easyPlaceState = false;
    private static long placeProperty = 0;
    private static BlockPos placeTargetPos = BlockPos.ZERO;
    private static Block placeTargetBlock = Blocks.AIR;

    private BetterEasyPlaceProtocolHandler() {
    }

    public static boolean isRuleEnabled() {
        return BetterEasyPlaceProtocolSettings.isEnabled();
    }

    public static boolean isEasyPlaceState() {
        return easyPlaceState;
    }

    public static void setEasyPlaceState(boolean value) {
        easyPlaceState = value;
    }

    public static void setPlaceProperty(long val) {
        placeProperty = val;
    }

    public static BlockPos getPlaceTargetPos() {
        return placeTargetPos;
    }

    public static void setPlaceTargetPos(BlockPos pos) {
        placeTargetPos = pos;
    }

    public static Block getPlaceTargetBlock() {
        return placeTargetBlock;
    }

    public static void setPlaceTargetBlock(Block block) {
        placeTargetBlock = block;
    }

    public static boolean hasPlaceFlag(long flag) {
        return (placeProperty & flag) == flag;
    }

    public static void setPlaceFlag(long flag) {
        placeProperty |= flag;
    }

    public static void register(Class<? extends Block> blockClass, BlockProtocolStateAdapter adapter) {
        ADAPTERS.put(blockClass, adapter);
    }

    public static @Nullable BlockProtocolStateAdapter getAdapter(Block block) {
        for (Class<?> cls = block.getClass(); cls != null && cls != Block.class; cls = cls.getSuperclass()) {
            BlockProtocolStateAdapter adapter = ADAPTERS.get(cls);
            if (adapter != null) {
                return adapter;
            }
        }
        return null;
    }

    public static @Nullable BlockState decodePlacementState(Block block, BlockPlaceContext context, @Nullable BlockState baseState) {
        if (!isRuleEnabled()) return null;
        if (baseState == null) {
            baseState = block.getStateForPlacement(context);
            if (baseState == null) {
                baseState = block.defaultBlockState();
            }
        }

        double relativeHitZ = getRelativeHitZ(context.getClickLocation(), context.getClickedPos());
        if (isProtocol(relativeHitZ)) {
            int additionValue = decodeProtocolValueFromHitDim(relativeHitZ);
            BlockProtocolStateAdapter adapter = getAdapter(block);
            if (adapter != null) {
                BlockState applied = adapter.hao$fromProtocolValue(additionValue, baseState, context);
                if (applied != null) {
                    baseState = applied;
                }
            }
            if (baseState.getBlock() instanceof SculkShriekerBlock) {
                baseState = baseState.setValue(SculkShriekerBlock.CAN_SUMMON, false);
            }
            if ((additionValue & EasyPlaceExtraProtocolHelper.WATERLOGGED_BIT) != 0
                    && baseState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && !"false".equals("false")
                    && context.getPlayer() != null
            ) {
                ItemStack itemStack = context.getPlayer().getOffhandItem();
                if (itemStack.is(Items.ICE)) {
                    if ("false".equals("offhand") && !context.getPlayer().getAbilities().instabuild)
                    {
                        itemStack.shrink(1);
                    }
                    baseState = baseState.setValue(BlockStateProperties.WATERLOGGED, true);
                }
            }
        }
        return baseState;
    }

    public static @Nullable BlockState decodeAttachablePlacementState(Block standingBlock, Block wallBlock, BlockPlaceContext context) {
        if (!isRuleEnabled()) return null;

        BlockState baseState = null;
        if (context.getClickedFace().getAxis() != Direction.Axis.Y) {
            baseState = wallBlock.getStateForPlacement(context);
        }
        if (baseState == null) {
            baseState = standingBlock.getStateForPlacement(context);
        }
        if (baseState == null) {
            return null;
        }

        double relativeHitZ = getRelativeHitZ(context.getClickLocation(), context.getClickedPos());
        if (!isProtocol(relativeHitZ)) {
            return baseState;
        }
        int additionValue = decodeProtocolValueFromHitDim(relativeHitZ);
        BlockProtocolStateAdapter adapter = getAdapter(baseState.getBlock());
        if (adapter != null) {
            BlockState applied = adapter.hao$fromProtocolValue(additionValue, baseState, context);
            if (applied != null) {
                baseState = applied;
            }
            if (baseState.getBlock() instanceof SculkShriekerBlock) {
                baseState = baseState.setValue(SculkShriekerBlock.CAN_SUMMON, false);
            }
        }
        if ((additionValue & EasyPlaceExtraProtocolHelper.WATERLOGGED_BIT) != 0
                && baseState.hasProperty(BlockStateProperties.WATERLOGGED)
                && !"false".equals("false")
                && context.getPlayer() != null
        ) {
            ItemStack itemStack = context.getPlayer().getOffhandItem();
            if (itemStack.is(Items.ICE)) {
                if ("false".equals("offhand") && !context.getPlayer().getAbilities().instabuild) {
                    itemStack.shrink(1);
                }
                baseState = baseState.setValue(BlockStateProperties.WATERLOGGED, true);
            }
        }
        return baseState;
    }

    public static @Nullable ItemStack applyItemStackProtocolData(ItemStack stack, BlockPlaceContext context) {
        if (!isRuleEnabled()) return null;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return null;
        BlockProtocolStateAdapter adapter = getAdapter(blockItem.getBlock());
        if (!(adapter instanceof ItemStackProtocolDataAdapter itemStackProtocolDataAdapter)) return null;

        double relativeHitZ = getRelativeHitZ(context.getClickLocation(), context.getClickedPos());
        if (!isProtocol(relativeHitZ)) return null;

        int protocolAdditionValue = decodeProtocolValueFromHitDim(relativeHitZ);
        return itemStackProtocolDataAdapter.hao$fromProtocolValueAddition(protocolAdditionValue, stack);
    }

    public static int encodeSignAttributesFromTag(net.minecraft.nbt.CompoundTag tag) {
        if (tag == null) {
            return 0;
        }
        int bits = 0;
        bits |= encodeSignTextFromTag(tag, "front_text", 0b10_0000, 6);
        bits |= encodeSignTextFromTag(tag, "back_text", 0b1000_0000_0000, 12);
        if (tag.getBoolean("is_waxed").orElse(false)) bits |= 0b100_0000_0000;
        return bits;
    }

    private static int encodeSignTextFromTag(net.minecraft.nbt.CompoundTag tag, String key, int glowingBit, int colorShift) {
        int bits = 0;
        net.minecraft.nbt.CompoundTag text = tag.getCompound(key).orElse(null);
        if (text != null && !text.isEmpty()) {
            if (text.getBoolean("has_glowing_text").orElse(false)) bits |= glowingBit;
            String colorName = text.getString("color").orElse("");
            for (net.minecraft.world.item.DyeColor c : net.minecraft.world.item.DyeColor.values()) {
                if (c.getName().equals(colorName)) {
                    bits |= (c.ordinal() & 0b1111) << colorShift;
                    break;
                }
            }
        }
        return bits;
    }

    public static int encodeBlockEntityProtocolAddition(BlockEntity blockEntity) {
        if (blockEntity instanceof net.minecraft.world.level.block.entity.CrafterBlockEntity crafterBlockEntity) {
            int bits = 0;
            for (int i = 0; i < 9; ++i) {
                if (crafterBlockEntity.isSlotDisabled(i)) {
                    bits |= (1 << i);
                }
            }
            return (bits & 0b0001_1111_1111) << 4;
        }
        if (blockEntity instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity beaconBlockEntity) {
            BeaconBlockEntityAccessor accessor = (BeaconBlockEntityAccessor) beaconBlockEntity;
            return BeaconBlockProtocolAdapter.encodeEffects(accessor.hao$getPrimaryPower(), accessor.hao$getSecondaryPower());
        }
        if (blockEntity instanceof net.minecraft.world.level.block.entity.SignBlockEntity signBlockEntity) {
            int bits = 0;
            net.minecraft.world.level.block.entity.SignText frontText = signBlockEntity.getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT);
            if (frontText.hasGlowingText()) bits |= 0b10_0000;
            bits |= (frontText.getColor().ordinal() & 0b1111) << 6;
            net.minecraft.world.level.block.entity.SignText backText = signBlockEntity.getText(net.minecraft.world.level.block.entity.SignTextSlot.BACK);
            if (backText.hasGlowingText()) bits |= 0b1000_0000_0000;
            bits |= (backText.getColor().ordinal() & 0b1111) << 12;
            if (signBlockEntity.isWaxed()) bits |= 0b100_0000_0000;
            return bits;
        }
        return 0;
    }

    public static int encodeBlockEntityNbtProtocolAddition(@Nullable net.minecraft.nbt.CompoundTag tag) {
        if (tag == null) {
            return 0;
        }
        try {
            if (tag.contains("front_text")) {
                return encodeSignAttributesFromTag(tag);
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}


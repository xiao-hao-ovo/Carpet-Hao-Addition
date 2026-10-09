package carpet_hao_addition.easyplace;

import carpet_hao_addition.BetterEasyPlaceProtocolSettings;
import carpet_hao_addition.easyplace.adapter.*;
import carpet_hao_addition.easyplace.BlockProtocolStateAdapter;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;

import java.util.HashMap;
import java.util.Map;


import carpet_hao_addition.easyplace.EasyPlaceExtraProtocolHelper;
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
        register(CommandBlock.class, new CommandBlockProtocolAdapter());
        register(DaylightDetectorBlock.class, new DaylightDetectorBlockProtocolAdapter());
        register(WallMountedBlock.class, new FaceAttachedHorizontalDirectionalBlockProtocolAdapter());
        register(MushroomBlock.class, new HugeMushroomBlockProtocolAdapter());
        register(JigsawBlock.class, new JigsawBlockProtocolAdapter());
        register(LanternBlock.class, new LanternBlockProtocolAdapter());
        register(LeverBlock.class, new LeverBlockProtocolAdapter());
        register(LightBlock.class, new LightBlockProtocolAdapter());
        register(MultifaceBlock.class, new MultifaceBlockProtocolAdapter());
        register(NoteBlock.class, new NoteBlockProtocolAdapter());
        register(RedstoneWireBlock.class, new RedStoneWireBlockProtocolAdapter());
        register(RedstoneLampBlock.class, new RedstoneLampBlockProtocolAdapter());
        register(SeaPickleBlock.class, new SeaPickleBlockProtocolAdapter());
        // 注意: 只注册 SkullBlock(站立头颅)。墙挂头颅 WallSkullBlock 没有 ROTATION 属性,
        // 若注册在 AbstractSkullBlock 上会在读取属性时抛异常。
        register(SkullBlock.class, new SkullBlockProtocolAdapter());
        register(SnowBlock.class, new SnowLayerBlockProtocolAdapter());
        register(StairsBlock.class, new StairBlockProtocolAdapter());
        register(SignBlock.class, new StandingSignBlockProtocolAdapter());
        // Yarn 1.21.8: SignBlock 是「站立标牌」, WallSignBlock 是「墙挂标牌」,
        // 两者都直接继承 AbstractSignBlock(墙挂不是站立的子类), 所以必须分别注册。
        register(WallSignBlock.class, new StandingSignBlockProtocolAdapter());
        register(WallHangingSignBlock.class, new CeilingHangingSignBlockProtocolAdapter());
        // Yarn 1.21.8: 天花板悬挂标牌叫 HangingSignBlock(没有 CeilingHangingSignBlock)。
        register(HangingSignBlock.class, new CeilingHangingSignBlockProtocolAdapter());
        register(StructureBlock.class, new StructureBlockProtocolAdapter());
        register(TurtleEggBlock.class, new TurtleEggBlockProtocolAdapter());
        register(VineBlock.class, new VineBlockProtocolAdapter());
        register(BulbBlock.class, new CopperBulbBlockProtocolAdapter());
        register(CrafterBlock.class, new CrafterBlockProtocolAdapter());
        register(FlowerbedBlock.class, new FlowerBedBlockProtocolAdapter());
        register(WallBlock.class, new WallBlockProtocolAdapter());
    }

    private static boolean easyPlaceState = false;
    private static long placeProperty = 0;
    private static BlockPos placeTargetPos = BlockPos.ORIGIN;
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

    public static BlockProtocolStateAdapter getAdapter(Block block) {
        for (Class<?> cls = block.getClass(); cls != null && cls != Block.class; cls = cls.getSuperclass()) {
            BlockProtocolStateAdapter adapter = ADAPTERS.get(cls);
            if (adapter != null) {
                return adapter;
            }
        }
        return null;
    }

    public static BlockState decodePlacementState(Block block, ItemPlacementContext context, BlockState baseState) {
        if (!isRuleEnabled()) return null;
        if (baseState == null) {
            baseState = block.getPlacementState(context);
            if (baseState == null) {
                baseState = block.getDefaultState();
            }
        }

        double relativeHitZ = getRelativeHitZ(context.getHitPos(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
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
                baseState = baseState.with(SculkShriekerBlock.CAN_SUMMON, false);
            }
            if ((additionValue & EasyPlaceExtraProtocolHelper.WATERLOGGED_BIT) != 0
                    && baseState.contains(Properties.WATERLOGGED)
                    && false
                    && context.getPlayer() != null
            ) {
                ItemStack itemStack = context.getPlayer().getOffHandStack();
                if (itemStack.isOf(Items.ICE)) {
                    if (false && !context.getPlayer().getAbilities().creativeMode)
                    {
                        itemStack.decrement(1);
                    }
                    baseState = baseState.with(Properties.WATERLOGGED, true);
                }
            }
        }
        return baseState;
    }

    public static BlockState decodeAttachablePlacementState(Block standingBlock, Block wallBlock, ItemPlacementContext context) {
        if (!isRuleEnabled()) return null;

        BlockState baseState = null;
        if (context.getSide().getAxis() != Direction.Axis.Y) {
            baseState = wallBlock.getPlacementState(context);
        }
        if (baseState == null) {
            baseState = standingBlock.getPlacementState(context);
        }
        if (baseState == null) {
            return null;
        }

        double relativeHitZ = getRelativeHitZ(context.getHitPos(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
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
                baseState = baseState.with(SculkShriekerBlock.CAN_SUMMON, false);
            }
        }
        if ((additionValue & EasyPlaceExtraProtocolHelper.WATERLOGGED_BIT) != 0
                && baseState.contains(Properties.WATERLOGGED)
                && false
                && context.getPlayer() != null
        ) {
            ItemStack itemStack = context.getPlayer().getOffHandStack();
            if (itemStack.isOf(Items.ICE)) {
                if (false && !context.getPlayer().getAbilities().creativeMode) {
                    itemStack.decrement(1);
                }
                baseState = baseState.with(Properties.WATERLOGGED, true);
            }
        }
        return baseState;
    }

    public static ItemStack applyItemStackProtocolData(ItemStack stack, ItemPlacementContext context) {
        if (!isRuleEnabled()) return null;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return null;
        BlockProtocolStateAdapter adapter = getAdapter(blockItem.getBlock());
        if (!(adapter instanceof ItemStackProtocolDataAdapter itemStackProtocolDataAdapter)) return null;

        double relativeHitZ = getRelativeHitZ(context.getHitPos(), EasyPlaceExtraProtocolHelper.getClickedPos(context));
        if (!isProtocol(relativeHitZ)) return null;

        int protocolAdditionValue = decodeProtocolValueFromHitDim(relativeHitZ);
        ItemStack restored = itemStackProtocolDataAdapter.hao$fromProtocolValueAddition(protocolAdditionValue, stack);
        System.out.println("[hao-easyplace] 服务端解码 值=" + protocolAdditionValue + " 物品=" + stack.getItem()
                + " 还原后BlockEntityTag=" + (restored.get(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA) != null));
        return restored;
    }

    public static int encodeSignAttributesFromTag(net.minecraft.nbt.NbtCompound tag) {
        if (tag == null) {
            return 0;
        }
        int bits = 0;
        bits |= encodeSignTextFromTag(tag, "front_text", 0b10_0000, 6);
        bits |= encodeSignTextFromTag(tag, "back_text", 0b1000_0000_0000, 12);
        if (tag.getBoolean("is_waxed")) bits |= 0b100_0000_0000;
        return bits;
    }

    private static int encodeSignTextFromTag(net.minecraft.nbt.NbtCompound tag, String key, int glowingBit, int colorShift) {
        int bits = 0;
        net.minecraft.nbt.NbtCompound text = tag.getCompound(key);
        if (text != null && !text.isEmpty()) {
            if (text.getBoolean("has_glowing_text")) bits |= glowingBit;
            String colorName = text.getString("color");
            for (net.minecraft.util.DyeColor c : net.minecraft.util.DyeColor.values()) {
                if (c.getName().equals(colorName)) {
                    bits |= (c.ordinal() & 0b1111) << colorShift;
                    break;
                }
            }
        }
        return bits;
    }

    public static int encodeBlockEntityProtocolAddition(BlockEntity blockEntity) {
        if (blockEntity instanceof net.minecraft.block.entity.CrafterBlockEntity crafterBlockEntity) {
            int bits = 0;
            for (int i = 0; i < 9; ++i) {
                if (crafterBlockEntity.isSlotDisabled(i)) {
                    bits |= (1 << i);
                }
            }
            return (bits & 0b0001_1111_1111) << 4;
        }
        if (blockEntity instanceof net.minecraft.block.entity.BeaconBlockEntity beaconBlockEntity) {
            carpet_hao_addition.mixin.easyplace.BeaconBlockEntityAccessor accessor =
                    (carpet_hao_addition.mixin.easyplace.BeaconBlockEntityAccessor) beaconBlockEntity;
            return BeaconBlockProtocolAdapter.encodeEffects(accessor.hao$getPrimaryPower(), accessor.hao$getSecondaryPower());
        }
        if (blockEntity instanceof net.minecraft.block.entity.SignBlockEntity signBlockEntity) {
            int bits = 0;
            net.minecraft.block.entity.SignText frontText = signBlockEntity.getFrontText();
            if (frontText.isGlowing()) bits |= 0b10_0000;
            bits |= (frontText.getColor().ordinal() & 0b1111) << 6;
            net.minecraft.block.entity.SignText backText = signBlockEntity.getBackText();
            if (backText.isGlowing()) bits |= 0b1000_0000_0000;
            bits |= (backText.getColor().ordinal() & 0b1111) << 12;
            if (signBlockEntity.isWaxed()) bits |= 0b100_0000_0000;
            return bits;
        }
        return 0;
    }

    public static int encodeBlockEntityNbtProtocolAddition(net.minecraft.nbt.NbtCompound tag) {
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

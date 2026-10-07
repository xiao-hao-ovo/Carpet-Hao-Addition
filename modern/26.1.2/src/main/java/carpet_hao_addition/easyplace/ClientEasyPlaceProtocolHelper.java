package carpet_hao_addition.easyplace;

import carpet_hao_addition.EasyPlaceNbtHandler;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ClientEasyPlaceProtocolHelper {

    public static Vec3 encodeHitPosItemData(Vec3 hitPos, BlockPos pos, BlockState stateSchematic) {
        if (!BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
            return hitPos;
        }
        Block block = stateSchematic.getBlock();
        Level world = SchematicWorldHandler.getSchematicWorld();

        // 只对珊瑚类:额外发投影状态给服务端强制设置(珊瑚受 canPlaceAt 约束,不满足会放不下
        // 或回退成地上的扇子)。其他方块不走这里,行为不变。
        if (hao$isCoral(block)) {
            CompoundTag coralNbt = getSchematicBlockEntityNbt(pos, stateSchematic);
            System.out.println("[hao-easyplace] [珊瑚] 发送投影状态=" + stateSchematic);
            EasyPlaceNbtHandler.send(pos, NbtUtils.writeBlockState(stateSchematic), coralNbt);
        }

        carpet_hao_addition.easyplace.BlockProtocolStateAdapter adapter =
                BetterEasyPlaceProtocolHandler.getAdapter(block);
        if (!(adapter instanceof ItemStackProtocolDataAdapter itemStackAdapter)) {
            int added = 0;
            if (adapter != null) {
                added = adapter.hao$toProtocolValue(0, stateSchematic);
                if (block instanceof PistonBaseBlock && stateSchematic.getValue(PistonBaseBlock.EXTENDED)) {
                    Level schematicWorld = SchematicWorldHandler.getSchematicWorld();
                    if (schematicWorld != null) {
                        BlockState headState = schematicWorld.getBlockState(pos.relative(stateSchematic.getValue(PistonBaseBlock.FACING)));
                        if (headState.getBlock() instanceof PistonHeadBlock) {
                            added |= 0b1_0000_0000;
                        }
                    }
                }
            }
            added |= EasyPlaceExtraProtocolHelper.waterloggedBit(stateSchematic);
            if (added == 0) {
                return hitPos;
            }
            return EasyPlaceExtraProtocolHelper.encodeProtocolValueToHitVecZ(added, hitPos);
        }
        int protocolAdditionValue = adapter.hao$toProtocolValue(0, stateSchematic);
        int attributesValue = 0;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            net.minecraft.world.item.ItemStack handStack = mc.player.getMainHandItem();
            if (handStack.isEmpty()) {
                handStack = mc.player.getOffhandItem();
            }
            attributesValue = itemStackAdapter.hao$toProtocolValueAddition(handStack);
        }
        if (attributesValue == 0) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            System.out.println("[hao-easyplace]   投影方块实体=" + (blockEntity == null ? "null" : blockEntity.getClass().getSimpleName()));
            if (blockEntity != null) {
                attributesValue = BetterEasyPlaceProtocolHandler.encodeBlockEntityProtocolAddition(blockEntity);
                System.out.println("[hao-easyplace]   实体编码位=" + attributesValue);
            }
            // 完整 NBT(含标牌文字等)走独立通道单独发送 —— 协议值 16 位装不下这些。
            CompoundTag fullNbt = getSchematicBlockEntityNbt(pos, stateSchematic);
            if (fullNbt != null) {
                EasyPlaceNbtHandler.send(pos, null, fullNbt);
            }
            if (attributesValue == 0) {
                System.out.println("[hao-easyplace]   投影NBT=" + (fullNbt == null ? "null" : fullNbt));
                attributesValue = BetterEasyPlaceProtocolHandler.encodeBlockEntityNbtProtocolAddition(fullNbt);
                System.out.println("[hao-easyplace]   NBT编码位=" + attributesValue);
            }
        }
        System.out.println("[hao-easyplace] 客户端编码 方块=" + block + " 状态位=" + protocolAdditionValue
                + " 方块实体位=" + attributesValue);
        protocolAdditionValue |= attributesValue;
        protocolAdditionValue |= EasyPlaceExtraProtocolHelper.waterloggedBit(stateSchematic);
        if (protocolAdditionValue == 0) {
            return hitPos;
        }
        return EasyPlaceExtraProtocolHelper.encodeProtocolValueToHitVecZ(protocolAdditionValue, hitPos);
    }

    /** 判断一个方块实体 NBT 的 id 是否与目标方块相容。 */
    private static boolean hao$idMatchesState(CompoundTag nbt, BlockState state) {
        String nbtId = nbt.getString("id").orElse("");
        if (nbtId.isEmpty()) {
            return true;
        }
        Identifier id = Identifier.tryParse(nbtId);
        if (id == null) {
            return false;
        }
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id);
        return type != null && type.isValid(state);
    }

    /** 是不是珊瑚类方块(按注册名判定, 活/失活的珊瑚块/扇/墙扇都覆盖)。 */
    private static boolean hao$isCoral(Block block) {
        try {
            return false;   // 强制珊瑚扇已移除:珊瑚按普通方块处理
        } catch (Exception e) {
            return false;
        }
    }

    private static @Nullable CompoundTag getSchematicBlockEntityNbt(BlockPos pos, BlockState stateSchematic) {
        // 投影世界坐标与世界一致,直接查同一坐标即可 —— 坐标换算那条路会偏格,还会抓错方块实体。
        try {
            Level schematicWorld = SchematicWorldHandler.getSchematicWorld();
            if (schematicWorld != null) {
                BlockEntity directBe = schematicWorld.getBlockEntity(pos);
                if (directBe != null) {
                    CompoundTag directNbt = directBe.saveWithFullMetadata(schematicWorld.registryAccess());
                    if (hao$idMatchesState(directNbt, stateSchematic)) {
                        System.out.println("[hao-easyplace]    投影世界直查命中: " + pos
                                + " id=" + directNbt.getString("id").orElse("?"));
                        return directNbt;
                    }
                    System.out.println("[hao-easyplace]    投影世界直查类型不符, 转换算: " + pos
                            + " id=" + directNbt.getString("id").orElse("?")
                            + " 期望方块=" + stateSchematic.getBlock());
                } else {
                    System.out.println("[hao-easyplace]    直查诊断: pos=" + pos
                            + " 投影该处方块=" + schematicWorld.getBlockState(pos)
                            + " 期望方块=" + stateSchematic.getBlock());
                }
            }
        } catch (Exception e) {
            System.out.println("[hao-easyplace]    投影世界直查异常: " + e);
        }
        try {
            List<SchematicPlacementManager.PlacementPart> parts = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingChunk(pos);
            if (parts.isEmpty()) {
                return null;
            }
            for (SchematicPlacementManager.PlacementPart part : parts) {
                SchematicPlacement schematicPlacement = part.getPlacement();
                String regionName = part.getSubRegionName();
                if (schematicPlacement == null || regionName == null) {
                    continue;
                }
                SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);
                if (placement == null || !placement.isEnabled()) {
                    continue;
                }
                LitematicaSchematic schematic = schematicPlacement.getSchematic();
                if (schematic == null) {
                    continue;
                }
                LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);
                Map<BlockPos, CompoundData> blockEntityMap = schematic.getBlockEntityMapForRegion(regionName);
                if (container == null || blockEntityMap == null || blockEntityMap.isEmpty()) {
                    continue;
                }
                BlockPos schematicPos = SchematicUtils.getSchematicContainerPositionFromWorldPosition(
                        pos, schematic, regionName, schematicPlacement, placement, container);
                if (schematicPos != null) {
                    System.out.println("[hao-easyplace]    换算诊断: worldPos=" + pos
                            + " schematicPos=" + schematicPos + " mapKeys=" + blockEntityMap.keySet());
                    CompoundData data = blockEntityMap.get(schematicPos);
                    CompoundTag nbt = data == null ? null : DataConverterNbt.toVanillaCompound(data);
                    if (nbt == null) {
                        // 换算坐标会整体偏移,所以按"类型匹配 + 距离最近"找,不要求 X/Z 相同。
                        int bestDist = 5;
                        BlockPos bestPos = null;
                        for (Map.Entry<BlockPos, CompoundData> en : blockEntityMap.entrySet()) {
                            CompoundTag candidate = DataConverterNbt.toVanillaCompound(en.getValue());
                            if (!hao$idMatchesState(candidate, stateSchematic)) {
                                continue;
                            }
                            BlockPos k = en.getKey();
                            int d = Math.abs(k.getX() - schematicPos.getX())
                                    + Math.abs(k.getY() - schematicPos.getY())
                                    + Math.abs(k.getZ() - schematicPos.getZ());
                            if (d < bestDist) {
                                bestDist = d;
                                bestPos = k;
                                nbt = candidate;
                            }
                        }
                        if (nbt != null) {
                            System.out.println("[hao-easyplace]    类型兜底命中: 目标=" + schematicPos
                                    + " 实际=" + bestPos + " 距离=" + bestDist
                                    + " id=" + nbt.getString("id").orElse("?"));
                        }
                    }
                    if (nbt != null) {
                        return nbt;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[hao-easyplace]     getSchematicBlockEntityNbt 异常: " + e);
            e.printStackTrace();
            return null;
        }
        return null;
    }
}

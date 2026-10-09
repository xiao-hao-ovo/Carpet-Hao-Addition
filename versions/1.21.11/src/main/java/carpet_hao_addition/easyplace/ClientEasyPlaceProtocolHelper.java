package carpet_hao_addition.easyplace;

import carpet_hao_addition.EasyPlaceNbtHandler;
import carpet_hao_addition.easyplace.ItemStackProtocolDataAdapter;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;

public class ClientEasyPlaceProtocolHelper {

    public static Vec3d encodeHitPosItemData(Vec3d hitPos, BlockPos pos, BlockState stateSchematic) {
        if (!BetterEasyPlaceProtocolHandler.isRuleEnabled()) {
            return hitPos;
        }
        Block block = stateSchematic.getBlock();
        World world = SchematicWorldHandler.getSchematicWorld();

        carpet_hao_addition.easyplace.BlockProtocolStateAdapter adapter =
                BetterEasyPlaceProtocolHandler.getAdapter(block);
        if (!(adapter instanceof ItemStackProtocolDataAdapter itemStackAdapter)) {
            int added = 0;
            if (adapter != null) {
                added = adapter.hao$toProtocolValue(0, stateSchematic);
            }
            added |= EasyPlaceExtraProtocolHelper.waterloggedBit(stateSchematic);
            if (added == 0) {
                return hitPos;
            }
            return EasyPlaceExtraProtocolHelper.encodeProtocolValueToHitVecZ(added, hitPos);
        }
        int protocolAdditionValue = adapter.hao$toProtocolValue(0, stateSchematic);
        int attributesValue = 0;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null) {
            net.minecraft.item.ItemStack handStack = mc.player.getMainHandStack();
            if (handStack.isEmpty()) {
                handStack = mc.player.getOffHandStack();
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
            net.minecraft.nbt.NbtCompound fullNbt = getSchematicBlockEntityNbt(pos, stateSchematic);
            if (fullNbt != null) {
                EasyPlaceNbtHandler.send(pos, fullNbt);
            }
            if (attributesValue == 0) {
                net.minecraft.nbt.NbtCompound nbt = fullNbt;
                System.out.println("[hao-easyplace]   投影NBT=" + (nbt == null ? "null" : nbt));
                attributesValue = BetterEasyPlaceProtocolHandler.encodeBlockEntityNbtProtocolAddition(nbt);
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
    private static boolean hao$idMatchesState(NbtCompound nbt, BlockState state) {
        String nbtId = nbt.getString("id").orElse("");
        if (nbtId.isEmpty()) {
            return true;
        }
        net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(nbtId);
        if (id == null) {
            return false;
        }
        net.minecraft.block.entity.BlockEntityType<?> type =
                net.minecraft.registry.Registries.BLOCK_ENTITY_TYPE.get(id);
        return type != null && type.supports(state);
    }

    private static NbtCompound getSchematicBlockEntityNbt(BlockPos pos, BlockState stateSchematic) {
        // 投影世界坐标与世界一致,直接查同一坐标即可 —— 坐标换算那条路会偏格,还会抓错方块实体。
        try {
            net.minecraft.world.World schematicWorld = SchematicWorldHandler.getSchematicWorld();
            if (schematicWorld != null) {
                BlockEntity directBe = schematicWorld.getBlockEntity(pos);
                if (directBe != null) {
                    NbtCompound directNbt = directBe.createNbtWithIdentifyingData(schematicWorld.getRegistryManager());
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
                // litematica 0.26(1.21.11 起)这里的值是 malilib 自研的 CompoundData, 需转换。
                Map<BlockPos, fi.dy.masa.malilib.util.data.tag.CompoundData> blockEntityMap =
                        schematic.getBlockEntityMapForRegion(regionName);
                if (container == null || blockEntityMap == null || blockEntityMap.isEmpty()) {
                    continue;
                }
                BlockPos schematicPos = SchematicUtils.getSchematicContainerPositionFromWorldPosition(
                        pos, schematic, regionName, schematicPlacement, placement, container);
                if (schematicPos != null) {
                    System.out.println("[hao-easyplace]    换算诊断: worldPos=" + pos
                            + " schematicPos=" + schematicPos + " mapKeys=" + blockEntityMap.keySet());
                    fi.dy.masa.malilib.util.data.tag.CompoundData rawNbt = blockEntityMap.get(schematicPos);
                    NbtCompound nbt = rawNbt == null ? null : carpet_hao_addition.MalilibNbtConverter.toVanilla(rawNbt);
                    if (nbt == null) {
                        // 换算坐标会整体偏移,所以按"类型匹配 + 距离最近"找,不要求 X/Z 相同。
                        int bestDist = 5;
                        BlockPos bestPos = null;
                        for (Map.Entry<BlockPos, fi.dy.masa.malilib.util.data.tag.CompoundData> en : blockEntityMap.entrySet()) {
                            NbtCompound candidate = carpet_hao_addition.MalilibNbtConverter.toVanilla(en.getValue());
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

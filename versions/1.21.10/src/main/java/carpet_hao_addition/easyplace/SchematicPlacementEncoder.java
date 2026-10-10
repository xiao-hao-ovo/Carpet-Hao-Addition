package carpet_hao_addition.easyplace;

import carpet_hao_addition.HaoDebug;

import carpet_hao_addition.BlockDataChannel;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;

public class SchematicPlacementEncoder {

    public static Vec3d encodeSchematicItemData(Vec3d hitPos, BlockPos pos, BlockState stateSchematic) {
        if (!ProjectionPlacement.enabled()) {
            return hitPos;
        }
        Block block = stateSchematic.getBlock();

        PlacementCodec codec = PlacementRules.codec(block);
        int bits = codec == null ? 0 : codec.pack(stateSchematic, 0);

        ItemDataCodec itemCodec = PlacementRules.itemData(block);
        if (itemCodec != null) {
            bits |= encodeItemData(itemCodec, pos, stateSchematic);
        }
        HaoDebug.log("[hao-easyplace] 客户端编码 方块=" + block + " 协议位=" + bits);
        return bits == 0 ? hitPos : PlacementBitTools.bitsToHitVec(bits, hitPos);
    }

    /**
     * 物品数据位的来源按可靠性排了三层：手上拿着的东西 → 投影世界的方块实体 →
     * 投影文件里的方块实体 NBT。取到就停。
     */
    private static int encodeItemData(ItemDataCodec codec, BlockPos pos, BlockState stateSchematic) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            ItemStack handStack = mc.player.getMainHandStack();
            if (handStack.isEmpty()) {
                handStack = mc.player.getOffHandStack();
            }
            int fromHand = codec.encodeStack(handStack);
            if (fromHand != 0) {
                return fromHand;
            }
        }

        World world = SchematicWorldHandler.getSchematicWorld();
        BlockEntity blockEntity = world == null ? null : world.getBlockEntity(pos);
        HaoDebug.log("[hao-easyplace]   投影方块实体=" + (blockEntity == null ? "null" : blockEntity.getClass().getSimpleName()));
        if (blockEntity != null) {
            int fromEntity = ProjectionPlacement.encodeBlockEntityBits(blockEntity);
            HaoDebug.log("[hao-easyplace]   实体编码位=" + fromEntity);
            if (fromEntity != 0) {
                return fromEntity;
            }
        }

        // 完整 NBT(含标牌文字等)走独立通道单独发送 —— 协议值装不下这些。
        NbtCompound fullNbt = schematicBlockEntityNbt(pos, stateSchematic);
        if (fullNbt != null) {
            BlockDataChannel.sendPacket(pos, fullNbt);
        }
        HaoDebug.log("[hao-easyplace]   投影NBT=" + (fullNbt == null ? "null" : fullNbt));
        int fromNbt = codec.encodeNbt(fullNbt);
        HaoDebug.log("[hao-easyplace]   NBT编码位=" + fromNbt);
        return fromNbt;
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

    private static NbtCompound schematicBlockEntityNbt(BlockPos pos, BlockState stateSchematic) {
        // 投影世界坐标与世界一致,直接查同一坐标即可 —— 坐标换算那条路会偏格,还会抓错方块实体。
        try {
            net.minecraft.world.World schematicWorld = SchematicWorldHandler.getSchematicWorld();
            if (schematicWorld != null) {
                BlockEntity directBe = schematicWorld.getBlockEntity(pos);
                if (directBe != null) {
                    NbtCompound directNbt = directBe.createNbtWithIdentifyingData(schematicWorld.getRegistryManager());
                    if (hao$idMatchesState(directNbt, stateSchematic)) {
                        HaoDebug.log("[hao-easyplace]    投影世界直查命中: " + pos
                                + " id=" + directNbt.getString("id").orElse("?"));
                        return directNbt;
                    }
                    HaoDebug.log("[hao-easyplace]    投影世界直查类型不符, 转换算: " + pos
                            + " id=" + directNbt.getString("id").orElse("?")
                            + " 期望方块=" + stateSchematic.getBlock());
                } else {
                    HaoDebug.log("[hao-easyplace]    直查诊断: pos=" + pos
                            + " 投影该处方块=" + schematicWorld.getBlockState(pos)
                            + " 期望方块=" + stateSchematic.getBlock());
                }
            }
        } catch (Exception e) {
            HaoDebug.log("[hao-easyplace]    投影世界直查异常: " + e);
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
                Map<BlockPos, NbtCompound> blockEntityMap = schematic.getBlockEntityMapForRegion(regionName);
                if (container == null || blockEntityMap == null || blockEntityMap.isEmpty()) {
                    continue;
                }
                BlockPos schematicPos = SchematicUtils.getSchematicContainerPositionFromWorldPosition(
                        pos, schematic, regionName, schematicPlacement, placement, container);
                if (schematicPos != null) {
                    HaoDebug.log("[hao-easyplace]    换算诊断: worldPos=" + pos
                            + " schematicPos=" + schematicPos + " mapKeys=" + blockEntityMap.keySet());
                    NbtCompound nbt = blockEntityMap.get(schematicPos);
                    if (nbt == null) {
                        // 换算坐标会整体偏移,所以按"类型匹配 + 距离最近"找,不要求 X/Z 相同。
                        int bestDist = 5;
                        BlockPos bestPos = null;
                        for (Map.Entry<BlockPos, NbtCompound> en : blockEntityMap.entrySet()) {
                            if (!hao$idMatchesState(en.getValue(), stateSchematic)) {
                                continue;
                            }
                            BlockPos k = en.getKey();
                            int d = Math.abs(k.getX() - schematicPos.getX())
                                    + Math.abs(k.getY() - schematicPos.getY())
                                    + Math.abs(k.getZ() - schematicPos.getZ());
                            if (d < bestDist) {
                                bestDist = d;
                                bestPos = k;
                                nbt = en.getValue();
                            }
                        }
                        if (nbt != null) {
                            HaoDebug.log("[hao-easyplace]    类型兜底命中: 目标=" + schematicPos
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
            HaoDebug.log("[hao-easyplace]     schematicBlockEntityNbt 异常: " + e);
            HaoDebug.stackTrace(e);
            return null;
        }
        return null;
    }
}

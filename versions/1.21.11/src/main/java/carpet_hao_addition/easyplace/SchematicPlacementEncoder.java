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
        if (!ProjectionPlacement.ruleEnabled()) {
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
                // 告示牌的颜色/发光在投影世界的 SignBlockEntity 上经常是默认值（只还原了文字），
                // 用它编码会把投影文件里的颜色覆盖成黑色。这类方块跳过 ①，直接走投影文件 NBT。
                boolean preferSchematicNbt = codec instanceof SignItemData;
                // ① 投影世界的方块实体
                World world = SchematicWorldHandler.getSchematicWorld();
                BlockEntity blockEntity = world == null ? null : world.getBlockEntity(pos);
                if (HaoDebug.ENABLED) {
                        HaoDebug.log("[hao-easyplace]   投影方块实体=" + (blockEntity == null ? "null" : blockEntity.getClass().getSimpleName()));
                }
                if (!preferSchematicNbt && blockEntity != null) {
                        int fromEntity = ProjectionPlacement.encodeBlockEntityBits(blockEntity);
                        if (HaoDebug.ENABLED) {
                                HaoDebug.log("[hao-easyplace]   实体编码位=" + fromEntity);
                        }
                        if (fromEntity != 0) {
                                return fromEntity;
                        }
                }

                // ② 投影文件里的完整 NBT
                NbtCompound fullNbt = schematicBlockEntityNbt(pos, stateSchematic);
                if (codec instanceof SignItemData signCodec) {
                        // 投影文件可能是 1.19 及以前导出的（Text1..Text4 + Color），服务端 1.21.8
                        // 只认 1.20+ 的 front_text/back_text；不转的话颜色/文字都会退回默认。
                        fullNbt = signCodec.normalizeSchematicNbt(fullNbt);
                }
                int fromNbt = codec.encodeNbt(fullNbt);
                if (HaoDebug.ENABLED) {
                        HaoDebug.log("[hao-easyplace]   投影NBT=" + (fullNbt == null ? "null" : fullNbt)
                                        + " NBT编码位=" + fromNbt);
                }
                // 只有协议位装不下的内容（标牌文字等）才走「完整 NBT 通道」。
                // 合成器/信标这种协议位已经完整的，发了包反而有害：服务端那条通道按位置
                // 缓存再取，密集放置时相邻位置的数据会互相串，表现就是「个别几个合成器没锁格子」。
                if (fullNbt != null && !codec.protocolCoversAll()) {
                        BlockDataChannel.dispatchPacket(pos, fullNbt);
                }
                if (fromNbt != 0) {
                        return fromNbt;
                }

                // ③ 手持物品兜底 —— 但仅当投影里根本没有这个位置的方块实体时。
                // 投影有这个方块实体、两层都没给出位，说明投影就是"没有额外数据"，
                // 这时拿手上的旧 NBT 顶上去只会把不同位置的状态抹成同一份。
                if (blockEntity != null) {
                        return 0;
                }
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                        ItemStack handStack = mc.player.getMainHandStack();
                        if (handStack.isEmpty()) {
                                handStack = mc.player.getOffHandStack();
                        }
                        return codec.encodeStack(handStack);
                }
                return 0;
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
        boolean preferSchematicNbt = PlacementRules.itemData(stateSchematic.getBlock()) instanceof SignItemData;
        if (!preferSchematicNbt) {
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
                // litematica 0.26(1.21.11 起) 这里给的是 malilib 自研的 CompoundData, 先整体转成原版 NBT。
                Map<BlockPos, NbtCompound> blockEntityMap = new java.util.HashMap<>();
                java.util.Map<BlockPos, fi.dy.masa.malilib.util.data.tag.CompoundData> hao$rawMap =
                        schematic.getBlockEntityMapForRegion(regionName);
                if (hao$rawMap != null) {
                    for (java.util.Map.Entry<BlockPos, fi.dy.masa.malilib.util.data.tag.CompoundData> hao$en
                            : hao$rawMap.entrySet()) {
                        NbtCompound hao$conv = carpet_hao_addition.MalilibNbtConverter.toVanilla(hao$en.getValue());
                        if (hao$conv != null) {
                            blockEntityMap.put(hao$en.getKey(), hao$conv);
                        }
                    }
                }
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

# 各版本文件清单

> **本文件由 `tools/gen_versions_doc.py` 从源码树自动生成，请勿手改。**
> 包结构或文件行数变了，跑一次 `python tools/gen_versions_doc.py` 即可同步。
> 行数口径：`len(text.split("\n"))`，即文件以换行结尾时会比 `wc -l` 多 1。

## 一、仓库结构总览

```
Carpet-Hao-Addition/
├─ src/main/java/            共享源码 27 个 java（所有版本都用）
├─ src/main/resources/       共享资源（lang 必须放这里）
├─ versions/                 1.21.x 各层 8 个：1.21, 1.21.1, 1.21.10, 1.21.11, 1.21.2, 1.21.4, 1.21.6, 1.21.8
│    └─ <版本>/src/main/java/   该层的完整实现
├─ modern/                   26.x 各层 3 个：26.1.2, 26.2, 26.3（独立构建）
├─ docs/                     文档
└─ tools/                    构建与发布脚本
```

### 构建模型：共享 + 覆盖

`versions/<版本>/build.gradle` 把 **两处** 源码合到一起编译：

```groovy
sourceSets.main.java.srcDirs = [
    rootProject.file('src/main/java'),   // 共享：规则设置、命令入口
    file('src/main/java')                // 本层：完整实现
]
duplicatesStrategy = EXCLUDE             // 同名文件整份覆盖
```

于是只有两种文件：

| 类型 | 放在哪 | 说明 |
|---|---|---|
| **共享** | `src/main/java` | 规则设置类、命令、跨层工具。所有版本共用一份 |
| **本层** | `versions/<版本>/src/main/java` | 各版本 API 不同，必须逐层实现 |

> ⚠️ `lang` 等资源**必须**放共享的 `src/main/resources`。放层内不会按 key 合并，
> 而是**整份覆盖**，会丢掉共享里其他 key。

## 二、共享源码（`src/main/java`，27 个）

**根包 · 规则设置与跨层实现**（23 个，1542 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingSettings.java` | 32 |
| `BedrockCanBeMinedSettings.java` | 29 |
| `CarpetHaoAdditionExtension.java` | 323 |
| `CopperStonecuttingSettings.java` | 31 |
| `CraftableTrimTemplateSettings.java` | 31 |
| `DirectDropsSettings.java` | 54 |
| `EasyPlaceEntityCommands.java` | 126 |
| `EasyPlaceEntitySettings.java` | 137 |
| `EasyPlaceWaterloggedSettings.java` | 116 |
| `EntityPlacementTrigger.java` | 82 |
| `GoldenCarrotCompostSettings.java` | 30 |
| `HaoDebug.java` | 56 |
| `LavaDepthStriderSettings.java` | 30 |
| `NoEndPortalTeleportSettings.java` | 30 |
| `ProjectionPlacementSettings.java` | 53 |
| `RocketShulkerCommands.java` | 96 |
| `RocketShulkerSettings.java` | 92 |
| `SnowyCalciteSettings.java` | 29 |
| `TerracottaUncolorSettings.java` | 28 |
| `UseDyeOnShulkerBoxSettings.java` | 32 |
| `WackoBeaconsSettings.java` | 37 |
| `WitherSkeletonDropReductionSettings.java` | 41 |
| `WorldEaterProMaxSettings.java` | 27 |

**Mixin · 其余规则**（2 个，111 行）

| 文件 | 行数 |
|---|---:|
| `WackoBeacons_BeaconBlockEntityMixin.java` | 68 |
| `WackoBeacons_BeaconButtonWidgetMixin.java` | 43 |

**区域守卫 · 区域与指令**（1 个，41 行）

| 文件 | 行数 |
|---|---:|
| `ZoneguardSettings.java` | 41 |

**末地门 · 传送名单**（1 个，69 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportList.java` | 69 |

## 三、1.21.x 各层

每层都是**完整实现**，不依赖增量补丁——改功能要逐层改。

| 版本 | 本层 java 数 |
|---|---:|
| `1.21` | 71 |
| `1.21.1` | 71 |
| `1.21.10` | 72 |
| `1.21.11` | 73 |
| `1.21.2` | 71 |
| `1.21.4` | 71 |
| `1.21.6` | 71 |
| `1.21.8` | 75 |

### 各层文件明细

<details>
<summary><b>1.21</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**（12 个，1559 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 88 |
| `BlockDataChannel.java` | 123 |
| `BlockDataPayload.java` | 30 |
| `EntityPlacementHandler.java` | 357 |
| `EntityPlacementPayload.java` | 46 |
| `EntitySpawn.java` | 43 |
| `RecipeDeployHooks.java` | 115 |
| `RocketShulkerHandler.java` | 139 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |
| `WaterloggedFillHandler.java` | 442 |
| `WaterloggedFillPayload.java` | 66 |

**Mixin · 其余规则**（27 个，1910 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 51 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `ClientPlayerInteractionManager_bedrockMinesMixin.java` | 50 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 63 |
| `EndPortalTeleportMixin.java` | 44 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 43 |
| `LivingEntity_lavaDepthStriderMixin.java` | 52 |
| `LivingEntity_witherSkeletonDropMixin.java` | 53 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 25 |
| `MobEntity_witherSkeletonDropMixin.java` | 37 |
| `ObserverBlockMixin.java` | 82 |
| `PlayerEntity_directEntityDropsMixin.java` | 41 |
| `ServerPlayerEntity_autoMendingMixin.java` | 26 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayerInteractionManager_directBlockDropsMixin.java` | 41 |
| `ServerWorld_directDropsMixin.java` | 43 |
| `ServerWorld_tickBlockMixin.java` | 42 |
| `StonecutterScreenHandlerMixin.java` | 57 |
| `WorldAccess_directDropsMixin.java` | 65 |
| `WorldUtils_entityPlacementMixin.java` | 406 |
| `WorldUtils_waterloggedFillMixin.java` | 314 |

**轻松放置 · Mixin**（8 个，421 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 58 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 31 |
| `ProjectionBlockItemMixin.java` | 145 |
| `SchematicHitVecMixin.java` | 115 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `SignPlacementMixin.java` | 29 |

**轻松放置 · 协议与规则编解码**（11 个，1855 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 94 |
| `CrafterItemData.java` | 89 |
| `ItemDataCodec.java` | 39 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 553 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 192 |
| `SignItemData.java` | 145 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，755 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 151 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 503 |

**区域守卫 · 区域与指令**（7 个，677 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 229 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 138 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（1 个，151 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

</details>

<details>
<summary><b>1.21.1</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**（12 个，1559 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 88 |
| `BlockDataChannel.java` | 123 |
| `BlockDataPayload.java` | 30 |
| `EntityPlacementHandler.java` | 357 |
| `EntityPlacementPayload.java` | 46 |
| `EntitySpawn.java` | 43 |
| `RecipeDeployHooks.java` | 115 |
| `RocketShulkerHandler.java` | 139 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |
| `WaterloggedFillHandler.java` | 442 |
| `WaterloggedFillPayload.java` | 66 |

**Mixin · 其余规则**（27 个，1910 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 51 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `ClientPlayerInteractionManager_bedrockMinesMixin.java` | 50 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 63 |
| `EndPortalTeleportMixin.java` | 44 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 43 |
| `LivingEntity_lavaDepthStriderMixin.java` | 52 |
| `LivingEntity_witherSkeletonDropMixin.java` | 53 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 25 |
| `MobEntity_witherSkeletonDropMixin.java` | 37 |
| `ObserverBlockMixin.java` | 82 |
| `PlayerEntity_directEntityDropsMixin.java` | 41 |
| `ServerPlayerEntity_autoMendingMixin.java` | 26 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayerInteractionManager_directBlockDropsMixin.java` | 41 |
| `ServerWorld_directDropsMixin.java` | 43 |
| `ServerWorld_tickBlockMixin.java` | 42 |
| `StonecutterScreenHandlerMixin.java` | 57 |
| `WorldAccess_directDropsMixin.java` | 65 |
| `WorldUtils_entityPlacementMixin.java` | 406 |
| `WorldUtils_waterloggedFillMixin.java` | 314 |

**轻松放置 · Mixin**（8 个，421 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 58 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 31 |
| `ProjectionBlockItemMixin.java` | 145 |
| `SchematicHitVecMixin.java` | 115 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `SignPlacementMixin.java` | 29 |

**轻松放置 · 协议与规则编解码**（11 个，1855 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 94 |
| `CrafterItemData.java` | 89 |
| `ItemDataCodec.java` | 39 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 553 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 192 |
| `SignItemData.java` | 145 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，755 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 151 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 503 |

**区域守卫 · 区域与指令**（7 个，677 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 229 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 138 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（1 个，151 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

</details>

<details>
<summary><b>1.21.10</b>（72 个 java）</summary>

**根包 · 规则设置与跨层实现**（13 个，1886 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 88 |
| `BlockDataChannel.java` | 123 |
| `BlockDataPayload.java` | 30 |
| `CopperStonecuttingDeployHook.java` | 301 |
| `EntityPlacementHandler.java` | 383 |
| `EntityPlacementPayload.java` | 46 |
| `EntitySpawn.java` | 43 |
| `RecipeDeployHooks.java` | 115 |
| `RocketShulkerHandler.java` | 139 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |
| `WaterloggedFillHandler.java` | 442 |
| `WaterloggedFillPayload.java` | 66 |

**Mixin · 其余规则**（27 个，1912 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 51 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `ClientPlayerInteractionManager_bedrockMinesMixin.java` | 50 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 63 |
| `EndPortalTeleportMixin.java` | 45 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 44 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 54 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 27 |
| `MobEntity_witherSkeletonDropMixin.java` | 37 |
| `ObserverBlockMixin.java` | 83 |
| `PlayerEntity_directEntityDropsMixin.java` | 41 |
| `ScheduledTickView_directDropsMixin.java` | 65 |
| `ServerPlayerEntity_autoMendingMixin.java` | 26 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayerInteractionManager_directBlockDropsMixin.java` | 41 |
| `ServerWorld_directDropsMixin.java` | 43 |
| `ServerWorld_tickBlockMixin.java` | 42 |
| `StonecutterScreenHandlerMixin.java` | 57 |
| `WorldUtils_entityPlacementMixin.java` | 400 |
| `WorldUtils_waterloggedFillMixin.java` | 314 |

**轻松放置 · Mixin**（8 个，424 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 58 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 31 |
| `ProjectionBlockItemMixin.java` | 148 |
| `SchematicHitVecMixin.java` | 115 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `SignPlacementMixin.java` | 29 |

**轻松放置 · 协议与规则编解码**（11 个，1891 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 92 |
| `CrafterItemData.java` | 87 |
| `ItemDataCodec.java` | 52 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 582 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 192 |
| `SignItemData.java` | 143 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，775 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 505 |

**区域守卫 · 区域与指令**（7 个，661 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 227 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 60 |
| `ZoneguardSavedData.java` | 125 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（1 个，151 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

</details>

<details>
<summary><b>1.21.11</b>（73 个 java）</summary>

**根包 · 规则设置与跨层实现**（14 个，1994 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 88 |
| `BlockDataChannel.java` | 123 |
| `BlockDataPayload.java` | 30 |
| `CopperStonecuttingDeployHook.java` | 301 |
| `EntityPlacementHandler.java` | 383 |
| `EntityPlacementPayload.java` | 46 |
| `EntitySpawn.java` | 43 |
| `MalilibNbtConverter.java` | 109 |
| `RecipeDeployHooks.java` | 114 |
| `RocketShulkerHandler.java` | 139 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |
| `WaterloggedFillHandler.java` | 442 |
| `WaterloggedFillPayload.java` | 66 |

**Mixin · 其余规则**（27 个，1915 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 51 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `ClientPlayerInteractionManager_bedrockMinesMixin.java` | 50 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 63 |
| `EndPortalTeleportMixin.java` | 45 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 44 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 54 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 27 |
| `MobEntity_witherSkeletonDropMixin.java` | 37 |
| `ObserverBlockMixin.java` | 83 |
| `PlayerEntity_directEntityDropsMixin.java` | 41 |
| `ScheduledTickView_directDropsMixin.java` | 65 |
| `ServerPlayerEntity_autoMendingMixin.java` | 26 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayerInteractionManager_directBlockDropsMixin.java` | 41 |
| `ServerWorld_directDropsMixin.java` | 43 |
| `ServerWorld_tickBlockMixin.java` | 42 |
| `StonecutterScreenHandlerMixin.java` | 57 |
| `WorldUtils_entityPlacementMixin.java` | 403 |
| `WorldUtils_waterloggedFillMixin.java` | 314 |

**轻松放置 · Mixin**（8 个，424 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 58 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 31 |
| `ProjectionBlockItemMixin.java` | 148 |
| `SchematicHitVecMixin.java` | 115 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `SignPlacementMixin.java` | 29 |

**轻松放置 · 协议与规则编解码**（11 个，1903 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 92 |
| `CrafterItemData.java` | 87 |
| `ItemDataCodec.java` | 52 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 582 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 204 |
| `SignItemData.java` | 143 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，775 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 505 |

**区域守卫 · 区域与指令**（7 个，662 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 227 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 125 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（1 个，152 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 152 |

</details>

<details>
<summary><b>1.21.2</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**（12 个，1577 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 88 |
| `BlockDataChannel.java` | 123 |
| `BlockDataPayload.java` | 30 |
| `EntityPlacementHandler.java` | 375 |
| `EntityPlacementPayload.java` | 46 |
| `EntitySpawn.java` | 43 |
| `RecipeDeployHooks.java` | 115 |
| `RocketShulkerHandler.java` | 139 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |
| `WaterloggedFillHandler.java` | 442 |
| `WaterloggedFillPayload.java` | 66 |

**Mixin · 其余规则**（27 个，1914 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 51 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `ClientPlayerInteractionManager_bedrockMinesMixin.java` | 50 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 63 |
| `EndPortalTeleportMixin.java` | 44 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 44 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 53 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 25 |
| `MobEntity_witherSkeletonDropMixin.java` | 37 |
| `ObserverBlockMixin.java` | 83 |
| `PlayerEntity_directEntityDropsMixin.java` | 41 |
| `ScheduledTickView_directDropsMixin.java` | 65 |
| `ServerPlayerEntity_autoMendingMixin.java` | 26 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayerInteractionManager_directBlockDropsMixin.java` | 41 |
| `ServerWorld_directDropsMixin.java` | 43 |
| `ServerWorld_tickBlockMixin.java` | 42 |
| `StonecutterScreenHandlerMixin.java` | 57 |
| `WorldUtils_entityPlacementMixin.java` | 406 |
| `WorldUtils_waterloggedFillMixin.java` | 314 |

**轻松放置 · Mixin**（8 个，421 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 58 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 31 |
| `ProjectionBlockItemMixin.java` | 145 |
| `SchematicHitVecMixin.java` | 115 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `SignPlacementMixin.java` | 29 |

**轻松放置 · 协议与规则编解码**（11 个，1855 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 94 |
| `CrafterItemData.java` | 89 |
| `ItemDataCodec.java` | 39 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 553 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 192 |
| `SignItemData.java` | 145 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，774 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 504 |

**区域守卫 · 区域与指令**（7 个，677 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 229 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 138 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（1 个，151 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

</details>

<details>
<summary><b>1.21.4</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**（12 个，1577 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 88 |
| `BlockDataChannel.java` | 123 |
| `BlockDataPayload.java` | 30 |
| `EntityPlacementHandler.java` | 375 |
| `EntityPlacementPayload.java` | 46 |
| `EntitySpawn.java` | 43 |
| `RecipeDeployHooks.java` | 115 |
| `RocketShulkerHandler.java` | 139 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |
| `WaterloggedFillHandler.java` | 442 |
| `WaterloggedFillPayload.java` | 66 |

**Mixin · 其余规则**（27 个，1914 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 51 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `ClientPlayerInteractionManager_bedrockMinesMixin.java` | 50 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 63 |
| `EndPortalTeleportMixin.java` | 44 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 44 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 53 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 25 |
| `MobEntity_witherSkeletonDropMixin.java` | 37 |
| `ObserverBlockMixin.java` | 83 |
| `PlayerEntity_directEntityDropsMixin.java` | 41 |
| `ScheduledTickView_directDropsMixin.java` | 65 |
| `ServerPlayerEntity_autoMendingMixin.java` | 26 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayerInteractionManager_directBlockDropsMixin.java` | 41 |
| `ServerWorld_directDropsMixin.java` | 43 |
| `ServerWorld_tickBlockMixin.java` | 42 |
| `StonecutterScreenHandlerMixin.java` | 57 |
| `WorldUtils_entityPlacementMixin.java` | 406 |
| `WorldUtils_waterloggedFillMixin.java` | 314 |

**轻松放置 · Mixin**（8 个，421 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 58 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 31 |
| `ProjectionBlockItemMixin.java` | 145 |
| `SchematicHitVecMixin.java` | 115 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `SignPlacementMixin.java` | 29 |

**轻松放置 · 协议与规则编解码**（11 个，1884 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 94 |
| `CrafterItemData.java` | 89 |
| `ItemDataCodec.java` | 39 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 582 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 192 |
| `SignItemData.java` | 145 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，774 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 504 |

**区域守卫 · 区域与指令**（7 个，677 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 229 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 138 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（1 个，151 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

</details>

<details>
<summary><b>1.21.6</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**（12 个，1577 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 88 |
| `BlockDataChannel.java` | 123 |
| `BlockDataPayload.java` | 30 |
| `EntityPlacementHandler.java` | 375 |
| `EntityPlacementPayload.java` | 46 |
| `EntitySpawn.java` | 43 |
| `RecipeDeployHooks.java` | 115 |
| `RocketShulkerHandler.java` | 139 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |
| `WaterloggedFillHandler.java` | 442 |
| `WaterloggedFillPayload.java` | 66 |

**Mixin · 其余规则**（27 个，1916 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 51 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `ClientPlayerInteractionManager_bedrockMinesMixin.java` | 50 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 63 |
| `EndPortalTeleportMixin.java` | 45 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 44 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 53 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 25 |
| `MobEntity_witherSkeletonDropMixin.java` | 37 |
| `ObserverBlockMixin.java` | 83 |
| `PlayerEntity_directEntityDropsMixin.java` | 41 |
| `ScheduledTickView_directDropsMixin.java` | 65 |
| `ServerPlayerEntity_autoMendingMixin.java` | 26 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayerInteractionManager_directBlockDropsMixin.java` | 41 |
| `ServerWorld_directDropsMixin.java` | 43 |
| `ServerWorld_tickBlockMixin.java` | 42 |
| `StonecutterScreenHandlerMixin.java` | 57 |
| `WorldUtils_entityPlacementMixin.java` | 407 |
| `WorldUtils_waterloggedFillMixin.java` | 314 |

**轻松放置 · Mixin**（8 个，421 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 58 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 31 |
| `ProjectionBlockItemMixin.java` | 145 |
| `SchematicHitVecMixin.java` | 115 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `SignPlacementMixin.java` | 29 |

**轻松放置 · 协议与规则编解码**（11 个，1884 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 94 |
| `CrafterItemData.java` | 89 |
| `ItemDataCodec.java` | 39 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 582 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 192 |
| `SignItemData.java` | 145 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，774 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 504 |

**区域守卫 · 区域与指令**（7 个，663 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 229 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 124 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（1 个，151 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

</details>

<details>
<summary><b>1.21.8</b>（75 个 java）</summary>

**根包 · 规则设置与跨层实现**（4 个，369 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 99 |
| `RocketShulkerHandler.java` | 160 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |

**Mixin · 其余规则**（27 个，1993 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 51 |
| `Block_bedrockCanBeMinedMixin.java` | 42 |
| `CactusShulkerBoxWashMixin.java` | 66 |
| `ClientPlayerInteractionManager_bedrockMinesMixin.java` | 50 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 65 |
| `EndPortalTeleportMixin.java` | 45 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 44 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 57 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 29 |
| `MobEntity_witherSkeletonDropMixin.java` | 37 |
| `ObserverBlockMixin.java` | 83 |
| `PlayerEntity_directEntityDropsMixin.java` | 41 |
| `ScheduledTickView_directDropsMixin.java` | 65 |
| `ServerPlayerEntity_autoMendingMixin.java` | 26 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayerInteractionManager_directBlockDropsMixin.java` | 41 |
| `ServerWorld_directDropsMixin.java` | 43 |
| `ServerWorld_tickBlockMixin.java` | 42 |
| `StonecutterScreenHandlerMixin.java` | 60 |
| `WorldUtils_entityPlacementMixin.java` | 456 |
| `WorldUtils_waterloggedFillMixin.java` | 322 |

**轻松放置 · Mixin**（9 个，486 行）

| 文件 | 行数 |
|---|---:|
| `AbstractRailBlock_keepShapeMixin.java` | 46 |
| `AttachableItemPlacementMixin.java` | 60 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 31 |
| `ProjectionBlockItemMixin.java` | 161 |
| `SchematicHitVecMixin.java` | 116 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `SignPlacementMixin.java` | 29 |

**轻松放置 · 协议与规则编解码**（13 个，2234 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 100 |
| `BlockDataChannel.java` | 200 |
| `BlockDataPayload.java` | 31 |
| `CrafterItemData.java` | 99 |
| `ItemDataCodec.java` | 56 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 613 |
| `PlacementWindow.java` | 52 |
| `ProjectionPlacement.java` | 204 |
| `SchematicPlacementEncoder.java` | 235 |
| `SignItemData.java` | 145 |

**实体放置 · 生成与统计**（4 个，928 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCounter.java` | 180 |
| `EntityPlacementHandler.java` | 639 |
| `EntityPlacementPayload.java` | 61 |
| `EntitySpawn.java` | 48 |

**含水填充 · 补水与流体**（2 个，642 行）

| 文件 | 行数 |
|---|---:|
| `WaterloggedFillHandler.java` | 561 |
| `WaterloggedFillPayload.java` | 81 |

**配方部署 · 石切机数据包**（3 个，566 行）

| 文件 | 行数 |
|---|---:|
| `CopperStonecuttingDeployHook.java` | 291 |
| `RecipeDeployHooks.java` | 122 |
| `TrimTemplateRecipeDeployHook.java` | 153 |

**直掉 · 掉落上下文**（2 个，192 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 59 |
| `PendingDirectDropTicks.java` | 133 |

**客户端 · 界面与材料计算**（3 个，861 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 591 |

**区域守卫 · 区域与指令**（7 个，705 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 252 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 143 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（1 个，151 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

</details>

## 四、26.x 各层（`modern/`）

26.x 是**独立构建**，不与 `versions/` 共用 gradle 配置。

| 版本 | 本层 java 数 |
|---|---:|
| `26.1.2` | 103 |
| `26.2` | 103 |
| `26.3` | 105 |

<details>
<summary><b>26.1.2</b>（103 个 java）</summary>

**根包 · 规则设置与跨层实现**（38 个，3317 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 93 |
| `AutoMendingSettings.java` | 32 |
| `BedrockCanBeMinedSettings.java` | 29 |
| `BlockDataChannel.java` | 115 |
| `BlockDataPayload.java` | 30 |
| `CarpetHaoAdditionExtension.java` | 271 |
| `CarpetHaoAdditionSettings.java` | 27 |
| `CopperStonecuttingDeployHook.java` | 294 |
| `CopperStonecuttingSettings.java` | 31 |
| `CraftableTrimTemplateSettings.java` | 31 |
| `DirectDropsSettings.java` | 55 |
| `EasyPlaceEntityCommands.java` | 126 |
| `EasyPlaceEntitySettings.java` | 137 |
| `EasyPlaceWaterloggedSettings.java` | 72 |
| `EntityPlacementHandler.java` | 348 |
| `EntityPlacementPayload.java` | 46 |
| `EntityPlacementTrigger.java` | 82 |
| `EntitySpawn.java` | 43 |
| `GoldenCarrotCompostSettings.java` | 30 |
| `HaoDebug.java` | 40 |
| `LavaDepthStriderSettings.java` | 30 |
| `NoEndPortalTeleportSettings.java` | 30 |
| `ProjectionPlacementSettings.java` | 53 |
| `RecipeDeployHooks.java` | 138 |
| `RocketShulkerCommands.java` | 96 |
| `RocketShulkerHandler.java` | 141 |
| `RocketShulkerSettings.java` | 92 |
| `ShulkerBoxContentHelper.java` | 101 |
| `SnowyCalciteHooks.java` | 27 |
| `SnowyCalciteSettings.java` | 29 |
| `TerracottaUncolorSettings.java` | 28 |
| `TrimTemplateRecipeDeployHook.java` | 153 |
| `UseDyeOnShulkerBoxSettings.java` | 32 |
| `WackoBeaconsSettings.java` | 37 |
| `WaterloggedFillHandler.java` | 266 |
| `WaterloggedFillPayload.java` | 64 |
| `WitherSkeletonDropReductionSettings.java` | 41 |
| `WorldEaterProMaxSettings.java` | 27 |

**Mixin · 其余规则**（29 个，1863 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 52 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 71 |
| `EndPortalTeleportMixin.java` | 45 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 40 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 54 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 24 |
| `MobEntity_witherSkeletonDropMixin.java` | 39 |
| `MultiPlayerGameMode_bedrockMinesMixin.java` | 50 |
| `ObserverBlockMixin.java` | 83 |
| `Player_directEntityDropsMixin.java` | 41 |
| `ScheduledTickAccess_directDropsMixin.java` | 62 |
| `ServerLevel_directDropsMixin.java` | 43 |
| `ServerLevel_tickBlockMixin.java` | 42 |
| `ServerPlayerGameMode_directBlockDropsMixin.java` | 41 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayer_autoMendingMixin.java` | 26 |
| `StonecutterScreenHandlerMixin.java` | 58 |
| `WackoBeacons_BeaconBlockEntityMixin.java` | 66 |
| `WackoBeacons_BeaconPowerButtonMixin.java` | 29 |
| `WorldUtils_entityPlacementMixin.java` | 370 |
| `WorldUtils_waterloggedFillMixin.java` | 198 |

**轻松放置 · Mixin**（8 个，409 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 57 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 27 |
| `ProjectionBlockItemMixin.java` | 149 |
| `SchematicHitVecMixin.java` | 68 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `WorldActionMixin.java` | 65 |

**轻松放置 · 协议与规则编解码**（13 个，1991 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 92 |
| `CrafterItemData.java` | 87 |
| `EasyPlacePendingPlacement.java` | 34 |
| `ItemDataCodec.java` | 54 |
| `PendingMultiStage.java` | 52 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 583 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 203 |
| `SignItemData.java` | 143 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，731 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 166 |
| `EntityPlacementMaterials.java` | 100 |
| `EntityPlacementScreen.java` | 465 |

**区域守卫 · 区域与指令**（8 个，704 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 227 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 126 |
| `ZoneguardSettings.java` | 41 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（2 个，221 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 152 |
| `PlayerNoEndPortalTeleportList.java` | 69 |

</details>

<details>
<summary><b>26.2</b>（103 个 java）</summary>

**根包 · 规则设置与跨层实现**（38 个，3309 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 93 |
| `AutoMendingSettings.java` | 32 |
| `BedrockCanBeMinedSettings.java` | 29 |
| `BlockDataChannel.java` | 115 |
| `BlockDataPayload.java` | 30 |
| `CarpetHaoAdditionExtension.java` | 271 |
| `CarpetHaoAdditionSettings.java` | 27 |
| `CopperStonecuttingDeployHook.java` | 294 |
| `CopperStonecuttingSettings.java` | 31 |
| `CraftableTrimTemplateSettings.java` | 31 |
| `DirectDropsSettings.java` | 55 |
| `EasyPlaceEntityCommands.java` | 126 |
| `EasyPlaceEntitySettings.java` | 137 |
| `EasyPlaceWaterloggedSettings.java` | 72 |
| `EntityPlacementHandler.java` | 349 |
| `EntityPlacementPayload.java` | 46 |
| `EntityPlacementTrigger.java` | 82 |
| `EntitySpawn.java` | 43 |
| `GoldenCarrotCompostSettings.java` | 30 |
| `HaoDebug.java` | 40 |
| `LavaDepthStriderSettings.java` | 30 |
| `NoEndPortalTeleportSettings.java` | 30 |
| `ProjectionPlacementSettings.java` | 53 |
| `RecipeDeployHooks.java` | 138 |
| `RocketShulkerCommands.java` | 96 |
| `RocketShulkerHandler.java` | 141 |
| `RocketShulkerSettings.java` | 92 |
| `ShulkerBoxContentHelper.java` | 92 |
| `SnowyCalciteHooks.java` | 27 |
| `SnowyCalciteSettings.java` | 29 |
| `TerracottaUncolorSettings.java` | 28 |
| `TrimTemplateRecipeDeployHook.java` | 153 |
| `UseDyeOnShulkerBoxSettings.java` | 32 |
| `WackoBeaconsSettings.java` | 37 |
| `WaterloggedFillHandler.java` | 266 |
| `WaterloggedFillPayload.java` | 64 |
| `WitherSkeletonDropReductionSettings.java` | 41 |
| `WorldEaterProMaxSettings.java` | 27 |

**Mixin · 其余规则**（29 个，1863 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 52 |
| `Block_bedrockCanBeMinedMixin.java` | 37 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 71 |
| `EndPortalTeleportMixin.java` | 45 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 50 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 40 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 54 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 24 |
| `MobEntity_witherSkeletonDropMixin.java` | 39 |
| `MultiPlayerGameMode_bedrockMinesMixin.java` | 50 |
| `ObserverBlockMixin.java` | 83 |
| `Player_directEntityDropsMixin.java` | 41 |
| `ScheduledTickAccess_directDropsMixin.java` | 62 |
| `ServerLevel_directDropsMixin.java` | 43 |
| `ServerLevel_tickBlockMixin.java` | 42 |
| `ServerPlayerGameMode_directBlockDropsMixin.java` | 41 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayer_autoMendingMixin.java` | 26 |
| `StonecutterScreenHandlerMixin.java` | 58 |
| `WackoBeacons_BeaconBlockEntityMixin.java` | 66 |
| `WackoBeacons_BeaconPowerButtonMixin.java` | 29 |
| `WorldUtils_entityPlacementMixin.java` | 370 |
| `WorldUtils_waterloggedFillMixin.java` | 198 |

**轻松放置 · Mixin**（8 个，406 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 57 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 27 |
| `ProjectionBlockItemMixin.java` | 149 |
| `SchematicHitVecMixin.java` | 65 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `WorldActionMixin.java` | 65 |

**轻松放置 · 协议与规则编解码**（13 个，1991 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 92 |
| `CrafterItemData.java` | 87 |
| `EasyPlacePendingPlacement.java` | 34 |
| `ItemDataCodec.java` | 54 |
| `PendingMultiStage.java` | 52 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 583 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 203 |
| `SignItemData.java` | 143 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，731 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 166 |
| `EntityPlacementMaterials.java` | 100 |
| `EntityPlacementScreen.java` | 465 |

**区域守卫 · 区域与指令**（8 个，704 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 227 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 126 |
| `ZoneguardSettings.java` | 41 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（2 个，221 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 152 |
| `PlayerNoEndPortalTeleportList.java` | 69 |

</details>

<details>
<summary><b>26.3</b>（105 个 java）</summary>

**根包 · 规则设置与跨层实现**（38 个，3693 行）

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 93 |
| `AutoMendingSettings.java` | 32 |
| `BedrockCanBeMinedSettings.java` | 29 |
| `BlockDataChannel.java` | 117 |
| `BlockDataPayload.java` | 30 |
| `CarpetHaoAdditionExtension.java` | 272 |
| `CarpetHaoAdditionSettings.java` | 27 |
| `CopperStonecuttingDeployHook.java` | 378 |
| `CopperStonecuttingSettings.java` | 36 |
| `CraftableTrimTemplateSettings.java` | 31 |
| `DirectDropsSettings.java` | 55 |
| `EasyPlaceEntityCommands.java` | 126 |
| `EasyPlaceEntitySettings.java` | 137 |
| `EasyPlaceWaterloggedSettings.java` | 116 |
| `EntityPlacementHandler.java` | 349 |
| `EntityPlacementPayload.java` | 46 |
| `EntityPlacementTrigger.java` | 82 |
| `EntitySpawn.java` | 43 |
| `GoldenCarrotCompostSettings.java` | 30 |
| `HaoDebug.java` | 40 |
| `LavaDepthStriderSettings.java` | 30 |
| `NoEndPortalTeleportSettings.java` | 30 |
| `ProjectionPlacementSettings.java` | 53 |
| `RecipeDeployHooks.java` | 138 |
| `RocketShulkerCommands.java` | 96 |
| `RocketShulkerHandler.java` | 141 |
| `RocketShulkerSettings.java` | 92 |
| `ShulkerBoxContentHelper.java` | 92 |
| `SnowyCalciteHooks.java` | 27 |
| `SnowyCalciteSettings.java` | 29 |
| `TerracottaUncolorSettings.java` | 28 |
| `TrimTemplateRecipeDeployHook.java` | 153 |
| `UseDyeOnShulkerBoxSettings.java` | 32 |
| `WackoBeaconsSettings.java` | 37 |
| `WaterloggedFillHandler.java` | 512 |
| `WaterloggedFillPayload.java` | 66 |
| `WitherSkeletonDropReductionSettings.java` | 41 |
| `WorldEaterProMaxSettings.java` | 27 |

**Mixin · 其余规则**（30 个，2060 行）

| 文件 | 行数 |
|---|---:|
| `BlockState_bedrockCanBeMinedMixin.java` | 52 |
| `Block_bedrockCanBeMinedMixin.java` | 41 |
| `CactusShulkerBoxWashMixin.java` | 64 |
| `DyeItem_useDyeOnShulkerBoxMixin.java` | 71 |
| `EndPortalTeleportMixin.java` | 45 |
| `FluidBlock_snowyCalciteMixin.java` | 50 |
| `FluidState_breakWaterloggedMixin.java` | 37 |
| `GoldenCarrotCompostMixin.java` | 61 |
| `LavaFluid_snowyCalciteMixin.java` | 44 |
| `LivingEntity_directEntityDropsMixin.java` | 40 |
| `LivingEntity_lavaDepthStriderMixin.java` | 54 |
| `LivingEntity_witherSkeletonDropMixin.java` | 54 |
| `MinecraftServer_rocketShulkerMixin.java` | 25 |
| `MinecraftServer_waterloggedFillMixin.java` | 26 |
| `MobEntity_witherSkeletonDropMixin.java` | 39 |
| `MultiPlayerGameMode_bedrockMinesMixin.java` | 50 |
| `ObserverBlockMixin.java` | 83 |
| `Player_directEntityDropsMixin.java` | 41 |
| `ScheduledTickAccess_directDropsMixin.java` | 62 |
| `ServerLevel_directDropsMixin.java` | 43 |
| `ServerLevel_tickBlockMixin.java` | 42 |
| `ServerPlayerGameMode_directBlockDropsMixin.java` | 41 |
| `ServerPlayerInteractionManager_bedrockMinesMixin.java` | 68 |
| `ServerPlayer_autoMendingMixin.java` | 26 |
| `SignBlockEntity_colorProbeMixin.java` | 53 |
| `StonecutterScreenHandlerMixin.java` | 58 |
| `WackoBeacons_BeaconBlockEntityMixin.java` | 66 |
| `WackoBeacons_BeaconPowerButtonMixin.java` | 29 |
| `WorldUtils_entityPlacementMixin.java` | 370 |
| `WorldUtils_waterloggedFillMixin.java` | 325 |

**轻松放置 · Mixin**（8 个，522 行）

| 文件 | 行数 |
|---|---:|
| `AttachableItemPlacementMixin.java` | 58 |
| `BeaconEffectAccessor.java` | 17 |
| `CopperBulbPlacementMixin.java` | 27 |
| `ProjectionBlockItemMixin.java` | 263 |
| `SchematicHitVecMixin.java` | 66 |
| `SchematicUtilsInvoker.java` | 13 |
| `SignListenerInvoker.java` | 13 |
| `WorldActionMixin.java` | 65 |

**轻松放置 · 协议与规则编解码**（14 个，2044 行）

| 文件 | 行数 |
|---|---:|
| `BeaconItemData.java` | 92 |
| `CrafterItemData.java` | 87 |
| `EasyPlacePendingPlacement.java` | 34 |
| `ItemDataCodec.java` | 54 |
| `PendingMultiStage.java` | 52 |
| `PlacementBitTools.java` | 62 |
| `PlacementCodec.java` | 65 |
| `PlacementCodecs.java` | 372 |
| `PlacementRules.java` | 583 |
| `PlacementWindow.java` | 63 |
| `ProjectionPlacement.java` | 181 |
| `SchematicPlacementEncoder.java` | 204 |
| `SignColorMemory.java` | 52 |
| `SignItemData.java` | 143 |

**直掉 · 掉落上下文**（2 个，196 行）

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**客户端 · 界面与材料计算**（3 个，731 行）

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 166 |
| `EntityPlacementMaterials.java` | 100 |
| `EntityPlacementScreen.java` | 465 |

**区域守卫 · 区域与指令**（8 个，704 行）

| 文件 | 行数 |
|---|---:|
| `DetectorRegion.java` | 47 |
| `RegionObserverRefresh.java` | 118 |
| `ZoneguardCommands.java` | 227 |
| `ZoneguardHooks.java` | 53 |
| `ZoneguardPermissions.java` | 61 |
| `ZoneguardSavedData.java` | 126 |
| `ZoneguardSettings.java` | 41 |
| `ZoneguardState.java` | 31 |

**末地门 · 传送名单**（2 个，221 行）

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 152 |
| `PlayerNoEndPortalTeleportList.java` | 69 |

</details>

# 各版本文件清单

> 本文件由仓库实际扫描生成，行数为真实行数。改完代码如果动了结构，请重新核对。

## 一、仓库结构总览

```
Carpet-Hao-Addition/
├─ src/main/java/            共享源码 28 个 java（所有版本都用）
├─ src/main/resources/       共享资源（lang 必须放这里）
├─ versions/                 1.21.x 各层 8 个：1.21, 1.21.1, 1.21.10, 1.21.11, 1.21.2, 1.21.4, 1.21.6, 1.21.8
│    └─ <版本>/src/main/java/   该层的完整实现（约 70+ 个 java）
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

**根包 · 规则设置与跨层实现**

| 文件 | 行数 |
|---|---:|
| `AutoMendingSettings.java` | 32 |
| `BedrockCanBeMinedSettings.java` | 29 |
| `CarpetHaoAdditionExtension.java` | 310 |
| `CopperStonecuttingSettings.java` | 31 |
| `CraftableTrimTemplateSettings.java` | 31 |
| `DirectDropsSettings.java` | 59 |
| `EasyPlaceEntityCommands.java` | 126 |
| `EasyPlaceEntitySettings.java` | 137 |
| `EasyPlaceWaterloggedSettings.java` | 116 |
| `EntityPlacementTrigger.java` | 82 |
| `GoldenCarrotCompostSettings.java` | 30 |
| `HaoDebug.java` | 39 |
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

**Mixin · 其余规则**

| 文件 | 行数 |
|---|---:|
| `WackoBeacons_BeaconBlockEntityMixin.java` | 68 |
| `WackoBeacons_BeaconButtonWidgetMixin.java` | 43 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportList.java` | 69 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardSettings.java` | 41 |

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
| `1.21.8` | 74 |

### 各层文件明细

<details>
<summary><b>1.21</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 151 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 517 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `command/ZoneguardCommands.java` | 229 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 138 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>1.21.1</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 151 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 517 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `command/ZoneguardCommands.java` | 229 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 138 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>1.21.10</b>（72 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 519 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `command/ZoneguardCommands.java` | 227 |
| `command/ZoneguardPermissions.java` | 60 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 125 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>1.21.11</b>（73 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 519 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 152 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `command/ZoneguardCommands.java` | 227 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 125 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>1.21.2</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 518 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `command/ZoneguardCommands.java` | 229 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 138 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>1.21.4</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 518 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `command/ZoneguardCommands.java` | 229 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 138 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>1.21.6</b>（71 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 518 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `command/ZoneguardCommands.java` | 229 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 124 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>1.21.8</b>（74 个 java）</summary>

**根包 · 规则设置与跨层实现**

| 文件 | 行数 |
|---|---:|
| `AutoMendingHandler.java` | 88 |
| `BlockDataChannel.java` | 125 |
| `BlockDataPayload.java` | 30 |
| `CopperStonecuttingDeployHook.java` | 290 |
| `EntityPlacementCounter.java` | 180 |
| `EntityPlacementHandler.java` | 611 |
| `EntityPlacementPayload.java` | 46 |
| `EntitySpawn.java` | 43 |
| `RecipeDeployHooks.java` | 115 |
| `RocketShulkerHandler.java` | 139 |
| `ShulkerBoxContentHelper.java` | 83 |
| `SnowyCalciteHooks.java` | 27 |
| `TrimTemplateRecipeDeployHook.java` | 152 |
| `WaterloggedFillHandler.java` | 490 |
| `WaterloggedFillPayload.java` | 66 |

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 169 |
| `EntityPlacementMaterials.java` | 101 |
| `EntityPlacementScreen.java` | 572 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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
| `StonecutterScreenHandlerMixin.java` | 57 |
| `WorldUtils_entityPlacementMixin.java` | 423 |
| `WorldUtils_waterloggedFillMixin.java` | 314 |

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 151 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `command/ZoneguardCommands.java` | 229 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 124 |
| `region/ZoneguardState.java` | 31 |

</details>

## 四、26.x 各层（`modern/`）

这三层**各自独立构建**，不并入根项目的 `./gradlew build`。

<details>
<summary><b>26.1.2</b>（103 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 166 |
| `EntityPlacementMaterials.java` | 100 |
| `EntityPlacementScreen.java` | 466 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 152 |
| `PlayerNoEndPortalTeleportList.java` | 69 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `ZoneguardSettings.java` | 41 |
| `command/ZoneguardCommands.java` | 227 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 126 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>26.2</b>（103 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 166 |
| `EntityPlacementMaterials.java` | 100 |
| `EntityPlacementScreen.java` | 466 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 152 |
| `PlayerNoEndPortalTeleportList.java` | 69 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `ZoneguardSettings.java` | 41 |
| `command/ZoneguardCommands.java` | 227 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 126 |
| `region/ZoneguardState.java` | 31 |

</details>

<details>
<summary><b>26.3</b>（105 个 java）</summary>

**根包 · 规则设置与跨层实现**

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

**客户端 · 界面与材料计算**

| 文件 | 行数 |
|---|---:|
| `EntityPlacementCosts.java` | 166 |
| `EntityPlacementMaterials.java` | 100 |
| `EntityPlacementScreen.java` | 466 |

**轻松放置 · 协议与规则编解码**

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

**轻松放置 · Mixin**

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

**Mixin · 其余规则**

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

**直掉 · 掉落上下文**

| 文件 | 行数 |
|---|---:|
| `DirectDropContext.java` | 58 |
| `PendingDirectDropTicks.java` | 138 |

**末地门 · 传送名单**

| 文件 | 行数 |
|---|---:|
| `PlayerNoEndPortalTeleportCommands.java` | 152 |
| `PlayerNoEndPortalTeleportList.java` | 69 |

**区域守卫 · 区域与指令**

| 文件 | 行数 |
|---|---:|
| `ZoneguardHooks.java` | 53 |
| `ZoneguardSettings.java` | 41 |
| `command/ZoneguardCommands.java` | 227 |
| `command/ZoneguardPermissions.java` | 61 |
| `region/DetectorRegion.java` | 47 |
| `region/RegionObserverRefresh.java` | 118 |
| `region/ZoneguardSavedData.java` | 126 |
| `region/ZoneguardState.java` | 31 |

</details>

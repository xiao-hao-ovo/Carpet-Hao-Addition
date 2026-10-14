**Carpet-Hao-Addition** 是一个 Fabric Carpet 扩展模组，内置一组可开关的地毯规则（**全部默认关闭**），覆盖 Minecraft **1.21 ~ 26.3**；只依赖 **Carpet + Fabric API + Fabric Loader**，可纯服务端运行。

**Carpet-Hao-Addition** is a Fabric Carpet extension that bundles a set of toggleable Carpet rules — **all disabled by default** — covering Minecraft **1.21 through 26.3**. It depends only on **Carpet + Fabric API + Fabric Loader**, and runs fine on a dedicated server.

- **规则 / Rules**：21 条规则，注册在 Carpet 默认管理器，分类 **Hao**（游戏内显示“昊”），中英双语文案。21 rules, registered in Carpet's default manager under the **Hao** category, with both Chinese and English text.
- **下载 / Download**：请下载与你的 Minecraft 版本**完全对应**的 jar —— 每个 jar 只针对一个版本（1.21, 1.21.1, 1.21.2, 1.21.4, 1.21.6, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2, 26.3）。Pick the jar matching your exact Minecraft version — each jar targets a single version.
- **依赖 / Requirements**：Fabric Loader ≥ 0.16.10 及对应版本的 Carpet，另需 Fabric API；1.21.x 需 Java 21+，26.x 需 **Java 25**。Fabric Loader ≥ 0.16.10 plus the matching Carpet release and Fabric API; Java 21+ for 1.21.x, **Java 25** for 26.x.
- **源码 / Source**：https://github.com/xiao-hao-ovo/Carpet-Hao-Addition

---

## 中文说明

基于 Fabric 的 [Carpet](https://github.com/gnembon/fabric-carpet) 多版本扩展模组，为技术生存与原版友好玩法提供少量实用、可配置的地毯规则。

- 所有规则**默认关闭**，在 `/carpet` 中手动开启后才生效。
- 只依赖 **Carpet + Fabric API**，不需要其它前置；可纯服务端使用。
- 全部规则注册在 Carpet 默认管理器，分类 **Hao（游戏内显示“昊”）**，文案中英双语。

### 依赖

| 名称 | 类型 | 链接 | 备注 |
|----|----|----|----|
| Carpet | 必须 | [Modrinth](https://modrinth.com/mod/carpet) | |
| Fabric Loader | 必须 | [官网](https://fabricmc.net/) | ≥0.16.10 |
| Fabric API | 必须 | [Modrinth](https://modrinth.com/mod/fabric-api) | 自定义网络包与逐 tick 回调需要它 |

### 版本支持

| 游戏版本 | 状态 |
|----|----|
| 1.21 / 1.21.1 / 1.21.2 / 1.21.4 / 1.21.6 | 维护中 |
| 1.21.8 / 1.21.10 / 1.21.11 | 维护中 |
| 26.1.2 / 26.2 / 26.3 | 维护中 |

Java 21+（26.x 需要 Java 25）。

### 规则列表

游戏内用 `/carpet <规则> <值>` 查看/切换（也可在 Carpet 的规则界面里操作）。

| 规则 | 名称 | 说明 |
|---|---|---|
| `haoAutoMending` | 自动经验修补 | 移植于 Carpet WuHu Addition。每 1 秒把玩家当前等级进度里已攒下的经验自动拿去修补身上带「经验修补」附魔且已损坏的装备,能修多少修多少,并扣除等量经验点(不会凭空生耐久)。默认关闭。 |
| `haoBedrockMines` | 基岩可挖掘 | 开启后,基岩可被挖掘(挖掘时硬度等同黑曜石),挖碎后掉落基岩物品;关闭时恢复原版。 |
| `haoCopperStonecutting` | 切石机切铜 | 让切石机按「材料当量」转换各种铜变种:任意变体可一步切出任意形态(切制/凿制/格栅/台阶/楼梯/门/活板门/铜灯),同当量 1:1 互转,降级按比例(铜块→切制 ×4、→台阶 ×8)。所有转换严格等值,切不出无限资源。默认关闭。 |
| `haoCraftableTrimTemplates` | 可合成纹饰模板 | 开启后 19 种纹饰/升级模板都能合成：7 个钻石 + 1 个该模板的获取途径材料（沉船→圆石、沙漠神殿→砂岩、古迹废墟→陶瓦……就是原版复制配方里的那个材料）。默认关闭。 |
| `haoDirectBlockDrops` | 方块掉落直入背包 | 开启后,玩家破坏方块产生的掉落物(含连锁破坏、失去支撑后的同步掉落,以及被破坏容器内释放的物品)优先直接进入该玩家背包;背包放不下的部分仍按原版掉落在世界中。 |
| `haoDirectEntityDrops` | 实体掉落直入背包 | 开启后,玩家击杀实体产生的掉落物(生物与盔甲架,以及矿车、船、画、物品展示框等)优先直接进入该玩家背包;背包放不下的部分仍按原版掉落在世界中。 |
| `haoEnhancedWorldEaterProMax` | 增强世界吞噬者ProMax | 让含水方块也能被爆炸炸掉。原版算抗性时取 max(方块抗性, 流体抗性),水的流体抗性是 100,所以含水的台阶/珊瑚扇等炸不动;开启后水的流体抗性按 0 算。只负责这一件事。默认关闭。 |
| `haoGoldenCarrotCompost` | 金胡萝卜堆肥 | 开启后,手持金胡萝卜对堆肥桶右键可 100% 堆肥(消耗与效果同普通可堆肥物品,满桶流程一致)。 |
| `haoLavaDepthStrider` | 岩浆中的深海探索者 | 开启后,玩家在岩浆中移动时不再受到岩浆减速:直接套用水中移动的公式(即 water_movement_efficiency 属性),手感与穿着深海探索者靴子在水中一致;附魔等级越高移速越快。 |
| `haoNoEndPortalTeleport` | 末地门不传送 | 开启后提供 /playerNoEndPortalTeleport 名单管理。规则本身不改变传送:把玩家加入黑名单或开启 globalMode 后,对应玩家才不会被末地传送门传送;非玩家实体不受影响。 |
| `haoProjectionEntity` | 轻松放置实体 | 照投影施工时把投影里的实体(矿车/船/盔甲架/展示框/画等)也一并放出,并扣掉对应物品。取值:false=关闭;true=开启,右键轻松放置与配套客户端的快捷键都触发;not_trigger=只有配套客户端(如 HaoMaSa)的快捷键触发,右键轻松放置不放实体、也不弹选择器界面;但服务端照常接受请求。只对有对应物品的实体生效;一次放几个由 easyPlaceEntityCount 决定。物品不够时一个都不放。默认关闭。 |
| `haoProjectionPlacement` | 投影精准放置 | 照投影轻松放置时,让带朝向、开关、颜色、方块实体数据等复杂状态的方块也按投影正确放下。需要安装 Litematica。含水方块请用 haoProjectionWaterlogged。取值:false=关闭(默认);true=开启,但不还原堆肥桶层数;with_composter_level=开启,连堆肥桶层数也一起还原。 |
| `haoProjectionWaterlogged` | 投影轻松放置补料 | 照投影轻松放置时自动补齐投影里的水/岩浆/装岩浆的炼药锅/火:放水消耗冰,放岩浆消耗岩浆块,点火优先消耗火焰弹。取值:false=关闭;true=只用背包材料;shulker_direct=直接扣潜影盒内的材料;shulker_take=把材料取到背包。材料不足时只放方块本身。需要 Litematica。 |
| `haoProjectionWaterloggedTrigger` | 投影轻松放置补料触发条件 | 补料功能在什么姿态下才生效:always=两种姿态都触发(站着或蹲下都补,默认);standing=站立触发(玩家未潜行时补料,潜行时不补);sneaking=蹲下触发(玩家按住潜行键时补料,未潜行时不补)。 |
| `haoRocketShulker` | 火箭潜影盒自动补给 | 名称正好是 rocket 的潜影盒:指定格子里的烟花用光时会从盒内自动补一组(最多 64),用光再补。用 /haoRocketShulker offhand(默认) 或 /haoRocketShulker mainhand <1-9> 设置补给位置,只存内存,重启还原。目标格有别的物品时不会动它。默认关闭。 |
| `haoSnowyCalcite` | 雪地方解石 | 开启后,在降雪(雪地)生物群系中,岩浆与水的刷石机产物(石头/圆石)将变为方解石。 |
| `haoTerracottaUncolor` | 陶瓦还原 | 开启后,切石机可把 16 种染色陶瓦还原为原色陶瓦、16 种釉陶瓦还原为同色染色陶瓦;规则关闭时切石机不出现这些还原配方。 |
| `haoUseDyeOnShulkerBox` | 潜影盒染色 | 手持染料右键潜影盒可把它染成对应颜色;潜行手持仙人掌右键有色潜影盒可洗回无色(不消耗仙人掌)。染色/洗色都完整保留盒内物品与自定义名称。默认关闭。 |
| `haoWackoBeacons` | 信标效果解锁(客户端) | 解除信标的效果限制:Regeneration 可作主效果(配合副效果即 II 级),效果按钮不再按信标层数禁用(9 块钻石也能选 Strength)。 |
| `haoWitherSkeletonDrop` | 凋零骷髅掉落去除 | 去除凋零骷髅的指定掉落物。选项:false=原版;bone=去骨头;coal=去煤炭;skull=去凋零骷髅头颅;sword=去掉落的手持石剑;all=去除以上全部。 |
| `haoZoneguard` | 局部侦测器禁用 | 在 /haoZoneguard 配置的立方区域内禁用侦测器(观察者)行为。关闭规则会恢复区域内侦测器。 |

扩展模板自带的示例规则（无实际功能，可按需删除）：

| 规则 | 名称 | 说明 |
|---|---|---|
| `exampleBoolean` | 示例布尔规则 | Carpet 扩展模板自带的布尔规则示例，无实际功能。 |
| `exampleString` | 示例字符串规则 | Carpet 扩展模板自带的字符串规则示例（可选 foo / bar / baz），无实际功能。 |

### 命令

各命令受**对应规则**门控：规则关闭时整棵命令树（含 `help`）不可见、不可执行。

#### `/haoZoneguard`

需要 `/carpet haoZoneguard true` 开启。

- `/haoZoneguard set <id> <from> <to>` —— 设置立方禁用区域（两个对角坐标自动归一化，不必按顺序给；配置随世界存档持久化）
- `/haoZoneguard view` —— 列出已配置的区域
- `/haoZoneguard clear <id>` —— 清除指定区域
- `/haoZoneguard op add|remove <player>` / `op list` —— 管理该命令的权限白名单
- `/haoZoneguard help` —— 显示使用说明

#### `/playerNoEndPortalTeleport`

需要 `/carpet haoNoEndPortalTeleport true` 开启。

- `/playerNoEndPortalTeleport globalMode <true|false>` —— `true`：**所有**玩家都不被末地门传送；`false`：只有**名单内**玩家不被传送
- `/playerNoEndPortalTeleport add|remove <player>` —— 名单增删
- `/playerNoEndPortalTeleport list` / `clear` —— 查看 / 清空名单
- `/playerNoEndPortalTeleport help` —— 显示用法

> 名单与全局模式保存在**内存**里，重启服务器后清空。

#### `/haoRocketShulker`

设置**自己**的火箭潜影盒补给位置（每个玩家各一份），配合 `/carpet haoRocketShulker true` 使用。

- `/haoRocketShulker` —— 查看自己当前的设置
- `/haoRocketShulker offhand` —— 补到自己的副手
- `/haoRocketShulker mainhand <1-9>` —— 补到自己的主手快捷栏第 1~9 格

#### `/easyPlaceEntityCount` / `/easyPlaceEntityUi`

配合 `/carpet haoProjectionEntity true`（或 `not_trigger`）使用。

- `/easyPlaceEntityCount [1-64]` —— 查看 / 设置自己一次放置几个实体（只存内存）
- `/easyPlaceEntityUi [true|false]` —— 查看 / 切换自己的选择器界面开关

### 常见问题

**装错版本会报 `Incompatible mods found!`** —— 每个 jar 只对应一个 Minecraft 版本，请下载与你游戏版本完全一致的那个。

### 许可与致谢

本项目以 **GNU LGPL-3.0** 授权。

基于 [fabric-carpet-extension-example-mod](https://github.com/gnembon/fabric-carpet-extension-example-mod) 改造。

---

## English

A multi-version [Carpet](https://github.com/gnembon/fabric-carpet) extension for Fabric, adding a small set of practical, configurable Carpet rules for technical survival and vanilla-friendly play.

- Every rule is **disabled by default** and only takes effect after you turn it on in `/carpet`.
- Depends only on **Carpet + Fabric API** — no other prerequisites; works on a dedicated server.
- All rules are registered in Carpet's default manager under the **Hao** category, with both Chinese and English text.

### Requirements

| Name | Required | Link | Notes |
|----|----|----|----|
| Carpet | Yes | [Modrinth](https://modrinth.com/mod/carpet) | |
| Fabric Loader | Yes | [fabricmc.net](https://fabricmc.net/) | ≥0.16.10 |
| Fabric API | Yes | [Modrinth](https://modrinth.com/mod/fabric-api) | needed for custom network payloads and per-tick callbacks |

### Supported versions

| Game version | Status |
|----|----|
| 1.21 / 1.21.1 / 1.21.2 / 1.21.4 / 1.21.6 | Maintained |
| 1.21.8 / 1.21.10 / 1.21.11 | Maintained |
| 26.1.2 / 26.2 / 26.3 | Maintained |

Java 21+ (Java 25 for 26.x).

### Rules

Use `/carpet <rule> <value>` in game to view or toggle a rule (the Carpet rule screen works too).

| Rule | Name | Description |
|---|---|---|
| `haoAutoMending` | Auto mending (new) | Ported from Carpet WuHu Addition. Once a second, automatically spend the XP already accumulated in the level bar to repair damaged gear enchanted with Mending, repairing as much as that XP allows and deducting the same amount of XP (no free durability). Off by default. |
| `haoBedrockMines` | Bedrock Can Be Mined | While enabled, bedrock can be mined (it mines like obsidian) and drops itself as an item when broken; while disabled, vanilla behavior is restored. |
| `haoCopperStonecutting` | Copper stonecutting recipes | Lets the stonecutter convert copper variants by equal material value: any variant can be cut in one step into any shape (cut/chiseled/grate/slab/stairs/door/trapdoor/bulb), equal-value shapes convert 1:1, downgrades use the exact ratio (copper block -> cut x4, -> slab x8). Strictly value-preserving, no infinite resource loop. Off by default. |
| `haoCraftableTrimTemplates` | Craftable Trim Templates | When on, all 19 trim/upgrade smithing templates become craftable: 7 diamonds + 1 material taken from that template's original duplicating recipe (shipwreck -> cobblestone, desert temple -> sandstone, trail ruins -> terracotta, ...). Off by default. |
| `haoDirectBlockDrops` | Direct Block Drops | While enabled, drops produced by blocks the player breaks (including chain breaks, drops of blocks that lost support in the same tick, and items released from broken containers) go straight into that player's inventory; whatever does not fit still drops in the world as usual. |
| `haoDirectEntityDrops` | Direct Entity Drops | While enabled, drops produced by killing an entity (mobs and armor stands, as well as minecarts, boats, paintings and item frames) go straight into that player's inventory; whatever does not fit still drops in the world as usual. |
| `haoEnhancedWorldEaterProMax` | Enhanced World Eater ProMax | Lets waterlogged blocks be destroyed by explosions. Vanilla takes max(block resistance, fluid resistance) and water has a fluid resistance of 100, so waterlogged slabs/coral fans etc. survive TNT; when on, water's fluid resistance counts as 0. Off by default. |
| `haoGoldenCarrotCompost` | Golden Carrot Composting | While enabled, right-clicking a composter while holding a golden carrot composts it with 100% chance, exactly like a regular compostable item. |
| `haoLavaDepthStrider` | Lava Depth Strider | While enabled, moving through lava no longer slows you down if you have Depth Strider: the water movement formula (and its water_movement_efficiency attribute) is applied to lava as well, so lava feels exactly like wearing Depth Strider boots in water. Higher enchantment levels move faster. |
| `haoNoEndPortalTeleport` | No End Portal Teleport | Enables the /playerNoEndPortalTeleport list management. The rule itself does not change teleporting: only players added to the blacklist (or with globalMode enabled) are not teleported by end portals; non-player entities are unaffected. |
| `haoProjectionEntity` | Easy Place Entities | While following a schematic with easy place, also spawns the schematic's entities (minecarts, boats, armor stands, item frames, paintings...) and consumes the matching items. Values: false = off; true = on, triggered by both right-click easy place and a companion client's hotkey; not_trigger = only a companion client's hotkey (e.g. HaoMaSa) triggers it - right-click easy place spawns no entities and shows no picker - while the server side keeps accepting requests. Only entities with a matching item are supported; count is set by easyPlaceEntityCount. Spawns nothing if items are short. Off by default. |
| `haoProjectionPlacement` | Projection Placement | When building from a schematic, places blocks with complex states (facing, toggle, color, block-entity data...) exactly as the schematic specifies. Requires Litematica. For waterlogged blocks use haoProjectionWaterlogged. Values: false = off (default); true = on, but a composter's fill level is not restored; with_composter_level = on, restores a composter's fill level too. |
| `haoProjectionWaterlogged` | Easy Place Fill | While following a schematic with easy place, fills in the schematic's water/lava/lava cauldron/fire: water consumes ice, lava consumes a magma block, igniting prefers a fire charge. Values: false = off; true = inventory only; shulker_direct = take from inside a shulker box; shulker_take = move the material into the inventory. If short on materials, only the block itself is placed. Requires Litematica. |
| `haoProjectionWaterloggedTrigger` | Easy Place Fill Trigger | When the fill feature is allowed to run: always = both poses, standing or sneaking (default); standing = only while the player is not sneaking; sneaking = only while the player is sneaking (holding the sneak key). |
| `haoRocketShulker` | Rocket Shulker Auto Refill | For a shulker box named exactly 'rocket': when the chosen slot runs out of fireworks, refills one stack (up to 64) from the box, repeating until the box is empty. Set the slot with /haoRocketShulker offhand (default) or /haoRocketShulker mainhand <1-9>; kept in memory only. Never touches a slot holding something else. Off by default. |
| `haoSnowyCalcite` | Snowy Calcite | While enabled, stone/cobblestone produced by lava+water generators in snowy (snow-precipitating) biomes becomes calcite. |
| `haoTerracottaUncolor` | Terracotta Uncolor | While enabled, the stonecutter can revert all 16 dyed terracotta to plain terracotta and all 16 glazed terracotta to their dyed terracotta; while disabled, these recipes do not appear in the stonecutter at all. |
| `haoUseDyeOnShulkerBox` | Use dye on shulker box | Right-click a shulker box with dye to recolour it; sneak + right-click a coloured box while holding a cactus to wash it back to plain (the cactus is not consumed). Contents and custom name are preserved. Off by default. |
| `haoWackoBeacons` | Wacko Beacons (client) | Client-side beacon unlock: Regeneration selectable as a primary effect, and effect buttons are no longer disabled by beacon tier. |
| `haoWitherSkeletonDrop` | Wither Skeleton Drop Reduction | Removes selected drops from wither skeletons. Options: false=vanilla; bone=no bones; coal=no coal; skull=no wither skeleton skull; sword=no dropped stone sword; all=remove all of the above. |
| `haoZoneguard` | haoZoneguard | Disables observers inside the cubic regions configured with /haoZoneguard. Turning the rule off restores observers in those regions. |

Example rules shipped with the extension template (no gameplay effect, safe to remove):

| Rule | Name | Description |
|---|---|---|
| `exampleBoolean` | Example Boolean Rule | Example boolean rule shipped with the Carpet extension template; no gameplay effect. |
| `exampleString` | Example String Rule | Example string rule shipped with the Carpet extension template (foo / bar / baz); no gameplay effect. |

### Commands

Every command is gated by its **corresponding rule**: while the rule is off, the whole command tree (including `help`) is hidden and unusable.

#### `/haoZoneguard`

Requires `/carpet haoZoneguard true`.

- `/haoZoneguard set <id> <from> <to>` — define a cuboid area (the two corners are normalized, order does not matter; the config persists with the world)
- `/haoZoneguard view` — list configured areas
- `/haoZoneguard clear <id>` — remove an area
- `/haoZoneguard op add|remove <player>` / `op list` — manage who may use this command
- `/haoZoneguard help` — usage

#### `/playerNoEndPortalTeleport`

Requires `/carpet haoNoEndPortalTeleport true`.

- `/playerNoEndPortalTeleport globalMode <true|false>` — `true`: **no** player is teleported by end portals; `false`: only players **on the list** are skipped
- `/playerNoEndPortalTeleport add|remove <player>` — edit the list
- `/playerNoEndPortalTeleport list` / `clear` — view / clear the list
- `/playerNoEndPortalTeleport help` — usage

> The list and global mode live in **memory** and reset when the server restarts.

#### `/haoRocketShulker`

Sets **your own** rocket-shulker refill slot (per player), used together with `/carpet haoRocketShulker true`.

- `/haoRocketShulker` — show your current setting
- `/haoRocketShulker offhand` — refill into your offhand
- `/haoRocketShulker mainhand <1-9>` — refill into hotbar slot 1~9

#### `/easyPlaceEntityCount` / `/easyPlaceEntityUi`

Used together with `/carpet haoProjectionEntity true` (or `not_trigger`).

- `/easyPlaceEntityCount [1-64]` — view / set how many entities you place at once (memory only)
- `/easyPlaceEntityUi [true|false]` — view / toggle your picker screen

### FAQ

**Installing the wrong jar gives `Incompatible mods found!`** — each jar targets exactly one Minecraft version; download the one matching your game version.

### License & credits

Licensed under **GNU LGPL-3.0**.

Based on [fabric-carpet-extension-example-mod](https://github.com/gnembon/fabric-carpet-extension-example-mod).

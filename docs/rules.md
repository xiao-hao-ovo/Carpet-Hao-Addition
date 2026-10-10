# 规则

**提示：可以使用`Ctrl+F`快速查找自己想要的规则**

本模组的规则注册在 Carpet 的默认管理器里，用 `/carpet <规则> <值>` 设置

## 自动经验修补 (autoMending_new)

移植于 Carpet WuHu Addition。每 1 秒把玩家当前等级进度里已攒下的经验自动拿去修补身上带「经验修补」附魔且已损坏的装备,能修多少修多少,并扣除等量经验点(不会凭空生耐久)。默认关闭。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 切石机切铜 (copperStonecuttingRecipes)

让切石机按「材料当量」转换各种铜变种:任意变体可一步切出任意形态(切制/凿制/格栅/台阶/楼梯/门/活板门/铜灯),同当量 1:1 互转,降级按比例(铜块→切制 ×4、→台阶 ×8)。所有转换严格等值,切不出无限资源。默认关闭。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 方块掉落直入背包 (directBlockDrops)

开启后,玩家破坏方块产生的掉落物(含连锁破坏、失去支撑后的同步掉落,以及被破坏容器内释放的物品)优先直接进入该玩家背包;背包放不下的部分仍按原版掉落在世界中。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 实体掉落直入背包 (directEntityDrops)

开启后,玩家击杀实体产生的掉落物(生物与盔甲架,以及矿车、船、画、物品展示框等)优先直接进入该玩家背包;背包放不下的部分仍按原版掉落在世界中。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 轻松放置实体 (easyPlaceEntity)

照投影施工时把投影里的实体(矿车/船/盔甲架/展示框/画等)也一并放出,并扣掉对应物品。只对有对应物品的实体生效;一次放几个由 easyPlaceEntityCount 决定。物品不够时一个都不放。默认关闭。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 投影轻松放置补料 (easyPlaceWaterlogged)

照投影轻松放置时自动补齐投影里的水/岩浆/装岩浆的炼药锅/火:放水消耗冰,放岩浆消耗岩浆块,点火优先消耗火焰弹。取值:false=关闭;true=只用背包材料;shulker_direct=直接扣潜影盒内的材料;shulker_take=把材料取到背包。材料不足时只放方块本身。需要 Litematica。

- 类型: `Mode`
- 默认值: `FALSE`
- 参考选项: `FALSE`, `TRUE`, `SHULKER_DIRECT`, `SHULKER_TAKE`
- 分类: `Hao`

## 投影轻松放置补料触发条件 (easyPlaceWaterloggedTrigger)

补料功能在什么姿态下才生效:always=两种姿态都触发(站着或蹲下都补,默认);standing=站立触发(玩家未潜行时补料,潜行时不补);sneaking=蹲下触发(玩家按住潜行键时补料,未潜行时不补)。

- 类型: `Trigger`
- 默认值: `ALWAYS`
- 参考选项: `STANDING`, `SNEAKING`, `ALWAYS`
- 分类: `Hao`

## 示例布尔规则 (exampleBoolean)

Carpet 扩展模板自带的布尔规则示例，无实际功能。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `haoaddition`

## 示例字符串规则 (exampleString)

Carpet 扩展模板自带的字符串规则示例（可选 foo / bar / baz），无实际功能。

- 类型: `String`
- 默认值: `foo`
- 参考选项: `foo`, `bar`, `baz`
- 分类: `haoaddition`

## 金胡萝卜堆肥 (goldenCarrotCompost)

开启后,手持金胡萝卜对堆肥桶右键可 100% 堆肥(消耗与效果同普通可堆肥物品,满桶流程一致)。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 基岩可挖掘 (haoBedrockMines)

开启后,基岩可被挖掘(挖掘时硬度等同黑曜石),挖碎后掉落基岩物品;关闭时恢复原版。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 投影精准放置 (haoProjectionPlacement)

照投影轻松放置时,让带朝向、开关、颜色、方块实体数据等复杂状态的方块也按投影正确放下。需要安装 Litematica。含水方块请用 easyPlaceWaterlogged。取值:false=关闭(默认);true=开启,但不还原堆肥桶层数;with_composter_level=开启,连堆肥桶层数也一起还原。

- 类型: `Mode`
- 默认值: `FALSE`
- 参考选项: `FALSE`, `TRUE`, `WITH_COMPOSTER_LEVEL`
- 分类: `Hao`

## 可合成纹饰模板 (haoCraftableTrimTemplates)

开启后 19 种纹饰/升级模板都能合成：7 个钻石 + 1 个该模板的获取途径材料（沉船→圆石、沙漠神殿→砂岩、古迹废墟→陶瓦……就是原版复制配方里的那个材料）。默认关闭。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 增强世界吞噬者ProMax (haoEnhancedWorldEaterProMax)

让含水方块也能被爆炸炸掉。原版算抗性时取 max(方块抗性, 流体抗性),水的流体抗性是 100,所以含水的台阶/珊瑚扇等炸不动;开启后水的流体抗性按 0 算。只负责这一件事 —— 高抗性方块(黑曜石/铁块等)请用 Carpet-AMS-Addition 的 enhancedWorldEater。默认关闭。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 信标效果解锁(客户端) (haoWackoBeacons)

解除信标的效果限制:Regeneration 可作主效果(配合副效果即 II 级),效果按钮不再按信标层数禁用(9 块钻石也能选 Strength)。移植自 wacko-beacons。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 岩浆中的深海探索者 (lavaDepthStrider)

开启后,玩家在岩浆中移动时不再受到岩浆减速:直接套用水中移动的公式(即 water_movement_efficiency 属性),手感与穿着深海探索者靴子在水中一致;附魔等级越高移速越快。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 末地门不传送 (noEndPortalTeleport)

开启后提供 /playerNoEndPortalTeleport 名单管理。规则本身不改变传送:把玩家加入黑名单或开启 globalMode 后,对应玩家才不会被末地传送门传送;非玩家实体不受影响。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 火箭潜影盒自动补给 (rocketShulker)

名称正好是 rocket 的潜影盒:指定格子里的烟花用光时会从盒内自动补一组(最多 64),用光再补。用 /rocketShulker offhand(默认) 或 /rocketShulker mainhand <1-9> 设置补给位置,只存内存,重启还原。目标格有别的物品时不会动它。默认关闭。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 雪地方解石 (snowyCalcite)

开启后,在降雪(雪地)生物群系中,岩浆与水的刷石机产物(石头/圆石)将变为方解石。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 陶瓦还原 (terracottaUncolor)

开启后,切石机可把 16 种染色陶瓦还原为原色陶瓦、16 种釉陶瓦还原为同色染色陶瓦;规则关闭时切石机不出现这些还原配方。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 潜影盒染色 (useDyeOnShulkerBox)

手持染料右键潜影盒可把它染成对应颜色;潜行手持仙人掌右键有色潜影盒可洗回无色(不消耗仙人掌)。染色/洗色都完整保留盒内物品与自定义名称。默认关闭。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

## 凋零骷髅掉落去除 (witherSkeletonDropReduction)

去除凋零骷髅的指定掉落物。选项:false=原版;bone=去骨头;coal=去煤炭;skull=去凋零骷髅头颅;sword=去掉落的手持石剑;all=去除以上全部。

- 类型: `String`
- 默认值: `false`
- 参考选项: `false`, `bone`, `coal`, `skull`, `sword`, `all`
- 分类: `Hao`

## 局部侦测器禁用 (zoneguard)

在 /zoneguard 配置的立方区域内禁用侦测器(观察者)行为。关闭规则会恢复区域内侦测器。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `Hao`

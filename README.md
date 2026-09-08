# Carpet-Hao-Addition

一个基于 [fabric-carpet-extension-example-mod](https://github.com/gnembon/fabric-carpet-extension-example-mod) 精简改造的
Fabric Carpet 多版本扩展模组。

内置功能:
- 示例规则(exampleBoolean / exampleString,见 `src/main/java/.../CarpetHaoAdditionSettings.java`);
- **zoneguard**:在 `/zoneguard` 配置的立方区域内禁用侦测器(观察者)行为,由地毯规则 `zoneguard` 开关(默认关闭,位于 `/carpet` 分类 Hao 下)。

- 目标版本: Minecraft **1.21.8** (Fabric)
- Java: **21**
- 依赖: [fabric-carpet](https://masa.dy.fi/maven/carpet/fabric-carpet/) `1.4.177+v250630` (即 `1.21.7-1.4.177+v250630`)

## 构建

需要 JDK 21(例如 `JAVA_HOME=F:\JDK-21`),然后:

```powershell
.\gradlew.bat build
```

根项目会构建全部受支持版本,并把产物汇总到 `build/libs/` 下,例如
`carpet-hao-addition-0.1.0+1.21.8.jar`。

启动指定版本的开发客户端:

```powershell
.\gradlew.bat :versions:1.21.8:runClient
```

## 结构

- `src/main/java/carpet_hao_addition/CarpetHaoAdditionExtension.java` — 所有版本共享的扩展入口(`ModInitializer` + `CarpetExtension`);
  在 `onInitialize()` 中通过 `CarpetServer.manageExtension(this)` 注册。
- `src/main/java/carpet_hao_addition/CarpetHaoAdditionSettings.java` — 共享规则类,字段加 `@Rule` 注解即成为本扩展的 Carpet 规则,
  游戏内用 `/haoaddition` 命令管理。
- `src/main/java/carpet_hao_addition/zoneguard/ZoneguardSettings.java` — 共享 zoneguard 规则(仅依赖 carpet API,不引用 Minecraft 版本类型)。
- `src/main/resources/fabric.mod.json` — 共享模组元数据(mod id: `carpet-hao-addition`)。
- `versions/1.21.8/` — Minecraft 1.21.8 的 Loom、映射和 Carpet 依赖配置;该目录也可放版本专用源码。
- `versions/1.21.8/src/` — **版本专用代码**:所有与 Minecraft 1.21.8 类型打交道的 zoneguard 实现
  (`carpet_hao_addition.zoneguard.{region,command,mixin}` 与 `zoneguard.mixins.json`)。
  新增 Minecraft 版本时,复制版本目录并适配这一层的 API 即可,共享层保持不变。

## zoneguard(侦测器禁用区域)

- 规则:注册在 Carpet 默认管理器,出现在 `/carpet` 的规则浏览/分类 **Hao** 下,
  `/carpet zoneguard true|false` 切换(默认 `false`,关闭时保持原版行为)。
- 命令(需为单机主人、OP ≥2 或本扩展白名单;`op` 子命令仅单机主人/OP):
  - `/zoneguard` / `/zoneguard view` — 列出已配置区域;
  - `/zoneguard set <序号> <起始坐标> <终点坐标>` — 设置一个立方禁用区域(含维度);
  - `/zoneguard clear <序号>` — 清除区域,并重启区域内"面对面"的侦测器对;
  - `/zoneguard op add|remove|list <玩家>` — 管理 ZoneGuard 权限白名单。
- 规则开启后,配置区域内的侦测器不再脉冲/更新/输出红石信号;区域与白名单随世界存档持久化
  (`zoneguard:detector_regions`)。
- 实现说明:判定与规则门控集中在 `ZoneguardState`(版本层),mixin 注入点覆盖 1.21.8
  `ObserverBlock` 的 scheduledTick/getStateForNeighborUpdate/updateNeighbors/强弱红石信号/
  onBlockAdded/onStateReplaced(参考实现中 26.2 独有的 ownSignal 在 1.21.8 不存在,其职责已由
  强弱信号注入覆盖,故未移植)。

## 修改指引

- 通用模组版本和构建配置放在根 `gradle.properties`。
- Minecraft、Yarn、Loader 和 Carpet 版本放在对应的 `versions/<版本>/gradle.properties`。
- 新增 Minecraft 版本时复制一个版本目录,修改其依赖,并在 `settings.gradle` 中注册新子项目。
- 规则改名或增删后,同步更新 `canHasTranslations(...)` 里的翻译键
  (格式:`{managerId}.rule.{ruleName}.name` / `.desc`、`{managerId}.category.{categoryId}`)。

## License

CC0-1.0,见 [LICENSE](LICENSE)。

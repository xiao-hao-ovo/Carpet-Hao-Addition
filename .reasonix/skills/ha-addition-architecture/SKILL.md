---
name: ha-addition-architecture
description: Carpet-Hao-Addition(carpet 附属 mod)的项目架构规范:多版本目录职责(共享核心 src 与 versions/<mc> 版本适配层)、模块拆分尺度(按功能拆 Settings/Hooks/Commands,解耦但不稀碎)、新增规则/功能/版本的落地流程、构建验证命令。加新功能或重构前先读这份。
---

# Carpet-Hao-Addition 架构规范

carpet 附属 mod,包 `carpet_hao_addition`,artifact `carpet-hao-addition`,mod_version 见根 `gradle.properties`(`mod_version`,jar 名 = `mod_version + mc版本`,如 `0.1.2+1.21.8`)。多版本同时构建:当前支持 MC `1.21.8`、`1.21.10`。

## 目录地图(职责边界)

```
Carpet-Hao-Addition/
├── gradle.properties          # loom_version / mod_version / maven_group 等(唯一版本真源)
├── settings.gradle            # include 'versions:1.21.8' 'versions:1.21.10'
├── src/main/                  # ★ 跨版本共享核心层
│   ├── java/carpet_hao_addition/
│   │   ├── CarpetHaoAdditionExtension.java   # CarpetExtension 入口:注册各 Settings、canHasTranslations、转发命令注册
│   │   ├── CarpetHaoAdditionSettings.java    # 自定义 manager(/haoaddition)示例规则
│   │   ├── <功能>Settings.java                # 每个规则一个 Settings 类(共享层)
│   │   └── <功能>/…                          # 复杂功能子包(见 zoneguard/)
│   └── resources/assets/carpet-hao-addition/ # 唯一 lang 真源(en_us.json / zh_cn.json)+ fabric.mod.json
└── versions/<mc>/             # ★ 版本适配层(每 MC 版本一个 loom subproject)
    ├── build.gradle           # sourceSets 合并 rootProject src/main/java + 自身 java
    └── src/main/java/carpet_hao_addition/    # mixin/、命令、hooks、需 MC 内部 API 的代码
```

**铁律:**
1. 共享层只允许依赖 Carpet API(`carpet.api.*`、`carpet.CarpetServer`)+ JDK,**禁止引用任何 `net.minecraft.*` 版本相关类型**;需要 MC 内部 API 一律下沉到 `versions/<mc>`。
2. 不把共享代码复制进版本层;版本差异用版本层文件承载,同名类按需在每版本各自实现(例:`SnowyCalciteHooks`、`ZoneguardCommands`)。
3. lang 与 fabric.mod.json 只放共享层,版本层不得再放资源副本(build 产物除外,不提交)。

## 规则与命令的注册位置

- **规则即 Settings 类里的 `@Rule` 静态字段**,两类注册方式并存:
  - 自定义 manager:`CarpetHaoAdditionExtension` 内 `settingsManager.parseSettingsClass(...)`,玩家用 `/haoaddition <规则> <值>`;lang 键前缀 `haoaddition.rule.*`。
  - Carpet 默认 manager:`CarpetServer.settingsManager.parseSettingsClass(...)`,规则归入分类 `Hao`,用 `/carpet <规则> true|false`;lang 键前缀 `carpet.rule.*` + `carpet.category.Hao`。
  - 判断规则是否开启**必须用 Settings 里提供的 `isEnabled()` 之类封装**(读 `@Rule` 静态字段不可靠,见 `ZoneguardSettings` javadoc)。
- **命令在版本层注册**(`versions/<mc>/src/.../<功能>Commands.java`),共享层 Extension 的 `registerCommands` 只做转发,避免把 MC 版本相关命令 API 拉进共享入口。

## 模块拆分尺度(解耦,但不稀碎)

- **简单规则 = 1 个 `<功能>Settings`(共享)+ 版本层若干 mixin/hooks**,不建多余子包。
- **复杂功能 = 功能子包**,参照 `zoneguard/`:`Settings`(规则定义 + isEnabled)+ 版本层 `Hooks` + `command/`(Commands/Permissions)+ `region/`(领域逻辑:State/SavedData/Detector 等)。
- 子包内再分目录的判据:同层文件约 5+ 且职责可一句话分开时才拆;**禁止**出现只含一个方法/一个字段的"碎片类"。
- 一个功能一个 Settings 类,Settings 类是纯规则声明与门控,业务逻辑放版本层 Hooks/子包,互不耦合。

## 新增功能/版本 checklist

新增一条规则:
1. 共享层建 `<功能>Settings.java`,按上面注册位置二选一;规则命名 camelCase。
2. 逻辑进版本层(每个支持版本都要有对应实现/mixin),或纯 Carpet API 可留在共享层。
3. lang 两文件 `en_us.json` + `zh_cn.json` 同步加键(见 i18n skill),同一次提交。
4. 编译验证:`.\gradlew.bat build`(或 `:versions:1.21.8:build` / `:versions:1.21.10:build` 单版本)。

新增 MC 版本:
1. `gradle.properties` 确认 loom/mc 版本;`settings.gradle` `include 'versions:<新mc>'`。
2. 复制最近版本的 `versions/<mc>/` 目录改版本号,逐个文件核对 API 差异(先看 mixin target、命令 API、`CarpetServer`/carpet API 版本)。
3. 所有支持版本都要能独立构建通过;共享层改动不得只在一版本验证。

构建约束:用仓库 `.\gradlew.bat`(系统无全局 gradle);改代码后跑构建/游戏内自测,没验证的改动不得声称通过。

## 改完代码后

不提交、不 push——把改动留给用户审核;用户明确说"提交"后才按 git skill 执行提交。

# Carpet-Hao-Addition

一个基于 Fabric 的 [Carpet](https://github.com/gnembon/fabric-carpet) 多版本扩展模组，为技术生存与原版友好玩法提供少量实用、可配置的地毯规则。

- 所有规则**默认关闭**，只有在 `/carpet` 中手动开启后才生效；默认配置下不改变原版行为。
- 只依赖 **Carpet + Fabric Loader**（刻意不依赖 fabric-api），可纯服务端使用。
- 全部规则注册在 Carpet 默认管理器，分类 **Hao（游戏内显示“昊”）**，文案支持中英双语（`en_us` / `zh_cn`）。

## 下载

| 平台 | 链接 |
|---|---|
| Modrinth | https://modrinth.com/mod/carpet-hao-addition |
| CurseForge | https://www.curseforge.com/minecraft/mc-mods/carpet-hao-addition |
| GitHub Releases | https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/releases |

> 每个 jar 只对应一个 Minecraft 版本，请下载与你游戏版本完全一致的那个；装错会报 `Incompatible mods found!`。

## 支持版本与依赖

| Minecraft | Yarn mappings | Fabric Loader | Carpet |
|---|---|---|---|
| 1.21 | 1.21+build.9 | 0.19.5（要求 ≥0.16.10） | 1.21-1.4.147+v240613 |
| 1.21.1 | 1.21.1+build.3 | 0.19.5（要求 ≥0.16.10） | 1.21-1.4.147+v240613（上游无专用 1.21.1,该版本声明 minecraft ">1.20.1"） |
| 1.21.2 | 1.21.2+build.1 | 0.19.5（要求 ≥0.16.10） | 1.21.2-1.4.158+v241022 |
| 1.21.4 | 1.21.4+build.8 | 0.19.5（要求 ≥0.16.10） | 1.21.4-1.4.161+v241203 |
| 1.21.6 | 1.21.6+build.1 | 0.19.5（要求 ≥0.16.10） | 1.21.6-1.4.176+v250617 |
| 1.21.8 | 1.21.8+build.1 | 0.19.5（要求 ≥0.16.10） | 1.21.7-1.4.177+v250630 |
| 1.21.10 | 1.21.10+build.3 | 0.19.5（要求 ≥0.16.10） | 1.21.10-1.4.188+v251016 |
| 1.21.11 | 1.21.11+build.6 | 0.19.5（要求 ≥0.16.10） | 1.21.11-1.4.194+v260107 |
| 26.1.2 | 无（26.x 起 Mojang 取消混淆，Fabric 停用 yarn） | 0.19.5+ | 26.1+v260401 |
| 26.2 | 无（26.x 起 Mojang 取消混淆，Fabric 停用 yarn） | 0.19.5+ | 26.2+v260616 |

- Java：21+
- 模组 id：`carpet-hao-addition`；Loom：1.17.20

## 规则列表

游戏内用 `/carpet <规则> <值>` 查看/切换（也可在 Carpet 的规则界面里操作）。

### 布尔规则

| 规则 | 默认 | 说明 |
|---|---|---|
| `zoneguard` | false | 在 `/zoneguard` 配置的立方区域内禁用侦测器（观察者）行为；关闭规则会恢复区域内侦测器 |
| `goldenCarrotCompost` | false | 手持金胡萝卜右键堆肥桶可 100% 堆肥（消耗与满桶流程同普通可堆肥物品） |
| `snowyCalcite` | false | 雪地刷石机产出方解石（雪/水与岩浆相接时生成方解石） |
| `noEndPortalTeleport` | false | 末地传送门传送控制：配合 `/playerNoEndPortalTeleport` 的名单与 `globalMode` 决定哪些玩家不被传送 |
| `terracottaUncolor` | false | 切石机把染色陶瓦 / 染色釉陶瓦还原为普通陶瓦；启用时自动向世界部署数据包配方并随规则启停 |
| `haoBedrockMines` | false | 基岩可被挖掘：默认按黑曜石硬度，掉落 1 块基岩；与 Carpet-AMS-Addition 的 `commandCustomBlockHardness` 同时开启时遵循其对 `minecraft:bedrock` 的自定义硬度 |

### 选项规则

| 规则 | 可选值 | 说明 |
|---|---|---|
| `witherSkeletonDropReduction` | `false` \| `bone` \| `coal` \| `skull` \| `sword` \| `all` | 自定义去除凋零骷髅掉落：骨头 / 煤炭 / 凋零骷髅头颅 / 掉落的手持石剑；`all` 为全部去除 |

### 示例规则

扩展自带的模板规则（归类 `haoaddition`，可用 `/haoaddition` 命令管理）：`exampleBoolean`、`exampleString`，仅作示例，可按需删除。

## 命令

### `/zoneguard`

- `/zoneguard set <id> <from> <to>` — 新增/覆盖一个立方区域（`id` 为整数，`from`/`to` 为方块坐标）
- `/zoneguard view` — 查看已配置区域
- `/zoneguard clear <id>` — 删除指定区域
- `/zoneguard op add|remove|list <player>` — 管理 ZoneGuard 权限（允许操作该命令的玩家）
- `/zoneguard help` — 帮助

> 规则关闭时命令不可见（切换规则后会向在线玩家重新推送命令树）。

### `/playerNoEndPortalTeleport`

- `/playerNoEndPortalTeleport add|remove <player>` — 名单增删
- `/playerNoEndPortalTeleport list` / `clear` — 查看 / 清空名单
- `/playerNoEndPortalTeleport globalMode [true|false]` — 查看 / 设置全局模式（true = 所有玩家都不被末地门传送）
- `/playerNoEndPortalTeleport help` — 帮助

## 构建

需要 JDK 21+（本仓库用 Java 21 目标，实测 Java 25 也可构建）：

```powershell
.\gradlew.bat build
```

26.x 使用**独立子工程** `modern/26.2/`（新版 Loom 插件 `net.fabricmc.fabric-loom`、不声明 mappings、Java 25）：

```powershell
cd modern.2
.\gradlew.bat build
```

1.21.x 产物汇总在根 `build/libs/`（每个受支持版本一个 jar）：

```
build/libs/carpet-hao-addition-0.1.2+1.21.jar
build/libs/carpet-hao-addition-0.1.2+1.21.1.jar
build/libs/carpet-hao-addition-0.1.2+1.21.2.jar
build/libs/carpet-hao-addition-0.1.2+1.21.4.jar
build/libs/carpet-hao-addition-0.1.2+1.21.6.jar
build/libs/carpet-hao-addition-0.1.2+1.21.8.jar
build/libs/carpet-hao-addition-0.1.2+1.21.10.jar
build/libs/carpet-hao-addition-0.1.2+1.21.11.jar
```

26.x 为独立子工程，需在各自目录构建，产物在 `modern/<mc>/build/libs/`：

```powershell
cd modern/26.2
.\gradlew.bat build
```

各版本独立构建也可用 `:versions:<mc>:build`。

### 开发运行

首次运行需在 `versions/<mc>/run/eula.txt` 写入 `eula=true`：

```powershell
.\gradlew.bat :versions:1.21.8:runServer
.\gradlew.bat :versions:1.21.8:runClient
```

## 发布

### GitHub Actions 自动发布

`.github/workflows/build.yml` 在推送 tag(如 `v0.1.4`)时会自动完成四件事:

1. JDK 21 构建 8 个 1.21.x 版本,JDK 25 构建 `modern/26.1.2` 与 `modern/26.2`;
2. 把 10 个 jar 作为 **GitHub Release** 资产上传;
3. 若配置了 `CURSEFORGE_TOKEN`,再把它们自动上传到 CurseForge 项目(默认 ID `1689732`,可用仓库变量 `CURSEFORGE_PROJECT_ID` 覆盖)。若同时配置了 `CURSEFORGE_CORE_KEY`,上传前会先用只读 Core API 查一遍项目里已有的文件名,已存在的直接跳过,避免重复上传;
4. 若配置了 `MODRINTH_TOKEN`,再把它们自动上传到 Modrinth 项目(https://modrinth.com/mod/carpet-hao-addition)。

三个平台互相独立:缺哪个 secret,就只对那个平台打一条 warning 跳过,不会让 workflow 失败。

发新版流程:

```powershell
# 1. 先把 gradle.properties 里的 mod_version 改成新版本(如 0.1.4)
# 2. 提交并推送
git add -A; git commit -m "chore: 0.1.4"; git push
# 3. 打 tag 触发自动构建 + 发布
git tag v0.1.4; git push origin v0.1.4
```

一次性配置(仓库 **Settings → Secrets and variables → Actions**):

- **Secrets** 新增 `MODRINTH_TOKEN`,值为 Modrinth PAT(`mrp_...`,在 https://modrinth.com/settings/pats 创建)。**创建时必须勾选权限**,至少要有 `USER_READ`、`PROJECT_CREATE`、`PROJECT_WRITE`、`VERSION_CREATE` —— 没有权限的 PAT 会被 API 一律拒绝(报 `Invalid Authentication Credentials`,和「token 不存在」是同一个错误码,很难排查);
- **Secrets** 新增 `CURSEFORGE_TOKEN`,值为 CurseForge **主站 API Token**(在 https://www.curseforge.com/account/api-tokens 创建；不是 console 的 Core API Key);
- **Secrets** 新增 `CURSEFORGE_CORE_KEY`(可选),值为 CurseForge **Core API Key**(`$2a$10$...`,在 https://console.curseforge.com 的 API Keys 页面创建)。只为上传前查重使用:Upload API 没有列文件的端点,只有 Core API 能查。不配它也能正常发布,只是重复上传时不会被自动拦下;
- 想换项目的话,在 **Variables** 新增 `CURSEFORGE_PROJECT_ID`(不配就用默认 `1689732`)。

### 本地手动上传

```powershell
# CurseForge
python tools/publish_curseforge.py versions --dry-run   # 先看会上传哪些 jar
python tools/publish_curseforge.py versions             # 上传仓库构建目录里的 jar(自动查重)
python tools/publish_curseforge.py versions --prefix 0.1.4   # 只传指定版本号的 jar
python tools/publish_curseforge.py versions --force     # 跳过查重,强制上传
python tools/publish_curseforge.py existing --prefix 0.1.3   # 列出项目里已存在的文件(核对重复)
python tools/publish_curseforge.py probe                 # 校验 token 并打印版本名->ID 映射

# Modrinth
python tools/publish_modrinth.py versions --dry-run     # 先看会上传哪些 jar(不需要凭据)
python tools/publish_modrinth.py versions               # 上传(项目不存在时会自动创建草稿)
python tools/publish_modrinth.py status                 # 打印项目与版本现状
python tools/publish_modrinth.py publish                # 把项目提交公开审核
```

凭据来源:

- CurseForge:环境变量 `CURSEFORGE_TOKEN` / `CURSEFORGE_PROJECT_ID`,或 `~/.secrets/curseforge-token.txt` 与 `~/.secrets/curseforge-project-id.txt`;
  - 查重另需只读 Core Key:环境变量 `CURSEFORGE_CORE_KEY`,或 `~/.secrets/curseforge-api-key.txt`。**两套凭据不能混用** —— 上传用主站 Token(`minecraft.curseforge.com/api`),查重只能用 Core Key(`api.curseforge.com/v1`);Core Key 缺失时脚本只打警告并跳过查重,仍会正常上传;
- Modrinth:环境变量 `MODRINTH_TOKEN`,或 `~/.secrets/modrinth-token.txt` / `~/.secrets/modrinth-pat.txt`。默认直连,需要代理时设 `MODRINTH_PROXY`(如 `http://127.0.0.1:7897`)。

> 上传时每个 jar 只勾它自己那一个 Minecraft 版本,一个版本只挂一个 jar —— 勾多了会让玩家装错并报 `Incompatible mods found!`。

## 项目结构

- `src/main/java/carpet_hao_addition/`
  - `CarpetHaoAdditionExtension.java` — 扩展入口（`ModInitializer` + `CarpetExtension`），解析各规则类、注册命令、处理规则变更回调
  - `*Settings.java` — 共享规则定义（仅依赖 carpet API，不引用 Minecraft 版本类型）
  - `portal/` — 末地门传送名单（`PlayerNoEndPortalTeleportList`）
- `src/main/resources/`
  - `fabric.mod.json` — 模组元数据与依赖
  - `assets/carpet-hao-addition/lang/{en_us,zh_cn}.json` — 全部用户可见文案（规则名/描述/命令消息）
- `versions/1.21.8/`、`versions/1.21.10/`
  - `gradle.properties` — 该版本的 minecraft / yarn / loader / carpet 版本
  - `src/main/java/carpet_hao_addition/` — 版本专用实现：mixin、`zoneguard/`、`portal/`、`RecipeDeployHooks` 等
  - `src/main/resources/carpet-hao-addition.mixins.json` — 该版本的 mixin 列表

根 `build.gradle` 汇总各版本子模块，版本层代码与共享层一起合并进对应版本的 jar。

## 开发注意事项

- **规则开关的权威读取**：使用各 `*Settings.isEnabled()` / `value()`（读取 Carpet 默认管理器中的规则值），不要直接读 `@Rule` 静态字段（carpet 新规则系统不保证回写字段）。
- **文案**：所有用户可见文本放 `assets/carpet-hao-addition/lang/*.json`（中英成对），Java 内零硬编码；聊天/命令消息在服务端渲染，兼容纯服务端与未装本模组的客户端。
- **Mixin 兼容性**：避免使用 `@Redirect`（旧版 mixinextras 0.5.4 会因 `FactoryRedirectWrapperMixinTransformer` 抛 `ClassCastException` 而崩溃），优先 `@ModifyArg` / `@WrapOperation` / `@Inject`。
- **版本 API 漂移按各自 jar 字节码确认**，例如：1.21.10 死亡掉落改走 `LivingEntity.generateLoot(...)`（而非 1.21.8 的 `dropLoot` 内直接调用）、1.21.10 的 `EndPortalBlock.onEntityCollision` 多一个 `boolean` 形参。
- 与其它 Carpet 扩展**同名规则会互相覆盖**（Carpet 默认管理器按规则名索引），新增规则请使用本模组自有命名（如 `haoBedrockMines`）。

## 致谢

- 基于 [fabric-carpet-extension-example-mod](https://github.com/gnembon/fabric-carpet-extension-example-mod) 改造。
- 部分规则思路参考社区扩展：Carpet-AMS-Addition、Carpet-FGA-Addition 等。

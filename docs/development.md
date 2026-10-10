# 开发

面向开发者的构建、发布与实现注意事项。规则和命令的用户文档见 [规则](rules.md) / [命令](commands.md)。

## 构建

需要 JDK 21+（本仓库用 Java 21 目标，实测 Java 25 也可构建）：

```powershell
.\gradlew.bat build
```

26.x 使用**独立子工程** `modern/26.1.2`、`modern/26.2`、`modern/26.3`（新版 Loom 插件 `net.fabricmc.fabric-loom`、不声明 mappings、Java 25）：

```powershell
cd modern/26.3
.\gradlew.bat build
```

1.21.x 产物在各版本目录的 `build/libs/`（每个受支持版本一个 jar）：

```
versions/1.21.8/build/libs/carpet-hao-addition-0.2.3+1.21.8.jar
versions/1.21.10/build/libs/carpet-hao-addition-0.2.3+1.21.10.jar
...
```

各版本也可单独构建（`1.21` 与 `1.21.1` 因项目名前缀歧义被重命名为 `mc-1.21` / `mc-1.21.1`）：

```powershell
.\gradlew.bat :versions:1.21.8:build
.\gradlew.bat :versions:mc-1.21.1:build
```

> 一次跑 `.\gradlew.bat build`（八层并行）时 `remapJar` 有概率报 `Failed to create service instance` 而失败；**逐层单独构建更稳**。
>
> 另外 `BUILD SUCCESSFUL` 不等于 jar 已落盘：请到 `versions/<mc>/build/libs/` 确认 `carpet-hao-addition-<版本>+<mc>.jar` 存在且时间戳更新。`build/devlibs/*-dev.jar` 是未 remap 的产物，不能作为发布物。

### 开发运行

首次运行需在 `versions/<mc>/run/eula.txt` 写入 `eula=true`：

```powershell
.\gradlew.bat :versions:1.21.8:runServer
.\gradlew.bat :versions:1.21.8:runClient
```

## 发布

### GitHub Actions 自动发布

`.github/workflows/build.yml` 在推送 tag(如 `v0.2.3`)时会自动完成四件事:

1. JDK 21 构建 8 个 1.21.x 版本,JDK 25 构建 `modern/26.1.2`、`modern/26.2` 与 `modern/26.3`;
2. 把 11 个 jar 作为 **GitHub Release** 资产上传;
3. 若配置了 `CURSEFORGE_TOKEN`,再把它们自动上传到 CurseForge 项目(默认 ID `1689732`,可用仓库变量 `CURSEFORGE_PROJECT_ID` 覆盖)。若同时配置了 `CURSEFORGE_CORE_KEY`,上传前会先用只读 Core API 查一遍项目里已有的文件名,已存在的直接跳过,避免重复上传;
4. 若配置了 `MODRINTH_TOKEN`,再把它们自动上传到 Modrinth 项目(https://modrinth.com/mod/carpet-hao-addition)。

推送到 `main` 分支只触发**构建**，不会发布 —— `publish` job 的条件是 `if: startsWith(github.ref, 'refs/tags/')`。

三个平台互相独立:缺哪个 secret,就只对那个平台打一条 warning 跳过,不会让 workflow 失败。

发新版流程:

```powershell
# 1. 先把 gradle.properties 里的 mod_version 改成新版本(如 0.2.4,共 4 处:根 + modern/26.1.2 + modern/26.2 + modern/26.3)
# 2. 提交并推送
git add -A; git commit -m "chore: 0.2.4"; git push
# 3. 打 tag 触发自动构建 + 发布
git tag v0.2.4; git push origin v0.2.4
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
python tools/publish_curseforge.py versions --prefix 0.2.3   # 只传指定版本号的 jar
python tools/publish_curseforge.py versions --force     # 跳过查重,强制上传
python tools/publish_curseforge.py existing --prefix 0.2.3   # 列出项目里已存在的文件(核对重复)
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
- `versions/<mc>/`（1.21.x 各层）
  - `gradle.properties` — 该版本的 minecraft / yarn / loader / carpet 版本
  - `src/main/java/carpet_hao_addition/` — 版本专用实现：mixin、`zoneguard/`、`portal/`、`RecipeDeployHooks` 等
  - `src/main/resources/carpet-hao-addition.mixins.json` — 该版本的 mixin 列表
- `modern/<mc>/`（26.x 三层，独立子工程）

根 `build.gradle` 汇总各版本子模块，版本层代码与共享层一起合并进对应版本的 jar。

## 开发注意事项

- **规则开关的权威读取**：使用各 `*Settings.isEnabled()` / `value()`（读取 Carpet 默认管理器中的规则值），不要直接读 `@Rule` 静态字段（carpet 新规则系统不保证回写字段）。
- **文案**：所有用户可见文本放 `assets/carpet-hao-addition/lang/*.json`（中英成对），Java 内零硬编码；聊天/命令消息在服务端渲染，兼容纯服务端与未装本模组的客户端。
- **Mixin 兼容性**：避免使用 `@Redirect`（旧版 mixinextras 0.5.4 会因 `FactoryRedirectWrapperMixinTransformer` 抛 `ClassCastException` 而崩溃），优先 `@ModifyArg` / `@WrapOperation` / `@Inject`。
- **版本 API 漂移按各自 jar 字节码确认**，例如：1.21.10 死亡掉落改走 `LivingEntity.generateLoot(...)`（而非 1.21.8 的 `dropLoot` 内直接调用）、1.21.10 的 `EndPortalBlock.onEntityCollision` 多一个 `boolean` 形参。
- 与其它 Carpet 扩展**同名规则会互相覆盖**（Carpet 默认管理器按规则名索引），新增规则请使用本模组自有命名（如 `haoBedrockMines`）。
- **改规则集后请同步** `docs/rules.md` / `docs/rules_en.md`（模板见 [new-rule-template.md](new-rule-template.md)）。

## 许可与致谢

本项目以 **GNU LGPL-3.0** 授权，见 [LICENSE](../LICENSE)。

- 基于 [fabric-carpet-extension-example-mod](https://github.com/gnembon/fabric-carpet-extension-example-mod) 改造。
- 部分规则思路参考社区扩展：Carpet-AMS-Addition、Carpet-FGA-Addition 等。

---
name: ha-addition-git
description: Carpet-Hao-Addition 的 git 提交规范:改完代码一律不提交不 push,等用户审核后明确指示"提交"才提交;提交信息用中文总结内容(动词开头+冒号展开,沿用现状风格);原子提交,规则功能与其 i18n 键(en_us/zh_cn)同一次提交;永不提交 build/ .gradle/ run/ .idea/ reasonix.toml。commit 前先读这份。
---

# Carpet-Hao-Addition Git 提交规范

## 核心铁律(最高优先级)

1. **每次改完代码后不提交、不 push、不 stage。** 改动留在工作区给用户审核。
2. **只有用户明确说"提交"之后**才执行 git 提交;用户说提交哪个范围就提交哪个范围。
3. 用户没提"提交"时,即使构建/测试通过也保持工作区原样,允许存在未提交改动。
4. 收到提交指令后,先 `git --no-pager status --short` + `git --no-pager diff --stat` 复核改动范围,确认无多余文件再提交。

## 提交信息格式(中文总结内容,沿用现状风格)

- 全部用中文写;动词/祈使开头概括做了什么,冒号后展开关键细节。
- 风格对照现有历史(禁止发明前缀符号):

```
新增规则 snowyCalcite:雪地群系刷石机产出方解石
zoneguard:新增 /zoneguard help 用法说明并补充注释,版本升至 0.1.2
新增 Minecraft 1.21.10 版本支持,构建 0.1.2+1.21.10 jar
noEndPortalTeleport 增加黑名单 + globalMode,新增 /playerNoEndPortalTeleport(仿 AMS)
```

- 单行放不下时用 body 补细节(中文,写动机/仿照对象/影响范围),首行保持 ≤ 50 字左右概括。

## 原子提交粒度

- 一个提交只做一件事:**一条规则 / 一个功能 / 一次版本适配 / 一次独立修复**。
- **规则功能提交必须包含该规则的 i18n 键,且 `en_us.json` 与 `zh_cn.json` 同步在同一提交内**;不允许代码与翻译分两次提交,更不允许只提交一份语言文件。
- 跨版本改动(共享层 + 多个 `versions/<mc>` 实现)属于同一件事,合成一个提交,不按目录拆。
- 新增 MC 版本 = 独立提交(`settings.gradle` include + `versions/<mc>/` + 版本参数调整一起)。
- 版本号 bump(`gradle.properties` 的 `mod_version`,影响 jar 名如 `0.1.2+1.21.8`):随同功能提交并在信息里体现,或独立 bump 提交,二选一但信息必须写清版本变化。

## 永不提交的内容

`.gitignore` 已覆盖以下项,提交前仍要肉眼确认没有被误 `git add` 进来:

- `build/`、`.gradle/`、`out/`、`classes/`(构建产物,含 `versions/*/build/`)
- `run/`(游戏运行目录)
- `.idea/`、`*.iml` 等 IDE 文件
- `reasonix.toml`(会话配置,不入库)

提交用显式文件/目录路径,避免无脑 `git add .` 带进产物;`git status --short` 出现上述路径即视为违规。

## 提交流程(收到"提交"指令后执行)

```powershell
git --no-pager status --short        # 复核改动范围
git --no-pager diff --stat           # 概览
git add <显式路径…>                   # 只加本次要提交的文件
git commit -m "中文总结内容"           # 多行正文用 git commit 编辑器
git --no-pager log --oneline -3      # 确认提交成功
```

不 push,除非用户同时说"推送"。用户审核的是工作区改动本身,提交只是把已审核内容落库。

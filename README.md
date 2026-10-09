# Carpet-Hao-Addition

[![License](https://img.shields.io/github/license/xiao-hao-ovo/Carpet-Hao-Addition)](LICENSE)
[![Modrinth](https://img.shields.io/modrinth/dt/carpet-hao-addition?color=00AF5C&label=Modrinth%20downloads&logo=modrinth)](https://modrinth.com/mod/carpet-hao-addition)
[![CurseForge](https://img.shields.io/curseforge/dt/1689732?logo=curseforge&label=CurseForge%20downloads&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/carpet-hao-addition)
[![MC Versions](https://cf.way2muchnoise.eu/versions/For%20MC_1689732_all.svg)](https://www.curseforge.com/minecraft/mc-mods/carpet-hao-addition)
[![GitHub downloads](https://img.shields.io/github/downloads/xiao-hao-ovo/Carpet-Hao-Addition/total?color=161616&label=GitHub%20downloads&logo=github)](https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/releases)
[![Build](https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/actions/workflows/build.yml/badge.svg)](https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/actions/workflows/build.yml)

**中文** | [English](README_en.md)

基于 Fabric 的 [Carpet](https://github.com/gnembon/fabric-carpet) 多版本扩展模组，为技术生存与原版友好玩法提供少量实用、可配置的地毯规则。

- 所有规则**默认关闭**，在 `/carpet` 中手动开启后才生效。
- 只依赖 **Carpet + Fabric Loader**，刻意不依赖 fabric-api，可纯服务端使用。
- 全部规则注册在 Carpet 默认管理器，分类 **Hao（游戏内显示“昊”）**，文案中英双语。

## 依赖

| 名称 | 类型 | 链接 | 备注 |
|----|----|----|----|
| Carpet | 必须 | [Modrinth](https://modrinth.com/mod/carpet) &#124; [GitHub](https://github.com/gnembon/fabric-carpet) | |
| Fabric Loader | 必须 | [官网](https://fabricmc.net/) | ≥0.16.10 |
| Fabric API | 不需要 | - | 刻意不依赖，可纯服务端使用 |

## 版本支持

| 游戏版本 | 状态 |
|----|----|
| 1.21 / 1.21.1 / 1.21.2 / 1.21.4 / 1.21.6 | 维护中 |
| 1.21.8 / 1.21.10 / 1.21.11 | 维护中 |
| 26.1.2 / 26.2 / 26.3 | 维护中 |

Java 21+（26.x 需要 Java 25）。

## 文档

- [规则](docs/rules.md) —— 全部规则的类型、默认值、参考选项与说明
- [命令](docs/commands.md) —— 各命令的语法与效果
- [开发](docs/development.md) —— 构建、发布、项目结构与注意事项

游戏内用 `/carpet <规则> <值>` 查看/切换规则（也可在 Carpet 的规则界面里操作）。

## 下载

- [Modrinth](https://modrinth.com/mod/carpet-hao-addition)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/carpet-hao-addition)
- [GitHub Releases](https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/releases)

> 每个 jar 只对应一个 Minecraft 版本，请下载与你游戏版本完全一致的那个；装错会报 `Incompatible mods found!`。

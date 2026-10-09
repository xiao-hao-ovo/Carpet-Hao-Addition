# Carpet-Hao-Addition

[![License](https://img.shields.io/github/license/xiao-hao-ovo/Carpet-Hao-Addition)](LICENSE)
[![Modrinth](https://img.shields.io/modrinth/dt/carpet-hao-addition?color=00AF5C&label=Modrinth%20downloads&logo=modrinth)](https://modrinth.com/mod/carpet-hao-addition)
[![CurseForge](https://img.shields.io/curseforge/dt/1689732?logo=curseforge&label=CurseForge%20downloads&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/carpet-hao-addition)
[![MC Versions](https://cf.way2muchnoise.eu/versions/For%20MC_1689732_all.svg)](https://www.curseforge.com/minecraft/mc-mods/carpet-hao-addition)
[![GitHub downloads](https://img.shields.io/github/downloads/xiao-hao-ovo/Carpet-Hao-Addition/total?color=161616&label=GitHub%20downloads&logo=github)](https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/releases)
[![Build](https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/actions/workflows/build.yml/badge.svg)](https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/actions/workflows/build.yml)

[中文](README.md) | **English**

A multi-version [Carpet](https://github.com/gnembon/fabric-carpet) extension mod for Fabric, adding a small set of practical, configurable rules for technical survival and vanilla-friendly play.

- Every rule is **off by default** and only takes effect once enabled via `/carpet`.
- Depends only on **Carpet + Fabric API**, no other prerequisites; it works on a pure server.
- All rules live in Carpet's default settings manager under the category **Hao**, with bilingual text.

## Dependencies

| Name | Type | Link | Note |
|----|----|----|----|
| Carpet | Required | [Modrinth](https://modrinth.com/mod/carpet) &#124; [GitHub](https://github.com/gnembon/fabric-carpet) | |
| Fabric Loader | Required | [Website](https://fabricmc.net/) | ≥0.16.10 |
| Fabric API | Required | [Modrinth](https://modrinth.com/mod/fabric-api) &#124; [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api) | Needed for custom packets and per-tick callbacks |

## Supported versions

| Minecraft | Status |
|----|----|
| 1.21 / 1.21.1 / 1.21.2 / 1.21.4 / 1.21.6 | Maintained |
| 1.21.8 / 1.21.10 / 1.21.11 | Maintained |
| 26.1.2 / 26.2 / 26.3 | Maintained |

Java 21+ (Java 25 for 26.x).

## Documentation

- [Rules](docs/rules_en.md) — type, default, options and description of every rule
- [Commands](docs/commands_en.md) — syntax and effect of every command
- [Development](docs/development_en.md) — building, publishing, project layout and notes

In game, use `/carpet <rule> <value>` to view/toggle rules (or use Carpet's rule screen).

## Download

- [Modrinth](https://modrinth.com/mod/carpet-hao-addition)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/carpet-hao-addition)
- [GitHub Releases](https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/releases)

> Each jar targets exactly one Minecraft version. Download the one matching your game version; a mismatch reports `Incompatible mods found!`.

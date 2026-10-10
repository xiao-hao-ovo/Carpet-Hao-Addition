# Development

Building, publishing and implementation notes for developers. User-facing docs for rules and commands live in [Rules](rules_en.md) / [Commands](commands_en.md).

## Building

JDK 21+ required (the repo targets Java 21; Java 25 also builds fine):

```powershell
.\gradlew.bat build
```

26.x lives in **separate sub-projects** `modern/26.1.2`, `modern/26.2` and `modern/26.3` (new Loom plugin `net.fabricmc.fabric-loom`, no mappings declared, Java 25):

```powershell
cd modern/26.3
.\gradlew.bat build
```

1.21.x artifacts land in each version's `build/libs/` (one jar per supported version):

```
versions/1.21.8/build/libs/carpet-hao-addition-0.2.3+1.21.8.jar
versions/1.21.10/build/libs/carpet-hao-addition-0.2.3+1.21.10.jar
...
```

Each version can also be built on its own (`1.21` and `1.21.1` are renamed to `mc-1.21` / `mc-1.21.1` to avoid project-name prefix ambiguity):

```powershell
.\gradlew.bat :versions:1.21.8:build
.\gradlew.bat :versions:mc-1.21.1:build
```

> Running `.\gradlew.bat build` (all eight layers in parallel) occasionally fails in `remapJar` with `Failed to create service instance`; **building layer by layer is more reliable**.
>
> Also note that `BUILD SUCCESSFUL` does not mean the jar was written: check that `versions/<mc>/build/libs/carpet-hao-addition-<version>+<mc>.jar` exists with a fresh timestamp. `build/devlibs/*-dev.jar` is the un-remapped artifact and must not be shipped.

### Development run

On the first run, write `eula=true` into `versions/<mc>/run/eula.txt`:

```powershell
.\gradlew.bat :versions:1.21.8:runServer
.\gradlew.bat :versions:1.21.8:runClient
```

## Publishing

### Automated publishing via GitHub Actions

Pushing a tag (e.g. `v0.2.3`) makes `.github/workflows/build.yml` do four things:

1. Build the 8 versions of 1.21.x on JDK 21, and `modern/26.1.2`, `modern/26.2`, `modern/26.3` on JDK 25;
2. upload the 11 jars as **GitHub Release** assets;
3. if `CURSEFORGE_TOKEN` is set, upload them to the CurseForge project (default ID `1689732`, overridable with the repository variable `CURSEFORGE_PROJECT_ID`). If `CURSEFORGE_CORE_KEY` is also set, existing filenames are checked first via the read-only Core API and skipped, avoiding duplicates;
4. if `MODRINTH_TOKEN` is set, upload them to the Modrinth project (https://modrinth.com/mod/carpet-hao-addition).

Pushing to `main` only triggers a **build**, never a publish — the `publish` job is gated by `if: startsWith(github.ref, 'refs/tags/')`.

The three platforms are independent: a missing secret only warns for that platform and never fails the workflow.

Release flow:

```powershell
# 1. bump mod_version in gradle.properties (4 places: root + modern/26.1.2 + modern/26.2 + modern/26.3)
# 2. commit and push
git add -A; git commit -m "chore: 0.2.4"; git push
# 3. tag to trigger the automated build + publish
git tag v0.2.4; git push origin v0.2.4
```

One-time setup (repository **Settings → Secrets and variables → Actions**):

- **Secrets**: add `MODRINTH_TOKEN` (a Modrinth PAT, `mrp_...`, created at https://modrinth.com/settings/pats). **You must tick permissions** — at least `USER_READ`, `PROJECT_CREATE`, `PROJECT_WRITE`, `VERSION_CREATE`. A PAT without them is rejected with `Invalid Authentication Credentials`, which is the same error code as "token does not exist" and is hard to diagnose;
- **Secrets**: add `CURSEFORGE_TOKEN` (the CurseForge **site** API Token, created at https://www.curseforge.com/account/api-tokens — not the console Core API Key);
- **Secrets**: add `CURSEFORGE_CORE_KEY` (optional) — the CurseForge **Core API Key** (`$2a$10$...`, created on the API Keys page of https://console.curseforge.com). Used only for duplicate checking before upload: the Upload API has no file-listing endpoint, only the Core API does. Publishing works without it; duplicates simply will not be caught automatically;
- To target another project, add the **Variables** entry `CURSEFORGE_PROJECT_ID` (defaults to `1689732`).

### Manual local upload

```powershell
# CurseForge
python tools/publish_curseforge.py versions --dry-run   # see which jars would be uploaded
python tools/publish_curseforge.py versions             # upload jars from the build dirs (dedup on)
python tools/publish_curseforge.py versions --prefix 0.2.3   # only jars of that version
python tools/publish_curseforge.py versions --force     # skip dedup, force upload
python tools/publish_curseforge.py existing --prefix 0.2.3   # list files already on the project
python tools/publish_curseforge.py probe                 # validate the token, print version-name -> ID map

# Modrinth
python tools/publish_modrinth.py versions --dry-run     # see which jars would be uploaded (no credentials needed)
python tools/publish_modrinth.py versions               # upload (creates a draft project if missing)
python tools/publish_modrinth.py status                 # print project and version status
python tools/publish_modrinth.py publish                # submit the project for public review
```

Credential sources:

- CurseForge: environment variables `CURSEFORGE_TOKEN` / `CURSEFORGE_PROJECT_ID`, or `~/.secrets/curseforge-token.txt` and `~/.secrets/curseforge-project-id.txt`;
  - dedup needs the read-only Core Key: environment variable `CURSEFORGE_CORE_KEY`, or `~/.secrets/curseforge-api-key.txt`. **The two credential sets cannot be mixed** — uploads use the site Token (`minecraft.curseforge.com/api`), dedup only works with the Core Key (`api.curseforge.com/v1`). If the Core Key is missing the script only warns and skips dedup, still uploading normally;
- Modrinth: environment variable `MODRINTH_TOKEN`, or `~/.secrets/modrinth-token.txt` / `~/.secrets/modrinth-pat.txt`. Direct connection by default; set `MODRINTH_PROXY` when a proxy is needed (e.g. `http://127.0.0.1:7897`).

> Each jar is tagged with exactly its own Minecraft version and one version carries exactly one jar — tagging extra versions makes players install the wrong file and hit `Incompatible mods found!`.

## Project layout

- `src/main/java/carpet_hao_addition/`
  - `CarpetHaoAdditionExtension.java` — extension entry point (`ModInitializer` + `CarpetExtension`): parses the rule classes, registers commands, handles rule-change callbacks
  - `*Settings.java` — shared rule definitions (carpet API only, no Minecraft version types)
  - `portal/` — end-portal teleport list (`PlayerNoEndPortalTeleportList`)
- `src/main/resources/`
  - `fabric.mod.json` — mod metadata and dependencies
  - `assets/carpet-hao-addition/lang/{en_us,zh_cn}.json` — all user-visible text (rule names/descriptions/command messages)
- `versions/<mc>/` (the 1.21.x layers)
  - `gradle.properties` — minecraft / yarn / loader / carpet versions for that layer
  - `src/main/java/carpet_hao_addition/` — version-specific implementation: mixins, `haoZoneguard/`, `portal/`, `RecipeDeployHooks`, ...
  - `src/main/resources/carpet-hao-addition.mixins.json` — that layer's mixin list
- `modern/<mc>/` (the three 26.x sub-projects)

The root `build.gradle` aggregates the version sub-modules; layer code is merged together with the shared layer into each version's jar.

## Development notes

- **Authoritative rule reads**: use each `*Settings.isEnabled()` / `value()` (which read the rule value from Carpet's default settings manager) instead of reading the `@Rule` static field directly — Carpet's newer rule system does not guarantee writing the field back.
- **Text**: all user-visible strings live in `assets/carpet-hao-addition/lang/*.json` (paired zh/en), with zero hard-coded strings in Java; chat/command messages are rendered server-side, so pure servers and clients without this mod both work.
- **Mixin compatibility**: avoid `@Redirect` (mixinextras 0.5.4 crashes with a `ClassCastException` from `FactoryRedirectWrapperMixinTransformer`); prefer `@ModifyArg` / `@WrapOperation` / `@Inject`.
- **Verify version API drift against each jar's bytecode**, e.g. 1.21.10 routes death loot through `LivingEntity.generateLoot(...)` (not the direct call inside `dropLoot` as in 1.21.8), and 1.21.10's `EndPortalBlock.onEntityCollision` takes an extra `boolean`.
- Rules with the same name as another Carpet extension **override each other** (Carpet's default manager indexes by rule name), so new rules should use this mod's own naming (e.g. `haoBedrockMines`).
- **After changing the rule set, update** `docs/rules.md` / `docs/rules_en.md` (template: [new-rule-template.md](new-rule-template.md)).

## License & Credits

This project is licensed under the **GNU LGPL-3.0** — see [LICENSE](../LICENSE).

- Based on [fabric-carpet-extension-example-mod](https://github.com/gnembon/fabric-carpet-extension-example-mod).
- Some rule ideas reference community extensions: 其它扩展, 其它扩展, and others.

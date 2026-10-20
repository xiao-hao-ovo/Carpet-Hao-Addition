#!/usr/bin/env python3
"""从源码树重新生成 docs/versions.md 与 docs/versions_en.md。

包结构或文件行数变了就跑一次，免得手写清单过时。
行数用 len(text.split("\n"))，文件以换行结尾时比 wc -l 多 1。

用法（仓库根目录）：
    python tools/gen_versions_doc.py
"""

import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIP_DIRS = {".git", "build", ".gradle", "run", ".idea", ".reasonix"}

# 包（相对 carpet_hao_addition 的第一段；"" 表示根包）-> (中文说明, 英文说明)
GROUP_LABELS = {
    "": ("根包 · 规则设置与跨层实现", "Root package · rule settings and cross-version helpers"),
    "mixin": ("Mixin · 其余规则", "Mixins · other rules"),
    "mixin.easyplace": ("轻松放置 · Mixin", "Easy place · mixins"),
    "easyplace": ("轻松放置 · 协议与规则编解码", "Easy place · protocol and rule codecs"),
    "entity": ("实体放置 · 生成与统计", "Entity placement · spawning and counting"),
    "waterlogged": ("含水填充 · 补水与流体", "Waterlogged fill · fluid top-up"),
    "recipe": ("配方部署 · 石切机数据包", "Recipe deploy · stonecutter datapacks"),
    "directdrops": ("直掉 · 掉落上下文", "Direct drops · drop context"),
    "client": ("客户端 · 界面与材料计算", "Client · screens and material math"),
    "zoneguard": ("区域守卫 · 区域与指令", "Zone guard · regions and commands"),
    "portal": ("末地门 · 传送名单", "End portal · teleport allowlist"),
}

GROUP_ORDER = ["", "mixin", "mixin.easyplace", "easyplace", "entity", "waterlogged",
               "recipe", "directdrops", "client", "zoneguard", "portal"]


def count_lines(path):
    with open(path, encoding="utf-8", errors="replace") as f:
        return len(f.read().split("\n"))


def scan_java_dir(base):
    """返回 {分组键: [(文件名, 行数)]}，分组键是相对 carpet_hao_addition 的首段包名。"""
    pkg_root = os.path.join(base, "carpet_hao_addition")
    groups = {}
    if not os.path.isdir(pkg_root):
        return groups
    for dp, dn, fn in os.walk(pkg_root):
        dn[:] = sorted(d for d in dn if d not in SKIP_DIRS)
        for f in sorted(fn):
            if not f.endswith(".java"):
                continue
            p = os.path.join(dp, f)
            rel = os.path.relpath(dp, pkg_root).replace("\\", "/")
            key = "" if rel == "." else rel
            # 归到顶层段：zoneguard/region -> zoneguard；mixin/easyplace 保留两级
            if key.startswith("mixin/"):
                key = "mixin.easyplace" if key.startswith("mixin/easyplace") else "mixin"
            elif "/" in key:
                key = key.split("/")[0]
            groups.setdefault(key, []).append((f, count_lines(p)))
    for k in groups:
        groups[k].sort()
    return groups


def ordered_keys(groups):
    out = [k for k in GROUP_ORDER if k in groups]
    out += sorted(k for k in groups if k not in GROUP_ORDER)
    return out


def emit_groups(groups, zh):
    """输出一个层里所有分组（中文说明 + 表格）。"""
    lines = []
    idx = 0 if zh else 1
    for k in ordered_keys(groups):
        files = groups[k]
        total = sum(n for _, n in files)
        label = GROUP_LABELS.get(k, (k or "root", k or "root"))[idx]
        if zh:
            lines.append("**%s**（%d 个，%d 行）" % (label, len(files), total))
        else:
            lines.append("**%s** (%d files, %d lines)" % (label, len(files), total))
        lines.append("")
        lines.append("| 文件 | 行数 |" if zh else "| File | Lines |")
        lines.append("|---|---:|")
        for f, n in files:
            lines.append("| `%s` | %d |" % (f, n))
        lines.append("")
    return "\n".join(lines)


def build(zh):
    versions = sorted(d for d in os.listdir(os.path.join(ROOT, "versions"))
                      if os.path.isdir(os.path.join(ROOT, "versions", d)))
    moderns = sorted(d for d in os.listdir(os.path.join(ROOT, "modern"))
                     if os.path.isdir(os.path.join(ROOT, "modern", d)))

    shared = scan_java_dir(os.path.join(ROOT, "src", "main", "java"))
    shared_total = sum(len(v) for v in shared.values())

    ver_data = {}
    for v in versions:
        g = scan_java_dir(os.path.join(ROOT, "versions", v, "src", "main", "java"))
        ver_data[v] = (g, sum(len(x) for x in g.values()))
    mod_data = {}
    for m in moderns:
        g = scan_java_dir(os.path.join(ROOT, "modern", m, "src", "main", "java"))
        mod_data[m] = (g, sum(len(x) for x in g.values()))

    L = []
    if zh:
        L.append("# 各版本文件清单\n")
        L.append("> 由 `tools/gen_versions_doc.py` 从源码树生成，请勿手改。行数按 `len(text.split(\"\\n\"))` 计。\n")
        L.append("## 一、仓库结构总览\n")
        L.append("```")
        L.append("Carpet-Hao-Addition/")
        L.append("├─ src/main/java/            共享源码 %d 个 java（所有版本都用）" % shared_total)
        L.append("├─ src/main/resources/       共享资源（lang 必须放这里）")
        L.append("├─ versions/                 1.21.x 各层 %d 个：%s" % (len(versions), ", ".join(versions)))
        L.append("│    └─ <版本>/src/main/java/   该层的完整实现")
        L.append("├─ modern/                   26.x 各层 %d 个：%s（独立构建）" % (len(moderns), ", ".join(moderns)))
        L.append("├─ docs/                     文档")
        L.append("└─ tools/                    构建与发布脚本")
        L.append("```\n")
        L.append("### 构建模型：共享 + 覆盖\n")
        L.append("`versions/<版本>/build.gradle` 把 **两处** 源码合到一起编译：\n")
        L.append("```groovy")
        L.append("sourceSets.main.java.srcDirs = [")
        L.append("    rootProject.file('src/main/java'),   // 共享：规则设置、命令入口")
        L.append("    file('src/main/java')                // 本层：完整实现")
        L.append("]")
        L.append("duplicatesStrategy = EXCLUDE             // 同名文件整份覆盖")
        L.append("```\n")
        L.append("于是只有两种文件：\n")
        L.append("| 类型 | 放在哪 | 说明 |")
        L.append("|---|---|---|")
        L.append("| **共享** | `src/main/java` | 规则设置类、命令、跨层工具。所有版本共用一份 |")
        L.append("| **本层** | `versions/<版本>/src/main/java` | 各版本 API 不同，必须逐层实现 |\n")
        L.append("> ⚠️ `lang` 等资源**必须**放共享的 `src/main/resources`。放层内不会按 key 合并，")
        L.append("> 而是**整份覆盖**，会丢掉共享里其他 key。\n")
        L.append("## 二、共享源码（`src/main/java`，%d 个）\n" % shared_total)
        L.append(emit_groups(shared, True))
        L.append("## 三、1.21.x 各层\n")
        L.append("每层都是**完整实现**，不依赖增量补丁——改功能要逐层改。\n")
        L.append("| 版本 | 本层 java 数 |")
        L.append("|---|---:|")
        for v in versions:
            L.append("| `%s` | %d |" % (v, ver_data[v][1]))
        L.append("\n### 各层文件明细\n")
        for v in versions:
            g, n = ver_data[v]
            L.append("<details>")
            L.append("<summary><b>%s</b>（%d 个 java）</summary>\n" % (v, n))
            L.append(emit_groups(g, True))
            L.append("</details>\n")
        L.append("## 四、26.x 各层（`modern/`）\n")
        L.append("26.x 是**独立构建**，不与 `versions/` 共用 gradle 配置。\n")
        L.append("| 版本 | 本层 java 数 |")
        L.append("|---|---:|")
        for m in moderns:
            L.append("| `%s` | %d |" % (m, mod_data[m][1]))
        L.append("")
        for m in moderns:
            g, n = mod_data[m]
            L.append("<details>")
            L.append("<summary><b>%s</b>（%d 个 java）</summary>\n" % (m, n))
            L.append(emit_groups(g, True))
            L.append("</details>\n")
    else:
        L.append("# Per-version file inventory\n")
        L.append("> Generated by `tools/gen_versions_doc.py`; do not edit by hand. Line count is `len(text.split(\"\\n\"))`.\n")
        L.append("## I. Repository layout\n")
        L.append("```")
        L.append("Carpet-Hao-Addition/")
        L.append("├─ src/main/java/            shared sources, %d java (used by every version)" % shared_total)
        L.append("├─ src/main/resources/       shared resources (lang MUST live here)")
        L.append("├─ versions/                 %d layers for 1.21.x: %s" % (len(versions), ", ".join(versions)))
        L.append("│    └─ <version>/src/main/java/   that layer's full implementation")
        L.append("├─ modern/                   %d layers for 26.x: %s (built separately)" % (len(moderns), ", ".join(moderns)))
        L.append("├─ docs/                     documentation")
        L.append("└─ tools/                    build and publish scripts")
        L.append("```\n")
        L.append("### Build model: shared + override\n")
        L.append("`versions/<version>/build.gradle` compiles **two** source roots together:\n")
        L.append("```groovy")
        L.append("sourceSets.main.java.srcDirs = [")
        L.append("    rootProject.file('src/main/java'),   // shared: rule settings, command entry points")
        L.append("    file('src/main/java')                // this layer: full implementation")
        L.append("]")
        L.append("duplicatesStrategy = EXCLUDE             // same-named file replaces wholly")
        L.append("```\n")
        L.append("So there are only two kinds of file:\n")
        L.append("| Kind | Location | Notes |")
        L.append("|---|---|---|")
        L.append("| **Shared** | `src/main/java` | Rule settings, commands, cross-version helpers. One copy for all versions |")
        L.append("| **Layer** | `versions/<version>/src/main/java` | APIs differ per version, so each layer implements its own |\n")
        L.append("> ⚠️ Resources such as `lang` **must** go in the shared `src/main/resources`.")
        L.append("> Placed inside a layer they are **wholesale replaced** rather than merged by key,")
        L.append("> which silently drops the other keys.\n")
        L.append("## II. Shared sources (`src/main/java`, %d)\n" % shared_total)
        L.append(emit_groups(shared, False))
        L.append("## III. 1.21.x layers\n")
        L.append("Each layer is a **complete implementation**, not an incremental patch — a feature change has to be applied per layer.\n")
        L.append("| Version | java files in layer |")
        L.append("|---|---:|")
        for v in versions:
            L.append("| `%s` | %d |" % (v, ver_data[v][1]))
        L.append("\n### Per-layer files\n")
        for v in versions:
            g, n = ver_data[v]
            L.append("<details>")
            L.append("<summary><b>%s</b> (%d java files)</summary>\n" % (v, n))
            L.append(emit_groups(g, False))
            L.append("</details>\n")
        L.append("## IV. 26.x layers (`modern/`)\n")
        L.append("The 26.x layers are **built separately** and do not share `versions/` gradle config.\n")
        L.append("| Version | java files in layer |")
        L.append("|---|---:|")
        for m in moderns:
            L.append("| `%s` | %d |" % (m, mod_data[m][1]))
        L.append("")
        for m in moderns:
            g, n = mod_data[m]
            L.append("<details>")
            L.append("<summary><b>%s</b> (%d java files)</summary>\n" % (m, n))
            L.append(emit_groups(g, False))
            L.append("</details>\n")

    return "\n".join(L)


def main():
    for name, zh in (("versions.md", True), ("versions_en.md", False)):
        path = os.path.join(ROOT, "docs", name)
        content = build(zh)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(content if content.endswith("\n") else content + "\n")
        print("wrote docs/%s (%d lines)" % (name, content.count("\n") + 1))


if __name__ == "__main__":
    sys.exit(main())

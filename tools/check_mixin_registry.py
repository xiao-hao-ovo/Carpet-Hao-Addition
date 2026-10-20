#!/usr/bin/env python3
"""核对 mixins.json 与源码里的 @Mixin 类是否一一对应。

漏登记的 mixin 不报错，只是静默不生效；多登记的启动就崩。

versions/ 各层会合并顶层 src/main/java，modern/ 三层是独立构建、不合并，
所以判断"某层该有哪些 mixin"前要先看该层 build.gradle。

用法：
    python tools/check_mixin_registry.py
    python tools/check_mixin_registry.py 1.21.8
"""

import json
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIP = {".git", "build", ".gradle", "run", ".idea", ".reasonix"}


def scan_mixin_fqns(java_root):
    """返回该目录树下所有带 @Mixin 的类的全限定名。"""
    found = set()
    if not os.path.isdir(java_root):
        return found
    for dp, dn, fn in os.walk(java_root):
        dn[:] = [d for d in dn if d not in SKIP]
        for f in fn:
            if not f.endswith(".java"):
                continue
            t = open(os.path.join(dp, f), encoding="utf-8", errors="replace").read()
            if not re.search(r"^\s*@Mixin", t, re.M):
                continue
            m = re.search(r"^package ([\w.]+);", t, re.M)
            if m:
                found.add(m.group(1) + "." + f[:-5])
    return found


def merges_shared(base_dir):
    """该层 build.gradle 是否把顶层的 src/main/java 合并进来。"""
    for name in ("build.gradle", "build.gradle.kts"):
        p = os.path.join(base_dir, name)
        if os.path.exists(p):
            t = open(p, encoding="utf-8", errors="replace").read()
            return "rootProject.file" in t or "rootDir" in t or "'../.." in t
    return False


def main(argv):
    only = argv[1] if len(argv) > 1 else None

    shared = scan_mixin_fqns(os.path.join(ROOT, "src", "main", "java"))

    mj_files = []
    for dp, dn, fn in os.walk(ROOT):
        dn[:] = [d for d in dn if d not in SKIP]
        for f in fn:
            if f.endswith(".mixins.json"):
                mj_files.append(os.path.join(dp, f))

    total = 0
    for mj_path in sorted(mj_files):
        # .../<layer>/src/main/resources/x.mixins.json
        main_dir = os.path.dirname(os.path.dirname(mj_path))
        layer_dir = os.path.dirname(os.path.dirname(main_dir))
        layer_name = os.path.basename(layer_dir)
        rel = mj_path.replace(ROOT + os.sep, "")
        if only and only != layer_name:
            continue

        own = scan_mixin_fqns(os.path.join(main_dir, "java"))
        pool = own | shared if merges_shared(layer_dir) else own

        mj = json.loads(open(mj_path, encoding="utf-8-sig").read())
        pkg = mj.get("package", "") + "."
        registered = set(mj.get("mixins", []) + mj.get("client", []) + mj.get("server", []))

        available = {full[len(pkg):] for full in pool if full.startswith(pkg)}

        miss_reg = sorted(available - registered)
        miss_cls = sorted(registered - available)

        tag = "共享+本层" if merges_shared(layer_dir) else "仅本层"
        if miss_reg or miss_cls:
            total += len(miss_reg) + len(miss_cls)
            print("=" * 60)
            print("%s   [%s]" % (rel, tag))
            if miss_reg:
                print("  [漏登记] 有 @Mixin 类但 json 没列（永不生效，且不报错）:")
                for x in miss_reg:
                    print("     - " + x)
            if miss_cls:
                print("  [多登记] json 列了但找不到类（启动崩）:")
                for x in miss_cls:
                    print("     - " + x)
        else:
            print("OK   %-50s [%s] 登记 %d 条" % (rel, tag, len(registered)))

    print("\n问题合计: %d" % total)
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

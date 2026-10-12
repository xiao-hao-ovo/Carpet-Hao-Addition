# -*- coding: utf-8 -*-
"""核验各层 mixins.json 的分段：common(mixins) 段里不能出现引用客户端专属类的 mixin。

为什么需要它：专用服务器上没有 net.minecraft.client.*。
如果某个 @Mixin(ClientXxx.class) 被注册进 common 段，配合 "required": true，
服务端启动就会 NoClassDefFoundError 直接崩 —— 而单机永远测不出来。

用法：
  python tools/check_mixin_segments.py           全部 11 层
  python tools/check_mixin_segments.py 1.21 26.3 指定层
"""
import glob
import io
import json
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def layers():
    out = []
    for base in ("versions", "modern"):
        for p in sorted(glob.glob(os.path.join(ROOT, base, "*", "gradle.properties"))):
            out.append((os.path.basename(os.path.dirname(p)), base))
    return out


def mixins_json(base, mc):
    p = os.path.join(ROOT, base, mc, "src", "main", "resources",
                     "carpet-hao-addition.mixins.json")
    return p if os.path.exists(p) else None


def src_of(base, mc, cls):
    rel = cls.replace(".", os.sep) + ".java"
    roots = [os.path.join(ROOT, base, mc, "src", "main", "java")]
    if base == "versions":
        roots.append(os.path.join(ROOT, "src", "main", "java"))
    for r in roots:
        p = os.path.join(r, rel)
        if os.path.exists(p):
            return p
    return None


def main():
    want = set(sys.argv[1:])
    problems = 0
    total_segs = {}
    print("层          common  client  server   结论")
    print("-" * 62)
    for mc, base in layers():
        if want and mc not in want:
            continue
        p = mixins_json(base, mc)
        if not p:
            print("%-11s %s" % (mc, "找不到 mixins.json"))
            continue
        d = json.load(io.open(p, encoding="utf-8"))
        pkg = d.get("package", "")
        segs = {k: [pkg + "." + x for x in d[k]] for k in ("mixins", "client", "server") if k in d}
        total_segs[mc] = segs
        bad = []
        for cls in segs.get("mixins", ()):
            f = src_of(base, mc, cls)
            if not f:
                bad.append((cls.rsplit(".", 1)[-1], "源码找不到"))
                continue
            s = re.sub(r"/\*.*?\*/", "", io.open(f, encoding="utf-8", errors="replace").read(),
                       flags=re.S)
            hits = sorted(set(re.findall(r"net\.minecraft\.client\.[\w.]+", s)))
            if hits:
                bad.append((cls.rsplit(".", 1)[-1], "引用了 " + ", ".join(hits[:3])))
        print("%-11s %-7d %-7d %-7d %s" % (
            mc, len(segs.get("mixins", ())), len(segs.get("client", ())),
            len(segs.get("server", ())), "✅" if not bad else "❌ %d 项" % len(bad)))
        for cls, why in bad:
            print("              ❌ common 段 %s —— %s" % (cls, why))
            problems += 1
    print()
    print("✅ 分段无客户端泄漏" if problems == 0 else "❌ %d 处客户端类进了 common 段" % problems)
    return problems


if __name__ == "__main__":
    main()

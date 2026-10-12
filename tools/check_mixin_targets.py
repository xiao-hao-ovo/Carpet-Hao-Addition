# -*- coding: utf-8 -*-
"""跨层核验 mixin 注解目标是否真实存在（自己解析 class 文件，不依赖 javap / refmap）。

核对内容：
  @Mixin(X.class) / targets="..."   目标类是否存在
  @Inject / @WrapMethod / @ModifyArg(method="...")   方法是否在目标类（或其父类链）里
  @Invoker / @Accessor("name")                       方法 / 字段是否存在
  @At(target="Lowner;name(desc)ret")                 owner + 方法名 + 完整描述符

为什么需要它：Mixin 的注解目标是**字符串**，名字写错时 javac 不会报错，
编译能过、启动才炸（require=1）或被 require=0 静默吞掉（规则"能开但没效果"）。

覆盖 11 个代码基：
  versions/<mc>   Yarn 名，classpath 用 yarn 映射后的 merged jar
  modern/<mc>     Mojang 名，classpath 用 minecraft-merged-deobf jar

用法：
  python tools/check_mixin_targets.py                   全部 11 层
  python tools/check_mixin_targets.py 1.21 26.3         指定层
  python tools/check_mixin_targets.py probe 1.21 net.minecraft.block.BlockState
"""
import glob
import io
import os
import re
import struct
import sys
import zipfile

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HOME = os.path.expanduser("~")
LOOM = os.path.join(HOME, ".gradle", "caches", "fabric-loom")
GRADLE = os.path.join(HOME, ".gradle", "caches")
MVN = os.path.join(LOOM, "minecraftMaven", "net", "minecraft")

MODS = (("litematica", "litematica_version"), ("malilib", "malilib_version"))


# ------------------------------------------------------------------ class 解析
class Cls(object):
    __slots__ = ("fqn", "sup", "ifs", "methods", "fields")

    def __init__(self, fqn, sup, ifs, methods, fields):
        self.fqn, self.sup, self.ifs = fqn, sup, ifs
        self.methods, self.fields = methods, fields

    @property
    def mnames(self):
        return set(n for n, _ in self.methods)

    @property
    def fnames(self):
        return set(n for n, _ in self.fields)


def parse_class(b):
    """只取本类声明的字段/方法（不含继承）——正是 Mixin 注入的判定语义。"""
    cp_count = struct.unpack_from(">H", b, 8)[0]
    cp = [None] * cp_count
    j, k = 10, 1
    while k < cp_count:
        tag = b[j]
        j += 1
        if tag == 1:
            ln = struct.unpack_from(">H", b, j)[0]
            j += 2
            cp[k] = b[j:j + ln].decode("utf-8", "replace")
            j += ln
        elif tag in (7, 8, 16, 19, 20):
            cp[k] = struct.unpack_from(">H", b, j)[0]
            j += 2
        elif tag == 15:
            j += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            j += 4
        elif tag in (5, 6):
            j += 8
            k += 1
        else:
            raise ValueError("bad constant-pool tag %d" % tag)
        k += 1

    st = {"j": j}

    def u2():
        v = struct.unpack_from(">H", b, st["j"])[0]
        st["j"] += 2
        return v

    def u4():
        v = struct.unpack_from(">I", b, st["j"])[0]
        st["j"] += 4
        return v

    def cname(i):
        return cp[i] if isinstance(cp[i], str) else cp[cp[i]]

    def skip_attrs(n):
        for _ in range(n):
            u2()
            ln = u4()
            st["j"] += ln

    u2()
    this_i = u2()
    sup_i = u2()
    ifs = [cname(u2()) for _ in range(u2())]
    fields = []
    for _ in range(u2()):
        u2()
        n, d = cp[u2()], cp[u2()]
        skip_attrs(u2())
        fields.append((n, d))
    methods = []
    for _ in range(u2()):
        u2()
        n, d = cp[u2()], cp[u2()]
        skip_attrs(u2())
        methods.append((n, d))
    return Cls(cname(this_i).replace("/", "."),
               cname(sup_i).replace("/", ".") if sup_i else None,
               [i.replace("/", ".") for i in ifs], methods, fields)


# ------------------------------------------------------------------ jar 索引
_jars = []
_cache = {}


def load_jars(paths):
    global _jars
    _jars = []
    _cache.clear()          # 换层必须清缓存，否则各层 class 数据串味
    for p in paths:
        if not p or not os.path.exists(p):
            continue
        try:
            z = zipfile.ZipFile(p)
        except Exception:
            continue
        idx = set(n[:-6].replace("/", ".") for n in z.namelist() if n.endswith(".class"))
        _jars.append((p, z, idx))


def find_class(fqn):
    if fqn in _cache:
        return _cache[fqn]
    cands = [fqn]
    if "." in fqn and "$" not in fqn:
        head, _, tail = fqn.rpartition(".")
        cands.append(head + "$" + tail)      # AbstractBlock.AbstractBlockState -> ...$...
    res = None
    for c in cands:
        for _, z, idx in _jars:
            if c in idx:
                try:
                    res = parse_class(z.read(c.replace(".", "/") + ".class"))
                except Exception:
                    res = None
                break
        if res:
            break
    _cache[fqn] = res
    return res


def chain(cls, seen=None):
    out, seen = [], seen or set()
    while cls and cls.fqn not in seen:
        seen.add(cls.fqn)
        out.append(cls)
        nxt = find_class(cls.sup) if cls.sup else None
        if nxt is None:
            for i in cls.ifs:
                nxt = find_class(i)
                if nxt:
                    break
        cls = nxt
    return out


def find_member(cls, name, desc=None):
    """-> (owner, inherited, exact) | None。exact=False 表示只按名匹配。
    继承也算命中：Mixin 的 @Inject 支持注入父类声明的方法。"""
    for i, c in enumerate(chain(cls)):
        if desc and (name, desc) in c.methods:
            return c.fqn, i > 0, True
        if name in c.mnames:
            return c.fqn, i > 0, not desc
    return None


def find_field(cls, name):
    for i, c in enumerate(chain(cls)):
        if name in c.fnames:
            return c.fqn, i > 0
    return None


# ------------------------------------------------------------- 注解 / import
def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"//[^\n]*", "", s)


def annot_args(s, name):
    """取 @name(...) 的参数原文，做括号配平（不惧嵌套 @At(...) 与参数顺序）。"""
    out = []
    for m in re.finditer(r"@" + name + r"\s*\(", s):
        i = m.end() - 1
        depth, j = 0, i
        while j < len(s):
            if s[j] == "(":
                depth += 1
            elif s[j] == ")":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        out.append(s[i + 1:j])
    return out


def imports_of(s):
    d = {}
    for m in re.finditer(r"^import\s+(?:static\s+)?([\w.]+);", s, re.M):
        f = m.group(1)
        d[f.rsplit(".", 1)[-1]] = f
    return d


def resolve_ref(t, imp):
    parts = t.split(".")
    if len(parts) == 1:
        return imp.get(t)
    if imp.get(parts[0]):
        return imp[parts[0]] + "$" + "$".join(parts[1:])
    return t


def at_targets(target):
    """Lowner;name(desc)ret | Lowner;name:type  ->  [(owner, name, desc, kind)]"""
    out = []
    for m in re.finditer(r"^L([^;]+);([A-Za-z0-9_$<>]+)(\([^)]*\)[^\s]*)$", target.strip()):
        out.append((m.group(1).replace("/", "."), m.group(2), m.group(3), "method"))
    if not out:
        m = re.match(r"^L([^;]+);([A-Za-z0-9_$]+):(.+)$", target.strip())
        if m:
            out.append((m.group(1).replace("/", "."), m.group(2), m.group(3), "field"))
    return out


# ------------------------------------------------------------------ 层定义
def layers():
    """返回 [(mc, 源码目录列表, jar 列表, 标签)]"""
    out = []
    for p in sorted(glob.glob(os.path.join(ROOT, "versions", "*", "gradle.properties"))):
        mc = os.path.basename(os.path.dirname(p))
        src = [os.path.join(ROOT, "src", "main", "java"),
               os.path.join(ROOT, "versions", mc, "src", "main", "java")]
        hits = glob.glob(os.path.join(MVN, "minecraft-merged", mc + "-net.*", "*.jar"))
        out.append((mc, src, hits[0] if hits else None, "yarn"))
    for p in sorted(glob.glob(os.path.join(ROOT, "modern", "*", "gradle.properties"))):
        mc = os.path.basename(os.path.dirname(p))
        src = [os.path.join(ROOT, "modern", mc, "src", "main", "java")]
        hits = glob.glob(os.path.join(MVN, "minecraft-merged-deobf", mc,
                                      "minecraft-merged-deobf-*.jar"))
        out.append((mc, src, hits[0] if hits else None, "mojang"))
    return out


def props(mc):
    for base in ("versions", "modern"):
        p = os.path.join(ROOT, base, mc, "gradle.properties")
        if os.path.exists(p):
            d = {}
            for line in io.open(p, encoding="utf-8", errors="replace"):
                if "=" in line and not line.strip().startswith("#"):
                    k, v = line.split("=", 1)
                    d[k.strip()] = v.strip()
            return d
    return {}


def mod_jars(mc):
    """litematica / malilib：优先本层 loom 的 remapped jar，退到他层同版本，
    再退到 gradle 缓存里的原始 jar（未 remap，描述符不可比）。"""
    ver = props(mc)
    found, labels = [], []
    for name, key in MODS:
        v = ver.get(key)
        pat = "remapped.maven.modrinth-%s-*%s.jar" % (name, v)
        cands = [
            glob.glob(os.path.join(ROOT, "versions", mc, "build", "loom-cache",
                                   "remapped_working", pat))
            + glob.glob(os.path.join(ROOT, "modern", mc, "build", "loom-cache",
                                     "remapped_working", pat)),
            glob.glob(os.path.join(ROOT, "*", "*", "build", "loom-cache",
                                   "remapped_working", pat)),
            glob.glob(os.path.join(GRADLE, "modules-2", "files-2.1", "maven.modrinth",
                                   name, v or "\0", "*", "%s-%s.jar" % (name, v))),
        ]
        hit, lab = None, None
        for i, c in enumerate(cands):
            if c:
                hit = c[0]
                lab = "remapped" if i == 0 else ("remapped(他层)" if i == 1 else "raw(描述符不可比)")
                break
        found.append(hit)
        labels.append("%s=%s[%s]" % (name, v, lab or "缺失"))
    return found, labels


ANN_PAT = [("Inject", True), ("WrapMethod", True), ("Invoker", False), ("Accessor", False)]


def scan(mc, srcdirs, mcjar, kind):
    mods, labels = mod_jars(mc)
    load_jars([mcjar] + mods)
    exact = loose = 0
    hard, soft = [], []
    for sd in srcdirs:
        for dp, _, fs in os.walk(sd):
            for fn in fs:
                if not fn.endswith(".java"):
                    continue
                s = strip_comments(io.open(os.path.join(dp, fn), encoding="utf-8",
                                           newline="").read())
                margs = annot_args(s, "Mixin")
                if not margs:
                    continue
                imp = imports_of(s)
                fqns = []
                for a in margs:
                    for t in re.findall(r"([A-Za-z0-9_.]+)\.class", a):
                        f = resolve_ref(t, imp)
                        if f:
                            fqns.append(f)
                    fqns += re.findall(r'targets\s*=\s*"([^"]+)"', a)
                if not fqns:
                    hard.append((fn, "-", "无法解析 @Mixin 目标类"))
                    continue
                for fq in fqns:
                    cls = find_class(fq)
                    if cls is None:
                        hard.append((fn, fq, "❌ 整个类不存在（classpath 找不到）"))
                        continue
                    for anno, uses_method in ANN_PAT:
                        for a in annot_args(s, anno):
                            m = re.search(r'method\s*=\s*"([^"]+)"', a) if uses_method \
                                else re.search(r'"([A-Za-z0-9_$]+)"', a)
                            if not m:
                                continue
                            nm, base = m.group(1), m.group(1).split("(")[0]
                            # method = "foo(" + 描述符常量 + ")" 这类拼接：本脚本只能看到前缀，
                            # 既无法核对描述符，也不能据此判定重载歧义。
                            concat = bool(re.search(r'method\s*=\s*"[^"]*"\s*\+', a))
                            if anno == "Accessor":
                                f = find_field(cls, base)
                                if f is None:
                                    hard.append((fn, fq, '❌ @Accessor("%s") 字段找不到' % nm))
                                else:
                                    exact += 1
                                continue
                            desc = nm[len(base):] if "(" in nm and not nm.endswith("(") else None
                            if concat:
                                desc = None
                            hit = find_member(cls, base, desc)
                            if not hit:
                                hard.append((fn, fq, '❌ @%s("%s") 目标类里找不到' % (anno, nm)))
                                continue
                            exact += hit[2]
                            loose += (not hit[2])
                            if not hit[2]:
                                soft.append((fn, fq, '⚠️ @%s("%s") 只按名匹配（描述符未对上）'
                                             % (anno, nm)))
                            if (anno == "Inject" and desc is None and not concat
                                    and base not in ("<init>", "<clinit>")):
                                n = sum(1 for x, _ in cls.methods if x == base)
                                if n > 1:
                                    soft.append((fn, fq, '⚠️ @Inject("%s") 有 %d 个重载，未写描述符'
                                                 % (base, n)))
                    for a in annot_args(s, "At"):
                        m = re.search(r'target\s*=\s*"([^"]+)"', a)
                        if not m:
                            continue
                        for own, nm, desc, kd in at_targets(m.group(1)):
                            tcls = find_class(own)
                            if tcls is None:
                                continue          # 非 MC/非依赖库类，跳过
                            if kd == "field":
                                hit = (own, False, True) if nm in tcls.fnames else None
                            else:
                                hit = find_member(tcls, nm, desc)
                            if not hit:
                                hard.append((fn, own, "❌ @At 目标 %s%s 不存在" % (nm, desc)))
                                continue
                            exact += hit[2]
                            loose += (not hit[2])
                            if not hit[2]:
                                soft.append((fn, own, '⚠️ @At 目标 %s%s 只按名匹配' % (nm, desc)))
    print("=== %-8s [%s]  %s" % (mc, kind, "  ".join(labels)))
    print("    精确 %d / 宽松 %d / ❌ %d / ⚠️ %d" % (exact, loose, len(hard), len(soft)))
    for fn, fq, msg in sorted(set(hard)):
        print("    %-46s %-34s %s" % (fn, fq.rsplit(".", 1)[-1], msg))
    for fn, fq, msg in sorted(set(soft)):
        print("    %-46s %-34s %s" % (fn, fq.rsplit(".", 1)[-1], msg))
    return len(hard)


def probe(mc, fqn):
    for m, src, jar, kind in layers():
        if m == mc:
            load_jars([jar] + mod_jars(mc)[0])
            c = find_class(fqn)
            if c is None:
                print("找不到", fqn)
                return
            print("%s  super=%s  interfaces=%s" % (c.fqn, c.sup, c.ifs))
            print("  fields :", ", ".join("%s : %s" % (n, d) for n, d in c.fields))
            for n, d in c.methods:
                print("      %s%s" % (n, d))
            return
    print("未知层", mc)


def selftest(mc="1.21.8"):
    """阴性对照：故意用错的输入，验证判定能力真的存在（全绿前先确认脚本不是瞎绿）。"""
    print("自检目标层 =", mc)
    for m, src, jar, kind in layers():
        if m == mc:
            load_jars([jar] + mod_jars(mc)[0])
            break
    else:
        print("❌ 找不到层", mc)
        return 1
    ent = find_class("net.minecraft.entity.Entity")
    if ent is None:
        print("❌ 连 net.minecraft.entity.Entity 都解析不到，classpath 有问题")
        return 1
    ok = True

    def chk(label, got, want):
        nonlocal ok
        good = (got == want)
        ok = ok and good
        print("  %s %-52s got=%s want=%s" % ("✅" if good else "❌", label, got, want))

    chk("不存在的方法名 -> None", find_member(ent, "haoNoSuchMethod"), None)
    chk("不存在的类 -> None", find_class("net.minecraft.HaoNoSuchClass"), None)
    chk("描述符写错 -> exact=False",
        (find_member(ent, "isTouchingWater", "()I") or (None,))[-1], False)
    chk("描述符正确 -> exact=True",
        (find_member(ent, "isTouchingWater", "()Z") or (None,))[-1], True)
    chk("只给方法名且无重载 -> 视为通过", find_member(ent, "isTouchingWater"), (ent.fqn, False, True))
    chk("字段与方法是两套命名空间",
        find_field(ent, "isTouchingWater"), None)
    print("✅ 脚本判定能力正常" if ok else "❌ 脚本判定能力有问题")
    return 0 if ok else 1


def main():
    a = sys.argv[1:]
    if a and a[0] == "probe":
        probe(a[1], a[2])
        return
    if a and a[0] == "selftest":
        sys.exit(selftest(*a[1:]))
    want = set(a)
    bad = 0
    for mc, src, jar, kind in layers():
        if want and mc not in want:
            continue
        if not jar:
            print("=== %-8s 找不到 Minecraft jar，跳过" % mc)
            continue
        bad += scan(mc, src, jar, kind)
    print()
    print("✅ 全部目标命中" if bad == 0 else "❌ 共 %d 项目标不存在" % bad)


if __name__ == "__main__":
    main()

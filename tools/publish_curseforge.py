#!/usr/bin/env python3
"""把构建好的 jar 上传到 CurseForge(官方 Upload API)。

凭据来源(按优先级):
  1. 环境变量 CURSEFORGE_API_KEY / CURSEFORGE_PROJECT_ID(CI 用)
  2. ~/.secrets/curseforge-api-key.txt 与 ~/.secrets/curseforge-project-id.txt(本机用)

用法:
  python tools/publish_curseforge.py versions             # 上传仓库构建目录里的全部 jar
  python tools/publish_curseforge.py versions --dir jars  # 上传指定目录里的 jar(CI 用)
  python tools/publish_curseforge.py versions --dry-run   # 只打印将要上传的内容(不需要凭据)
  python tools/publish_curseforge.py list                 # 列出项目已有的文件
  python tools/publish_curseforge.py probe                # 查看 CurseForge 认得的 MC 版本 / 加载器名
"""
import argparse, json, os, pathlib, sys, urllib.error, urllib.request, uuid

API = "https://api.curseforge.com/v1"
REPO = pathlib.Path(__file__).resolve().parent.parent
SECRETS = pathlib.Path(os.path.expanduser("~")) / ".secrets"
GAME_ID = 432  # Minecraft
DEFAULT_PROJECT_ID = "1689732"
GAME_VERSIONS = ["1.21", "1.21.1", "1.21.2", "1.21.4", "1.21.6", "1.21.8",
                 "1.21.10", "1.21.11", "26.1.2", "26.2"]
CHANGELOG = ("通过 GitHub Actions 自动发布。需要 Fabric Loader >= 0.16.10(26.x 建议 >= 0.19)与对应版本的 Carpet。"
             "全部规则默认关闭,游戏内用 `/carpet <规则> <值>` 开启。")

def api_key():
    v = os.environ.get("CURSEFORGE_API_KEY", "").strip()
    if v:
        return v
    p = SECRETS / "curseforge-api-key.txt"
    if p.is_file():
        return p.read_text(encoding="utf-8").strip()
    sys.exit(f"缺少 CurseForge API key:设置环境变量 CURSEFORGE_API_KEY 或写入 {p}")

def project_id():
    v = os.environ.get("CURSEFORGE_PROJECT_ID", "").strip()
    if not v:
        p = SECRETS / "curseforge-project-id.txt"
        v = p.read_text(encoding="utf-8").strip() if p.is_file() else DEFAULT_PROJECT_ID
    digits = "".join(ch for ch in v if ch.isdigit())
    if not digits:
        sys.exit(f"projectID 无效: {v!r}")
    return digits

def api(method, path, body=None):
    req = urllib.request.Request(API + path, method=method)
    req.add_header("x-api-key", api_key())
    req.add_header("Accept", "application/json")
    if body is not None:
        req.add_header("Content-Type", "application/json")
        req.data = json.dumps(body).encode("utf-8")
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            raw = json.loads(raw)
        except Exception:
            raw = raw.decode("utf-8", "replace")
        return e.code, raw

def upload(path, mc):
    boundary = "----hao" + uuid.uuid4().hex
    version = path.name[:-4].split("-", 3)[-1].split("+", 1)[0]
    meta = {"changelog": f"版本 {version} · {CHANGELOG}", "changelogType": "markdown",
            "displayName": f"{version} for Minecraft {mc}",
            "releaseType": "release", "gameVersions": [mc, "Fabric"]}
    buf = bytearray()
    buf += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"metadata\"\r\n\r\n"
            f"{json.dumps(meta, ensure_ascii=False)}\r\n").encode("utf-8")
    buf += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{path.name}\"\r\n"
            f"Content-Type: application/java-archive\r\n\r\n").encode("utf-8")
    buf += path.read_bytes()
    buf += f"\r\n--{boundary}--\r\n".encode("utf-8")
    req = urllib.request.Request(f"{API}/projects/{project_id()}/upload-file", data=bytes(buf), method="POST")
    req.add_header("x-api-key", api_key())
    req.add_header("Accept", "application/json")
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    try:
        with urllib.request.urlopen(req, timeout=900) as r:
            return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            raw = json.loads(raw)
        except Exception:
            raw = raw.decode("utf-8", "replace")
        return e.code, raw

def local_jars(directory=None):
    if directory:
        paths = sorted(pathlib.Path(directory).glob("*.jar"))
    else:
        paths = []
        for pat in ("build/libs/*.jar", "modern/*/build/libs/*.jar"):
            paths += sorted(REPO.glob(pat))
    out = []
    for p in paths:
        if "sources" in p.name or not p.name.startswith("carpet-hao-addition-") or "+" not in p.name:
            continue
        out.append((p.name[:-4].split("+", 1)[1], p))
    return sorted(out, key=lambda t: GAME_VERSIONS.index(t[0]) if t[0] in GAME_VERSIONS else 99)

def existing_filenames():
    st, body = api("GET", f"/mods/{project_id()}/files?pageSize=200")
    if st != 200 or not isinstance(body, dict):
        print(f"警告:无法列出项目已有文件 http={st}", file=sys.stderr)
        return set()
    return {f.get("fileName") for f in body.get("data", [])}

def cmd_versions(args):
    jobs = local_jars(args.dir)
    if not jobs:
        sys.exit("没有找到可上传的 jar")
    print(f"待上传 {len(jobs)} 个文件:")
    for mc, p in jobs:
        print(f"   {mc:8s} {p.name} ({p.stat().st_size} bytes)")
    if args.dry_run:
        print("--dry-run:未上传")
        return
    done = existing_filenames()
    print(f"项目里已有 {len(done)} 个文件")
    fail = 0
    for mc, p in jobs:
        if p.name in done:
            print(f"skip {mc:8s} {p.name}(已存在)")
            continue
        st, body = upload(p, mc)
        fid = body.get("data", {}).get("id") if isinstance(body, dict) else None
        if st in (200, 201) and fid:
            print(f"OK   {mc:8s} {p.name} file_id={fid}")
        else:
            fail += 1
            print(f"FAIL {mc:8s} {p.name} http={st} {json.dumps(body, ensure_ascii=False)[:300]}")
    if fail:
        sys.exit(f"{fail} 个文件上传失败")

def cmd_list(args):
    st, body = api("GET", f"/mods/{project_id()}/files?pageSize=200")
    print("files http:", st)
    if isinstance(body, dict):
        for f in body.get("data", []):
            print("  ", f.get("id"), "|", f.get("displayName"), "|", f.get("fileName"), "|", f.get("gameVersions"))
    else:
        print(body)

def cmd_probe(args):
    st, body = api("GET", f"/games/{GAME_ID}/versions")
    print("game versions http:", st)
    if isinstance(body, dict):
        names = set()
        for v in body.get("data", []):
            for k in ("name", "versionString"):
                if isinstance(v.get(k), str):
                    names.add(v[k])
        for mc in GAME_VERSIONS:
            print(("FOUND  " if mc in names else "MISSING"), mc)
    else:
        print(body)
    st, body = api("GET", "/minecraft/modloader?version=1.21.1")
    print("modloader http:", st)
    if isinstance(body, dict):
        print("loaders:", [m.get("name") for m in body.get("data", [])][:12])

def main():
    ap = argparse.ArgumentParser(description="上传 jar 到 CurseForge")
    sub = ap.add_subparsers(dest="cmd", required=True)
    for name, fn in (("versions", cmd_versions), ("list", cmd_list), ("probe", cmd_probe)):
        sp = sub.add_parser(name)
        sp.add_argument("--dir", help="jar 所在目录(CI 里传下载下来的 artifacts 目录)")
        sp.add_argument("--dry-run", action="store_true", help="只打印将上传的文件")
        sp.set_defaults(func=fn, dry_run=False)
    args = ap.parse_args()
    args.func(args)

if __name__ == "__main__":
    main()

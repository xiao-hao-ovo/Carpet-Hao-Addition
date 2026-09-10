#!/usr/bin/env python3
"""把 Carpet-Hao-Addition 的各版本构建产物发布到 Modrinth。

凭据来源(按优先级):
  1. 环境变量 MODRINTH_TOKEN(CI 用;PAT 需要 PROJECT_CREATE / VERSION_CREATE 权限)
  2. ~/.secrets/modrinth-token.txt 或 ~/.secrets/modrinth-pat.txt(本机用)

网络:默认直连;需要代理时设置 MODRINTH_PROXY(或 HTTPS_PROXY / https_proxy)。

用法:
  python tools/publish_modrinth.py check                    # 校验 token / slug / carpet 依赖
  python tools/publish_modrinth.py versions --dir jars      # 上传目录里的 jar(项目不存在时自动建草稿)
  python tools/publish_modrinth.py versions --dry-run       # 只打印将上传的内容(不需要凭据)
  python tools/publish_modrinth.py create                   # 只创建草稿项目
  python tools/publish_modrinth.py publish                  # 提交公开审核(requested_status=approved)
  python tools/publish_modrinth.py status                   # 打印项目与版本现状
"""
import argparse, json, os, pathlib, sys, urllib.error, urllib.parse, urllib.request, uuid

API = "https://api.modrinth.com/v2"
REPO = pathlib.Path(__file__).resolve().parent.parent
SECRETS = pathlib.Path(os.path.expanduser("~")) / ".secrets"
TOKEN_FILES = [SECRETS / "modrinth-token.txt", SECRETS / "modrinth-pat.txt"]
UA = "xiao-hao-ovo/Carpet-Hao-Addition (github.com/xiao-hao-ovo/Carpet-Hao-Addition)"
TIMEOUT = 300

TITLE = "Carpet-Hao-Addition"
SLUG = "carpet-hao-addition"
DESCRIPTION = ("Carpet 扩展 / Fabric Carpet addon:提供一批默认关闭、可随时开关的地毯规则(区域禁侦测器、"
               "金胡萝卜堆肥、雪地方解石、末地门传送白名单、陶瓦去色、基岩可挖、凋灵骷髅掉落削减),"
               "覆盖 Minecraft 1.21–26.2。Opt-in Carpet rules, all disabled by default.")
CATEGORIES = ["utility", "management"]
LICENSE_ID = "CC0-1.0"
CLASSIFIER = "release"          # 版本渠道: release / beta / alpha
GAME_VERSIONS = ["1.21", "1.21.1", "1.21.2", "1.21.4", "1.21.6", "1.21.8",
                 "1.21.10", "1.21.11", "26.1.2", "26.2"]
DEFAULT_ICON = REPO / "tools" / "icon-512.png"
DEFAULT_BODY = REPO / "tools" / "modrinth-body.md"

def token():
    v = os.environ.get("MODRINTH_TOKEN", "").strip()
    if v:
        return v
    for p in TOKEN_FILES:
        if p.is_file():
            v = p.read_text(encoding="utf-8").strip()
            if v:
                return v
    sys.exit("缺少 Modrinth 凭据:设置环境变量 MODRINTH_TOKEN 或写入 "
             + " / ".join(str(p) for p in TOKEN_FILES))

def opener():
    proxy = (os.environ.get("MODRINTH_PROXY") or os.environ.get("HTTPS_PROXY")
             or os.environ.get("https_proxy") or "").strip()
    if proxy:
        return urllib.request.build_opener(
            urllib.request.ProxyHandler({"http": proxy, "https": proxy}))
    return urllib.request.build_opener()

def request(method, path, tok=None, body=None, ctype="application/json", raw=None):
    url = path if path.startswith("http") else API + path
    req = urllib.request.Request(
        url,
        data=raw if raw is not None else (json.dumps(body).encode() if body is not None else None),
        method=method)
    req.add_header("User-Agent", UA)
    if tok:
        req.add_header("Authorization", tok)
    if body is not None or (raw is not None and ctype):
        req.add_header("Content-Type", ctype)
    try:
        with opener().open(req, timeout=TIMEOUT) as r:
            data = r.read()
            return r.status, (json.loads(data) if data else None)
    except urllib.error.HTTPError as e:
        payload = e.read()
        try:
            payload = json.loads(payload)
        except Exception:
            payload = payload.decode("utf-8", "replace")
        return e.code, payload

def multipart(fields, files):
    """fields: dict(name->str);files: list[(field, path, ctype)]"""
    boundary = "----hao" + uuid.uuid4().hex
    buf = bytearray()
    for k, v in fields.items():
        buf += f"--{boundary}\r\nContent-Disposition: form-data; name=\"{k}\"\r\n\r\n{v}\r\n".encode("utf-8")
    for field, path, ctype in files:
        path = pathlib.Path(path)
        buf += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{field}\"; filename=\"{path.name}\"\r\n"
                f"Content-Type: {ctype}\r\n\r\n").encode("utf-8")
        buf += path.read_bytes()
        buf += b"\r\n"
    buf += f"--{boundary}--\r\n".encode("utf-8")
    return bytes(buf), f"multipart/form-data; boundary={boundary}"

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

def project_id(tok):
    st, body = request("GET", f"/project/{SLUG}", tok)
    return body["id"] if st == 200 and isinstance(body, dict) else None

def make_changelog(total):
    override = os.environ.get("MODRINTH_CHANGELOG", "").strip()
    if override:
        return override
    mod, mc = total.split("+", 1)
    return (f"对应 GitHub Release v{mod}。支持 Minecraft {mc},需 Fabric Loader ≥ 0.16.10"
            f"(26.x 建议 ≥ 0.19)与对应版本的 Carpet;全部规则默认关闭,"
            f"游戏内用 `/carpet <规则> <值>` 开启。")

def carpet_dependency():
    """查 Carpet 本体的 project_id(公开端点,不需要 token)。"""
    qs = urllib.parse.urlencode({"query": "carpet", "facets": '[["project_type:mod"]]', "limit": "5"})
    st, res = request("GET", f"/search?{qs}")
    if st != 200 or not isinstance(res, dict):
        print(f"警告: 搜索 Carpet 失败 http={st}", file=sys.stderr)
        return None
    for hit in res.get("hits", []):
        if hit.get("slug") == "carpet":
            return hit.get("project_id")
    print("警告: 搜索结果里没有 slug=carpet 的项目", file=sys.stderr)
    return None

def existing_versions(tok, pid):
    st, body = request("GET", f"/project/{pid}/version", tok)
    if st != 200 or not isinstance(body, list):
        return {}
    return {v["version_number"]: v["id"] for v in body}

def create_project(tok, args):
    body_file = pathlib.Path(args.body_file) if args.body_file else DEFAULT_BODY
    if not body_file.is_file():
        body_file = REPO / "README.md"
    data = {
        "title": TITLE, "slug": SLUG, "description": DESCRIPTION,
        "body": body_file.read_text(encoding="utf-8") if body_file.is_file() else "",
        "categories": CATEGORIES, "additional_categories": [],
        "license_id": LICENSE_ID, "project_type": "mod", "is_draft": True,
        "source_url": "https://github.com/xiao-hao-ovo/Carpet-Hao-Addition",
        "issues_url": "https://github.com/xiao-hao-ovo/Carpet-Hao-Addition/issues",
        "client_side": "unsupported", "server_side": "required",
    }
    icon = pathlib.Path(args.icon) if args.icon else DEFAULT_ICON
    files = [("icon", icon, "image/png")] if icon.is_file() else []
    if not files:
        print(f"警告: 找不到项目图标 {icon}", file=sys.stderr)
    raw, ctype = multipart({"data": json.dumps(data, ensure_ascii=False)}, files)
    st, body = request("POST", "/project", tok, raw=raw, ctype=ctype)
    if st in (200, 201) and isinstance(body, dict):
        print(f"created project: id={body.get('id')} status={body.get('status')}")
        return body.get("id")
    print(f"FAIL create project http={st} {json.dumps(body, ensure_ascii=False)[:400]}")
    return None

def ensure_project(tok, args):
    pid = project_id(tok)
    if pid:
        return pid
    print(f"项目 {SLUG} 不存在,先创建草稿项目")
    return create_project(tok, args)

def upload_version(tok, pid, mc, path, dep, changelog):
    total = path.name[:-4].split("-", 3)[3]
    data = {
        "name": f"{total} for Minecraft {mc}",
        "version_number": total,
        "changelog": changelog,
        "dependencies": [{"project_id": dep, "dependency_type": "required"}] if dep else [],
        "game_versions": [mc],
        "version_type": CLASSIFIER,
        "loaders": ["fabric"],
        "project_id": pid,
        "file_parts": ["file"],
        "primary_file": "file",
        "featured": False,
        "environment": "client_and_server",
    }
    raw, ctype = multipart({"data": json.dumps(data, ensure_ascii=False)},
                           [("file", path, "application/java-archive")])
    return request("POST", "/version", tok, raw=raw, ctype=ctype)

def cmd_check(args):
    tok = token()
    st, user = request("GET", "/user", tok)
    print("token/user:", st, (user or {}).get("username") if isinstance(user, dict) else user)
    st, valid = request("GET", f"/project/check?slug={SLUG}", tok)
    print("slug check:", st, valid)
    print("carpet dependency project:", carpet_dependency())
    print("local jars:")
    for mc, p in local_jars(args.dir):
        print("  ", mc, p.name, p.stat().st_size)

def cmd_create(args):
    tok = token()
    pid = project_id(tok)
    if pid:
        print(f"项目已存在: {pid}(跳过创建)")
        return
    create_project(tok, args)

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
    tok = token()
    pid = ensure_project(tok, args)
    if not pid:
        sys.exit("项目不可用,已中止")
    dep = carpet_dependency()
    if not dep:
        print("警告: 未解析出 Carpet 依赖,上传的版本将不带 required 依赖声明", file=sys.stderr)
    have = existing_versions(tok, pid)
    print(f"项目已有 {len(have)} 个版本")
    fail = 0
    for mc, p in jobs:
        total = p.name[:-4].split("-", 3)[3]
        if total in have:
            print(f"skip {mc:8s} {total}(已存在)")
            continue
        st, body = upload_version(tok, pid, mc, p, dep, make_changelog(total))
        vid = body.get("id") if isinstance(body, dict) else None
        if st in (200, 201) and vid:
            print(f"OK   {mc:8s} {total:16s} version_id={vid}")
        else:
            fail += 1
            print(f"FAIL {mc:8s} {total:16s} http={st} {json.dumps(body, ensure_ascii=False)[:300]}")
    if fail:
        sys.exit(f"{fail} 个版本上传失败")

def cmd_publish(args):
    tok = token()
    pid = project_id(tok)
    if not pid:
        sys.exit(f"找不到项目 {SLUG}")
    st, body = request("PATCH", f"/project/{pid}", tok, body={"requested_status": "approved"})
    print("submit for review:", st, json.dumps(body, ensure_ascii=False)[:300] if isinstance(body, dict) else body)

def cmd_status(args):
    tok = token()
    pid = project_id(tok)
    st, body = request("GET", f"/project/{pid or SLUG}", tok)
    if isinstance(body, dict):
        print("project:", body.get("id"), body.get("slug"), "| status:", body.get("status"),
              "| requested:", body.get("requested_status"), "| icon:", bool(body.get("icon_url")))
    else:
        print("project:", st, body)
    st, versions = request("GET", f"/project/{pid or SLUG}/version", tok)
    if isinstance(versions, list):
        print("versions:", len(versions))
        for v in versions:
            print("  ", v["version_number"], v["game_versions"], v["version_type"], v["status"])
    else:
        print("versions:", st, versions)

def main():
    ap = argparse.ArgumentParser(description="发布 jar 到 Modrinth")
    sub = ap.add_subparsers(dest="cmd", required=True)
    for name, fn in (("check", cmd_check), ("create", cmd_create), ("versions", cmd_versions),
                     ("publish", cmd_publish), ("status", cmd_status)):
        sp = sub.add_parser(name)
        sp.add_argument("--dir", help="jar 所在目录(CI 里传下载下来的 artifacts 目录)")
        sp.add_argument("--icon", help="项目图标 png")
        sp.add_argument("--body-file", help="项目正文 markdown")
        sp.add_argument("--dry-run", action="store_true", help="只打印将上传的文件")
        sp.set_defaults(func=fn, dry_run=False)
    args = ap.parse_args()
    args.func(args)

if __name__ == "__main__":
    main()

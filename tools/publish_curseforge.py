#!/usr/bin/env python3
"""把构建好的 jar 上传到 CurseForge。

上传走传统 Upload API(不是只读的 Core API):

    POST https://minecraft.curseforge.com/api/projects/{projectId}/upload-file
    X-Api-Token: 主站 API Token
    User-Agent:  必须带,否则会被 Cloudflare 拦

上传前会用只读 Core API 查一遍项目里已有的文件名,命中则跳过,避免重复上传:
Upload API 没有列文件的端点(GET .../projects/{id}/files 会 403),只有 Core API 能查。

两套凭据/端点不可混用:
  * 上传  minecraft.curseforge.com/api + 主站 Token(curseforge.com/account/api-tokens)
  * 只读  api.curseforge.com/v1        + Core Key($2a$10$...,console.curseforge.com)

凭据来源(按优先级):
  * 上传 Token  环境变量 CURSEFORGE_TOKEN     -> ~/.secrets/curseforge-token.txt
  * Core Key   环境变量 CURSEFORGE_CORE_KEY -> ~/.secrets/curseforge-api-key.txt
Core Key 缺失时只打印警告并跳过查重(仍会上传),方便本地不带 Core Key 时使用。

用法:
  python tools/publish_curseforge.py versions
  python tools/publish_curseforge.py versions --dir jars
  python tools/publish_curseforge.py versions --prefix 0.1.3 --dry-run
  python tools/publish_curseforge.py versions --prefix 0.1.3 --force   # 不做查重
  python tools/publish_curseforge.py existing --prefix 0.1.3           # 只列出已存在的版本
  python tools/publish_curseforge.py probe
"""
import argparse
import json
import mimetypes
import os
import pathlib
import sys
import urllib.error
import urllib.request
import uuid

UPLOAD_API = "https://minecraft.curseforge.com/api"
CORE_API = "https://api.curseforge.com/v1"
REPO = pathlib.Path(__file__).resolve().parent.parent
SECRETS = pathlib.Path(os.path.expanduser("~")) / ".secrets"
TOKEN_FILE = SECRETS / "curseforge-token.txt"
CORE_KEY_FILE = SECRETS / "curseforge-api-key.txt"
DEFAULT_PROJECT_ID = "1689732"
CARPET_PROJECT_ID = 349239
CARPET_SLUG = "carpet"
USER_AGENT = "xiao-hao-ovo/Carpet-Hao-Addition GitHub-Actions"
GAME_VERSIONS = ["1.21", "1.21.1", "1.21.2", "1.21.4", "1.21.6", "1.21.8",
                 "1.21.10", "1.21.11", "26.1.2", "26.2"]
CHANGELOG = ("通过 GitHub Actions 自动发布。需要 Fabric Loader >= 0.16.10(26.x 建议 >= 0.19)与对应版本的 Carpet。"
             "全部规则默认关闭,游戏内用 `/carpet <规则> <值>` 开启。")


def token():
    value = os.environ.get("CURSEFORGE_TOKEN", "").strip()
    if value:
        return value
    if TOKEN_FILE.is_file():
        value = TOKEN_FILE.read_text(encoding="utf-8").strip()
        if value:
            return value
    sys.exit(f"缺少 CurseForge 主站 API Token:设置环境变量 CURSEFORGE_TOKEN 或写入 {TOKEN_FILE}")


def core_key():
    """读取只读 Core API 的 Key;缺失时返回 None(由调用方决定是否降级)。"""
    value = os.environ.get("CURSEFORGE_CORE_KEY", "").strip()
    if not value and CORE_KEY_FILE.is_file():
        value = CORE_KEY_FILE.read_text(encoding="utf-8").strip()
    return value or None


def core_request(method, url, key):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "x-api-key": key}, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            raw = response.read().decode("utf-8", errors="replace")
            try:
                return response.status, (json.loads(raw) if raw else None), raw
            except json.JSONDecodeError:
                return response.status, None, raw
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        try:
            return exc.code, json.loads(raw), raw
        except json.JSONDecodeError:
            return exc.code, None, raw


def existing_file_names(key):
    """列出项目里已存在的文件名(只含已通过审核的文件),按 API 顺序返回(可含同名重复项)。

    读失败时返回 None,调用方据此退化为"不查重",避免因为查询问题挡掉上传。
    """
    if not key:
        return None
    names = []
    index = 0
    while True:
        status, data, raw = core_request("GET", f"{CORE_API}/mods/{project_id()}/files?pageSize=50&index={index}", key)
        if status != 200 or not isinstance(data, dict):
            print(f"WARN 读取 CurseForge 已有文件列表失败(HTTP {status}),跳过查重: {raw[:150]}")
            return None
        items = data.get("data") or []
        for item in items:
            if isinstance(item, dict) and item.get("fileName"):
                names.append(item["fileName"])
        if len(items) < 50:
            break
        index += len(items)
    return names


def project_id():
    value = os.environ.get("CURSEFORGE_PROJECT_ID", "").strip() or DEFAULT_PROJECT_ID
    digits = "".join(ch for ch in value if ch.isdigit())
    if not digits:
        sys.exit(f"projectID 无效: {value!r}")
    return digits


def request(method, url, tok, data=None, headers=None):
    merged = {"User-Agent": USER_AGENT, "X-Api-Token": tok}
    if headers:
        merged.update(headers)
    req = urllib.request.Request(url, data=data, headers=merged, method=method)
    try:
        with urllib.request.urlopen(req, timeout=180) as response:
            raw = response.read().decode("utf-8", errors="replace")
            try:
                return response.status, (json.loads(raw) if raw else None), raw
            except json.JSONDecodeError:
                return response.status, None, raw
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        try:
            return exc.code, json.loads(raw), raw
        except json.JSONDecodeError:
            return exc.code, None, raw


def version_ids(tok, names):
    """把版本名映射为 CurseForge 数字 ID。"""
    status, data, raw = request("GET", f"{UPLOAD_API}/game/versions", tok)
    if status != 200 or not isinstance(data, list):
        sys.exit(f"读取 CurseForge 版本列表失败: HTTP {status}: {raw[:200]}")
    by_name = {}
    for item in data:
        if isinstance(item, dict) and item.get("name") and item.get("id") is not None:
            by_name.setdefault(item["name"], item["id"])
    missing = [n for n in names if n not in by_name]
    if missing:
        sys.exit(f"CurseForge 没有这些版本标签: {', '.join(missing)}")
    return [by_name[n] for n in names]


def multipart(metadata, jar):
    boundary = "----HaoAddition" + uuid.uuid4().hex
    nl = b"\r\n"
    chunks = [
        f"--{boundary}".encode(),
        b'Content-Disposition: form-data; name="metadata"',
        b"Content-Type: application/json", b"",
        json.dumps(metadata, separators=(",", ":")).encode("utf-8"),
        f"--{boundary}".encode(),
        b'Content-Disposition: form-data; name="file"; filename="' + jar.name.encode("utf-8") + b'"',
        f"Content-Type: {mimetypes.guess_type(jar.name)[0] or 'application/java-archive'}".encode(), b"",
        jar.read_bytes(),
        f"--{boundary}--".encode(), b"",
    ]
    return nl.join(chunks), f"multipart/form-data; boundary={boundary}"


def local_jars(directory=None, prefix=None, files=None):
    if files:
        paths = [pathlib.Path(f) for f in files]
    elif directory:
        paths = sorted(pathlib.Path(directory).glob("*.jar"))
    else:
        paths = []
        for pattern in ("build/libs/*.jar", "modern/*/build/libs/*.jar"):
            paths += sorted(REPO.glob(pattern))
    out = []
    for p in paths:
        if not p.is_file() or "sources" in p.name or "-dev" in p.name or "+" not in p.name:
            continue
        if prefix and f"-{prefix}+" not in p.name:
            continue
        out.append((p.name[:-4].split("+", 1)[1], p))
    return sorted(out, key=lambda t: GAME_VERSIONS.index(t[0]) if t[0] in GAME_VERSIONS else 99)


def upload(tok, mc, jar, tag_ids):
    version = jar.name[:-4].split("-", 3)[3].split("+", 1)[0]
    metadata = {
        "displayName": f"{version} for Minecraft {mc}",
        "changelog": f"版本 {version} · {CHANGELOG}",
        "changelogType": "markdown",
        "gameVersions": tag_ids,
        "releaseType": "release",
        "isMarkedForManualRelease": False,
        "relations": {"projects": [
            {"slug": CARPET_SLUG, "projectID": CARPET_PROJECT_ID, "type": "requiredDependency"}
        ]},
    }
    body, content_type = multipart(metadata, jar)
    return request("POST", f"{UPLOAD_API}/projects/{project_id()}/upload-file", tok, body,
                   {"Content-Type": content_type})


def cmd_versions(args):
    jobs = local_jars(args.dir, args.prefix, args.files)
    if not jobs:
        sys.exit("没有找到可上传的 jar")
    print(f"待上传 {len(jobs)} 个文件:")
    for mc, p in jobs:
        print(f"   {mc:8s} {p.name} ({p.stat().st_size} bytes)")
    if args.dry_run:
        print("--dry-run:未上传")
        return

    tok = token()
    needed = list(dict.fromkeys([mc for mc, _ in jobs] + ["Fabric", "Client", "Server"]))
    ids = version_ids(tok, needed)
    by_name = dict(zip(needed, ids))
    print("版本 ID:", {n: by_name[n] for n in needed})

    # 上传前查重:Upload API 不能列文件,只能借只读 Core API 拿项目里已有的文件名。
    skip_names = set()
    if args.force:
        print("查重: --force,不做查重")
    else:
        key = core_key()
        if not key:
            print("查重: 缺少 Core Key(环境变量 CURSEFORGE_CORE_KEY 或 ~/.secrets/curseforge-api-key.txt),跳过查重")
        else:
            names = existing_file_names(key)
            if names is not None:
                skip_names = set(names)
                print(f"查重: 项目已有 {len(names)} 个文件({len(skip_names)} 个不同文件名)")

    fail = 0
    skipped = 0
    for mc, jar in jobs:
        if jar.name in skip_names:
            skipped += 1
            print(f"skip {mc:8s} {jar.name}(项目里已存在同名文件)")
            continue
        status, data, raw = upload(tok, mc, jar, [by_name[mc], by_name["Fabric"], by_name["Client"], by_name["Server"]])
        if 200 <= status < 300:
            fid = data.get("id") if isinstance(data, dict) else "?"
            print(f"OK   {mc:8s} {jar.name} file_id={fid}")
        elif status in (400, 409) and any(t in raw.lower() for t in ("already exists", "duplicate", "same file")):
            print(f"skip {mc:8s} {jar.name}(已存在)")
        else:
            fail += 1
            print(f"FAIL {mc:8s} {jar.name} http={status} {raw[:200]}")
    if fail:
        sys.exit(f"{fail} 个文件上传失败")


def cmd_existing(args):
    """只列出项目里已存在的文件名(用于核对是否有多余/重复文件)。"""
    key = core_key()
    if not key:
        sys.exit(f"需要 Core Key:设置环境变量 CURSEFORGE_CORE_KEY 或写入 {CORE_KEY_FILE}")
    names = existing_file_names(key)
    if names is None:
        sys.exit("读取 CurseForge 文件列表失败")
    pattern = f"-{args.prefix}+" if args.prefix else ""
    matched = [n for n in names if not pattern or pattern in n]
    counts = {}
    for n in matched:
        counts[n] = counts.get(n, 0) + 1
    print(f"项目 {project_id()} 共 {len(names)} 个文件,匹配 {len(matched)} 个({len(counts)} 个不同文件名):")
    for n in sorted(counts):
        print(f"  x{counts[n]}  {n}" if counts[n] > 1 else f"  {n}")
    dupes = {n: c for n, c in counts.items() if c > 1}
    if dupes:
        print(f"注意:存在同名重复文件 {len(dupes)} 个,API 无法删除,需在 authors.curseforge.com 后台手动清理")


def cmd_probe(args):
    tok = token()
    status, data, raw = request("GET", f"{UPLOAD_API}/game/versions", tok)
    print("game/versions http:", status)
    if status != 200 or not isinstance(data, list):
        print(raw[:300])
        return
    by_name = {}
    for item in data:
        if isinstance(item, dict) and item.get("name"):
            by_name.setdefault(item["name"], item["id"])
    print("  Fabric ->", by_name.get("Fabric"))
    for mc in GAME_VERSIONS:
        print(f"  {mc:8s} -> {by_name.get(mc)}")


def main():
    ap = argparse.ArgumentParser(description="上传 jar 到 CurseForge")
    sub = ap.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("versions", help="上传构建出来的 jar(带查重)")
    p.add_argument("--dir", help="jar 所在目录(CI 里传下载下来的 artifacts 目录)")
    p.add_argument("--prefix", help="只上传文件名里含该版本号的 jar,如 0.1.4")
    p.add_argument("--files", nargs="*", help="显式指定要上传的 jar")
    p.add_argument("--dry-run", action="store_true", help="只打印将上传的文件")
    p.add_argument("--force", action="store_true", help="不做查重,强制上传")
    p.set_defaults(func=cmd_versions, dry_run=False, force=False)

    p = sub.add_parser("existing", help="列出项目里已存在的文件名(核对重复用)")
    p.add_argument("--prefix", help="只看文件名里含该版本号的文件")
    p.set_defaults(func=cmd_existing)

    p = sub.add_parser("probe", help="打印 CurseForge 版本标签对应的数字 ID")
    p.set_defaults(func=cmd_probe)

    args = ap.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()

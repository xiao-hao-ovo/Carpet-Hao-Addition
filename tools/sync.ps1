<#
.SYNOPSIS
    本仓库唯一的同步入口: DSH 与 Reasonix 共用同一份工作副本, 谁改都要先拉后推。
.DESCRIPTION
    默认行为: git fetch --tags, 落后 origin/main 且工作区干净时自动 rebase。
    -Push   : 要求工作区干净, 落后先 rebase, 然后 push origin HEAD:main; 被拒时重试一次。
.EXAMPLE
    .\tools\sync.ps1          # 动手前
    .\tools\sync.ps1 -Push    # 改完构建通过后
#>
param(
    [switch]$Push
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

function Invoke-Git {
    param([string[]]$GitArgs)
    & git @GitArgs
    if ($LASTEXITCODE -ne 0) { throw ("git " + ($GitArgs -join " ") + " 失败") }
}

$dirty = (git status --porcelain | Measure-Object).Count
Invoke-Git @("fetch", "origin", "--tags")

$counts = ((git rev-list --left-right --count "HEAD...origin/main") -split "\s+") | Where-Object { $_ -ne "" }
$ahead = [int]$counts[0]
$behind = [int]$counts[1]
Write-Host ("当前: 领先 " + $ahead + " / 落后 " + $behind + ", 工作区改动 " + $dirty + " 处")

if ($Push) {
    if ($dirty -gt 0) { throw "工作区有未提交改动, 先提交再 -Push" }
    if ($ahead -eq 0 -and $behind -eq 0) { Write-Host "已经在 origin/main 上, 无需推送"; return }
    if ($behind -gt 0) { Invoke-Git @("pull", "--rebase", "origin", "main") }
    try {
        Invoke-Git @("push", "origin", "HEAD:main")
    } catch {
        Write-Host "push 被拒, rebase 后重试一次"
        Invoke-Git @("fetch", "origin")
        Invoke-Git @("rebase", "origin/main")
        Invoke-Git @("push", "origin", "HEAD:main")
    }
    Write-Host "已推送到 origin/main"
    return
}

if ($behind -gt 0) {
    if ($dirty -gt 0) { Write-Host "落后 origin/main 且有未提交改动: 先提交或 stash 再同步"; exit 1 }
    Invoke-Git @("pull", "--rebase", "origin", "main")
    Write-Host "已 rebase 到 origin/main"
} else {
    Write-Host "已是最新"
}

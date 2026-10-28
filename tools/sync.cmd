@echo off
rem 调 tools\sync.ps1（脚本执行策略被限制时用它：Bypass 只作用于这一次进程）
setlocal
set "PS=powershell"
where pwsh >nul 2>nul && set "PS=pwsh"
"%PS%" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync.ps1" %*
exit /b %ERRORLEVEL%

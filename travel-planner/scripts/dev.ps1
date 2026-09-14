# 一键启动：后端在新窗口运行，前端在当前窗口启动开发服务器
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Start-Process -FilePath "cmd" -ArgumentList "/c", "mvn -f backend\pom.xml compile exec:java" -WorkingDirectory $root

Set-Location (Join-Path $root "frontend")
if (-not (Test-Path node_modules)) { npm install }
npm run dev

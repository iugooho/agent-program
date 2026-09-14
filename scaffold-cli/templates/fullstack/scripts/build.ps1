# 全量构建：后端打包 + 前端生产构建
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "==> 后端 mvn package" -ForegroundColor Cyan
& mvn -B -f (Join-Path $root "backend\pom.xml") package

Write-Host "==> 前端 npm run build" -ForegroundColor Cyan
Set-Location (Join-Path $root "frontend")
if (-not (Test-Path node_modules)) { npm install }
npm run build

# 生产构建
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path node_modules)) { npm install }
npm run build

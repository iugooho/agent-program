# 安装依赖（首次）并启动开发服务器
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path node_modules)) { npm install }
npm run dev

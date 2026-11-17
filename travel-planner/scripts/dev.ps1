# 一键启动：后端在新窗口运行，前端在当前窗口启动开发服务器
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env.local"

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "缺少 .env.local。请先复制 .env.local.example 并替换其中的 CHANGE_ME_* 占位符。"
}

foreach ($rawLine in Get-Content -LiteralPath $envFile) {
    $line = $rawLine.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
        continue
    }
    $separator = $line.IndexOf("=")
    if ($separator -lt 1) {
        throw ".env.local 存在无效配置行：$rawLine"
    }
    $name = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim().Trim('"')
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

Start-Process -FilePath "cmd" -ArgumentList "/c", "mvn -f backend\pom.xml compile exec:java" -WorkingDirectory $root

Set-Location (Join-Path $root "frontend")
if (-not (Test-Path node_modules)) { npm install }
npm run dev

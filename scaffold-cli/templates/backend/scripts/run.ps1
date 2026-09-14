# 编译并运行 App（等价于 mvn compile exec:java）
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
& mvn -q compile exec:java

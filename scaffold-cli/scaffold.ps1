# scaffold-cli launcher (Windows PowerShell)
#
# Usage:
#   .\scaffold.ps1 list
#   .\scaffold.ps1 init my-app --type backend
#   .\scaffold.ps1 init my-shop --type fullstack --deps spring-core,junit,logback
#   .\scaffold.ps1 init my-shop --type fullstack --deps spring-core junit logback
#
# Builds the jar on first run, then runs it.
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    $Args
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$jar = Join-Path $root "target\scaffold-cli-1.0.0.jar"

# Chinese output needs UTF-8 console + UTF-8 java streams.
try { chcp 65001 > $null } catch { }
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

if (-not (Test-Path $jar)) {
    $mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source
    if (-not $mvn -and $env:MAVEN_HOME) { $mvn = Join-Path $env:MAVEN_HOME "bin\mvn.cmd" }
    if (-not $mvn) { throw "Maven not found. Install Maven or set MAVEN_HOME." }
    Write-Host "==> building scaffold-cli" -ForegroundColor Cyan
    & $mvn -B "-Dmaven.test.skip=true" -f (Join-Path $root "pom.xml") package
    if ($LASTEXITCODE -ne 0) { throw "mvn package failed with exit code $LASTEXITCODE" }
}

# PowerShell turns `a,b,c` into a nested array, so flatten recursively:
# it must reach java as `--deps a b c`, which the CLI accepts.
function Expand-ScaffoldArgs($items) {
    $flat = @()
    foreach ($item in $items) {
        if ($item -is [System.Array]) { $flat += Expand-ScaffoldArgs $item }
        else { $flat += $item }
    }
    return $flat
}

# @(...) is required: PowerShell unrolls a single-element array on return, and
# splatting a bare string passes it character by character ("list" -> l i s t).
$tokens = @(Expand-ScaffoldArgs $Args)
$jvmArgs = @("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-jar", $jar)

& java @jvmArgs @tokens
exit $LASTEXITCODE

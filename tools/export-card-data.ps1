# Export every card's full data to docs/cards-export.txt (and .md)  -- v0.5.5.4
#
# ASCII-ONLY on purpose: Chinese text in a .ps1 needs a UTF-8 BOM for Windows PowerShell 5.1 and any
# editor that rewrites the file can drop it (docs/handover.md section 7). All Chinese output comes
# from the Java exporter instead.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools\export-card-data.ps1
#   powershell -ExecutionPolicy Bypass -File tools\export-card-data.ps1 -Out build\export
#
# Produces (UTF-8):
#   <out>/cards-export.txt  full per-card dump  (opens in Notepad / Word)
#   <out>/cards-export.md   the same content as Markdown
#
# "Effect" text is the card face wording; the engine implements effects in Java, so the face text is
# the only faithful textual form of a card's effect.

[CmdletBinding()]
param(
    [string]$Out = 'docs'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'D:\itheima\environments\openjdk21' }
$java = Join-Path $jdk 'bin\java.exe'
$javac = Join-Path $jdk 'bin\javac.exe'
if (-not (Test-Path $java)) {
    Write-Host "JDK not found: $jdk (set JAVA_HOME)" -ForegroundColor Red
    exit 1
}

# Engine core is Minecraft-free, so this needs no NeoForm classpath.
$coreOut = 'build\export-core'
if (Test-Path $coreOut) { Remove-Item $coreOut -Recurse -Force }
New-Item -ItemType Directory -Force -Path $coreOut | Out-Null

$coreSrc = Get-ChildItem 'src\main\java\com\slaythecraft\combat\core' -Filter *.java |
    Select-Object -ExpandProperty FullName
$compile = & $javac -encoding UTF-8 -d $coreOut @coreSrc 'tools\ExportCardData.java' 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "failed to compile the card-data exporter:" -ForegroundColor Red
    $compile | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    exit 1
}

$lines = & $java '-Dstdout.encoding=UTF-8' -cp $coreOut ExportCardData $Out 2>&1
foreach ($line in $lines) { Write-Host $line }
if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Host "done. open $Out\cards-export.txt (UTF-8) in Notepad or Word." -ForegroundColor Green

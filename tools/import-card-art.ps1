# Import card / relic art into the mod resources (v0.5.5.1).
#
# ASCII-ONLY on purpose: Chinese text in a .ps1 requires a UTF-8 BOM for Windows PowerShell 5.1,
# and any editor that rewrites the file can drop the BOM (we lost it twice already, see
# docs/handover.md section 7). All Chinese output is produced by the Java probe instead.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools\import-card-art.ps1 -List     # write the checklist only
#   powershell -ExecutionPolicy Bypass -File tools\import-card-art.ps1 -Import   # copy assets/cards/*.png
#   powershell -ExecutionPolicy Bypass -File tools\import-card-art.ps1 -Import -Relics
#
# Where the files go:
#   assets/cards/<card id>.png   -> src/main/resources/assets/stc/textures/card/<card id>.png
#   assets/relics/<relic id>.png -> src/main/resources/assets/stc/textures/relic/<relic id>.png
#
# The game reads exactly those paths (see CardArt / RelicIcons), so after copying + rebuilding the
# jar the art shows up everywhere: hand cards, codex, deck lists and the relic bar.

[CmdletBinding()]
param(
    [switch]$List,
    [switch]$Import,
    [switch]$Relics
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

# ---------------------------------------------------------------- 1) card id checklist
$coreOut = 'build\art-core'
if (Test-Path $coreOut) { Remove-Item $coreOut -Recurse -Force }
New-Item -ItemType Directory -Force -Path $coreOut | Out-Null
$coreSrc = Get-ChildItem 'src\main\java\com\slaythecraft\combat\core' -Filter *.java |
    Select-Object -ExpandProperty FullName
& $javac -encoding UTF-8 -d $coreOut @coreSrc 'tools\ExportCardIds.java' 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "failed to compile the card-id probe" -ForegroundColor Red
    exit 1
}
$checklistDir = 'assets\cards'
$probe = & $java '-Dstdout.encoding=UTF-8' -cp $coreOut ExportCardIds $checklistDir 2>&1
$ids = @()
foreach ($line in $probe) {
    if ($line -match '^([a-z0-9_]+)\t') { $ids += $Matches[1] }
}
$total = ($probe | Where-Object { $_ -match '^cards=(\d+)$' } | ForEach-Object { $Matches[1] }) | Select-Object -Last 1
$generated = ($probe | Where-Object { $_ -match '^generated=(\d+)$' } | ForEach-Object { $Matches[1] }) | Select-Object -Last 1
Write-Host ("card ids: " + $ids.Count + " (generated: " + $generated + ")")
Write-Host ("checklist written to " + $checklistDir + "\card-art-checklist.md and .csv")
if ($total) { Write-Host ("ids that can take a texture: " + $total) }

if (-not $Import) {
    Write-Host "run with -Import to copy the art files." -ForegroundColor Yellow
    exit 0
}

# ---------------------------------------------------------------- 2) copy card art
$srcDir = 'assets\cards'
$dstDir = 'src\main\resources\assets\stc\textures\card'
New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
$copied = 0
$unknown = @()
if (Test-Path $srcDir) {
    foreach ($file in Get-ChildItem $srcDir -Filter *.png -File) {
        $id = $file.BaseName
        if ($ids -notcontains $id) { $unknown += $id }
        Copy-Item $file.FullName (Join-Path $dstDir ($id + '.png')) -Force
        $copied++
    }
}
Write-Host ("card art copied: " + $copied + " -> " + $dstDir)
if ($unknown.Count -gt 0) {
    Write-Host ("names that are not card ids (check the spelling): " + ($unknown -join ', ')) -ForegroundColor Yellow
}
$missing = @()
foreach ($id in $ids) {
    if (-not (Test-Path (Join-Path $dstDir ($id + '.png')))) { $missing += $id }
}
Write-Host ("cards still without art: " + $missing.Count + " / " + $ids.Count)
if ($missing.Count -gt 0 -and $missing.Count -le 20) {
    Write-Host ("  " + ($missing -join ', '))
}

# ---------------------------------------------------------------- 3) copy relic art
if ($Relics) {
    $relicSrc = 'assets\relics'
    $relicDst = 'src\main\resources\assets\stc\textures\relic'
    New-Item -ItemType Directory -Force -Path $relicDst | Out-Null
    $relicCopied = 0
    if (Test-Path $relicSrc) {
        foreach ($file in Get-ChildItem $relicSrc -Filter *.png -File) {
            Copy-Item $file.FullName (Join-Path $relicDst ($file.BaseName + '.png')) -Force
            $relicCopied++
        }
    }
    Write-Host ("relic art copied: " + $relicCopied + " -> " + $relicDst)
}

Write-Host "done. rebuild with: .\gradlew.bat build --offline" -ForegroundColor Green

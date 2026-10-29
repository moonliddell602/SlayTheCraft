# Run every self-check suite (v0.5.0.7).
#
# Usage:
#   .\tools\run-checks.ps1                 # engine self-test + every suite
#   .\tools\run-checks.ps1 -Only Chat      # only suites whose name matches "Chat"
#   .\tools\run-checks.ps1 -EngineOnly     # engine self-test only (no Minecraft classpath needed)
#   .\tools\run-checks.ps1 -SkipEngine     # suites only
#
# IMPORTANT (v0.5.0.7): this script is deliberately **plain ASCII** and never matches on the
# JVM's Chinese output. It used to contain Chinese strings, which require a UTF-8 BOM to survive
# Windows PowerShell 5.1; losing the BOM (any editor/tool that rewrites the file) turned the whole
# script into mojibake and broke it. Now pass/fail comes from exit codes and the counts come from
# the language-independent "<n> / <n>" pattern, so the file's encoding cannot break it.
#
# Exit code: 0 = everything passed, 1 = something failed.

[CmdletBinding()]
param(
    [string]$Only = '',
    [switch]$EngineOnly,
    [switch]$SkipEngine
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$Root = Split-Path -Parent $scriptDir
Set-Location $Root

$jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'D:\itheima\environments\openjdk21' }
$java = Join-Path $jdk 'bin\java.exe'
$javac = Join-Path $jdk 'bin\javac.exe'
if (-not (Test-Path $java)) {
    Write-Host "JDK not found: $jdk (set JAVA_HOME)" -ForegroundColor Red
    exit 1
}

$failed = 0

# ---------------------------------------------------------------- engine self-test
if (-not $SkipEngine) {
    $coreOut = 'build\selftest-core'
    if (Test-Path $coreOut) { Remove-Item $coreOut -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $coreOut | Out-Null
    $coreSrc = Get-ChildItem 'src\main\java\com\slaythecraft\combat\core' -Filter *.java |
        Select-Object -ExpandProperty FullName
    $compile = & $javac -encoding UTF-8 -d $coreOut @coreSrc 'src\test\java\com\slaythecraft\combat\core\CombatSelfTestMain.java' 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "engine self-test failed to compile:" -ForegroundColor Red
        $compile | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
        exit 1
    }
    $engineOut = & $java '-Dstdout.encoding=UTF-8' -cp $coreOut com.slaythecraft.combat.core.CombatSelfTestMain 2>&1
    # The JVM prints its own summary in Chinese; just echo it verbatim. The summary is the only line
    # shaped "<n> <word> / <n> <word>" (plain PASS lines can also contain "11 / 7", so the pattern
    # must require the unit words on both sides).
    $engineSummary = ($engineOut | Where-Object { $_ -match '\d+\s+\S+\s*/\s*\d+\s+\S+' } | Select-Object -Last 1)
    if (-not $engineSummary) { $engineSummary = '(no summary line)' }
    if ($LASTEXITCODE -eq 0) {
        Write-Host ("  [engine] OK   " + $engineSummary) -ForegroundColor Green
    } else {
        Write-Host ("  [engine] FAIL " + $engineSummary) -ForegroundColor Red
        $engineOut | Where-Object { $_ -match '^FAIL' } | Select-Object -First 20 |
            ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
        $failed++
    }
}

if ($EngineOnly) { exit $failed }

# ---------------------------------------------------------------- suites
$cpFile = 'build\neoForm\neoFormJoined1.21.1-20240808.144430\steps\writeMinecraftClasspathData\classpath.txt'
if (-not (Test-Path $cpFile)) {
    Write-Host "missing $cpFile -- run .\gradlew.bat compileJava compileTestJava first" -ForegroundColor Red
    exit 1
}
# NOTE: keep the classpath construction exactly as it always was. This file is ASCII-only now, but
# the classpath file itself may start with a UTF-8 BOM; Get-Content -Encoding UTF8 handles it.
$cp = 'build\classes\java\main;build\classes\java\test;build\resources\main;' +
    ((Get-Content $cpFile -Encoding UTF8) -join ';')

$suites = @(
    'com.slaythecraft.ConfigDefaultsCheck',
    'com.slaythecraft.api.StcApiCheck',
    'com.slaythecraft.binder.CardDeleteServiceCheck',
    'com.slaythecraft.binder.CuriosSlotCheck',
    'com.slaythecraft.cardreg.CardRegistryCheck',
    'com.slaythecraft.character.CharacterServiceCheck',
    'com.slaythecraft.chat.ChatOutputCheck',
    'com.slaythecraft.client.BinderSkinCheck',
    'com.slaythecraft.client.IconFitCheck',
    'com.slaythecraft.client.CodexPagesCheck',
    'com.slaythecraft.client.SoundAssetCheck',
    'com.slaythecraft.combat.BattleTitleCheck',
    'com.slaythecraft.combat.DefeatFlowCheck',
    'com.slaythecraft.data.BossCodexCheck',
    'com.slaythecraft.data.ModMetadataCheck',
    'com.slaythecraft.data.AdvancementCheck',
    'com.slaythecraft.data.CardDataCodecCheck',
    'com.slaythecraft.data.MobTierTagCheck',
    'com.slaythecraft.data.RecipeCheck',
    'com.slaythecraft.merchant.CardShopCheck',
    'com.slaythecraft.network.BattleViewCheck',
    'com.slaythecraft.question.QuestionCheck',
    'com.slaythecraft.reward.RewardRollerCheck'
)

$totalPass = 0
$totalFail = 0
foreach ($suite in $suites) {
    if ($Only -and $suite -notmatch $Only) { continue }
    $out = & $java '-Dstdout.encoding=UTF-8' -cp $cp $suite 2>&1
    # Same trick as the engine summary: require the unit words around the slash so that ordinary
    # PASS lines like "... 11 / 7" are never mistaken for the summary.
    $summary = ($out | Where-Object { $_ -match '\d+\s+\S+\s*/\s*\d+\s+\S+' } | Select-Object -Last 1)
    if (-not $summary) { $summary = '(no output -- did the suite crash?)' }
    # Counts come from the language-independent "<n> <word> / <n> <word>" summary fragment.
    $m = [regex]::Match($summary, '(\d+)\s+\S+\s*/\s*(\d+)\s+\S+')
    if ($m.Success) {
        $totalPass += [int]$m.Groups[1].Value
        $totalFail += [int]$m.Groups[2].Value
    }
    $short = $suite.Split('.')[-1]
    if ($LASTEXITCODE -eq 0) {
        Write-Host ("  [OK]   {0,-26} {1}" -f $short, $summary) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] {0,-26} {1}" -f $short, $summary) -ForegroundColor Red
        $out | Where-Object { $_ -match '^FAIL' } | ForEach-Object { Write-Host "      $_" -ForegroundColor Red }
        $failed++
    }
}

Write-Host ''
Write-Host ("suites total: $totalPass passed / $totalFail failed; failing suites: $failed") -ForegroundColor Cyan
if ($failed -gt 0) { exit 1 }
exit 0

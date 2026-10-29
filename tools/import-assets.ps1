# 把 `assets/` 里的素材同步进打包目录（v0.4.6.6+）
#
# 用法：
#   .\tools\import-assets.ps1           # 预览（dry-run，只打印会做什么，不动文件）
#   .\tools\import-assets.ps1 -Apply    # 真的复制
#
# 只复制素材文件（.png/.jpg/.jpeg/.ogg/.ttf/.otf/.json），跳过 README.md 之类的说明文件。
# 映射关系（左 = 素材工作区，右 = 打包目录）：
#   assets/cards/**                        -> src/main/resources/assets/stc/textures/card/**
#   assets/textures/items/**               -> src/main/resources/assets/stc/textures/item/**
#   assets/textures/blocks/**              -> src/main/resources/assets/stc/textures/block/**
#   assets/textures/gui/**                 -> src/main/resources/assets/stc/textures/gui/**
#   assets/textures/status/**              -> src/main/resources/assets/stc/textures/status/**
#   assets/textures/entity/**              -> src/main/resources/assets/stc/textures/entity/**
#   assets/models/item/**                  -> src/main/resources/assets/stc/models/item/**
#   assets/models/block/**                 -> src/main/resources/assets/stc/models/block/**
#   assets/sounds/**                       -> src/main/resources/assets/stc/sounds/**
#   assets/sounds/sounds.json              -> src/main/resources/assets/stc/sounds.json   ← 注意位置！
#   assets/fonts/**                        -> src/main/resources/assets/stc/font/**
#   assets/lang/**                         -> 只提示（语言文件需要人工合并，避免覆盖现有键）
#
# ============================ 音频两个大坑 ============================
# 1) 只支持 Ogg Vorbis（.ogg）。mp3 / wav / m4a / flac 一律**不会被复制**，
#    就算硬塞进包里 Minecraft 也放不出来（不是音量问题，是解码器不支持）。
#    转换命令：ffmpeg -i in.mp3 -c:a libvorbis -q:a 6 out.ogg
# 2) sounds.json 必须放在 `assets/stc/sounds.json`（assets/stc 根目录下），
#    **不是** `assets/stc/sounds/sounds.json`。放错位置 = 所有声音事件都加载不到。
# 3) 以 `_` 开头的子目录（例如 assets/sounds/_originals/）只当"原件/参考素材"存放处，
#    不会被导入、也不会报警 —— 转换前的 mp3 就放那里。
# =====================================================================

[CmdletBinding()]
param(
    [switch]$Apply,
    [string]$Root = ''
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# 脚本所在目录（$PSScriptRoot 在有些调用方式下为空，这里两种都兼容）
$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path -Parent $scriptDir
}
$Root = (Resolve-Path $Root).Path

$sourceRoot = Join-Path $Root 'assets'
$targetRoot = Join-Path $Root 'src\main\resources\assets\stc'

if (-not (Test-Path $sourceRoot)) {
    Write-Host "找不到素材工作区：$sourceRoot" -ForegroundColor Red
    exit 1
}

# 素材工作区的子目录 → 打包目录
$map = [ordered]@{
    'cards'                                  = 'textures\card'
    'textures\items'                         = 'textures\item'
    'textures\blocks'                        = 'textures\block'
    'textures\gui'                           = 'textures\gui'
    'textures\status'                        = 'textures\status'
    'textures\entity'                        = 'textures\entity'
    'models\item'                            = 'models\item'
    'models\block'                           = 'models\block'
    'sounds'                                 = 'sounds'
    'fonts'                                  = 'font'
}

$allowed = @('.png', '.jpg', '.jpeg', '.ogg', '.ttf', '.otf', '.json')
# Minecraft 只能解码 Ogg Vorbis；这些一律不复制，只报警
$audioBad = @('.mp3', '.wav', '.m4a', '.flac', '.aac', '.wma', '.opus', '.aiff')
$copied = 0
$planned = @()
$soundEvents = @()
$unsupported = @()

foreach ($key in $map.Keys) {
    $from = Join-Path $sourceRoot $key
    if (-not (Test-Path $from)) { continue }
    $to = Join-Path $targetRoot $map[$key]

    $files = Get-ChildItem -Path $from -Recurse -File
    foreach ($file in $files) {
        $ext = $file.Extension.ToLowerInvariant()
        $relative = $file.FullName.Substring($from.Length).TrimStart('\')

        # 以 _ 开头的目录（如 _originals）= 原件/参考素材存放处：不导入，也不报警
        if (($relative -split '\\') | Where-Object { $_ -like '_*' }) { continue }

        if ($audioBad -contains $ext) {
            $unsupported += [pscustomobject]@{ File = $file.FullName.Substring($sourceRoot.Length).TrimStart('\'); Ext = $ext }
            continue
        }
        if ($allowed -notcontains $ext) { continue }

        $destination = Join-Path $to $relative
        # sounds.json 是唯一的例外：它必须落在 assets/stc/ 根目录，而不是 assets/stc/sounds/ 里
        if ($key -eq 'sounds' -and $relative -eq 'sounds.json') {
            $destination = Join-Path $targetRoot 'sounds.json'
        }
        $planned += [pscustomobject]@{ From = $file.FullName; To = $destination }

        if ($ext -eq '.ogg') {
            $soundPath = $relative -replace '\\', '/' -replace '\.ogg$', ''
            if ($relative -match '^boss\\floor_(\d+)\\') {
                # BOSS 音乐有"约定事件名"：放好文件就响，不用改代码
                $soundEvents += "stc:boss_music_floor_$($Matches[1])  <= 放好就生效（约定事件名，无需写代码）"
            } else {
                $soundEvents += "stc:$soundPath"
            }
        }

        if ($Apply) {
            $dir = Split-Path -Parent $destination
            if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
            Copy-Item -LiteralPath $file.FullName -Destination $destination -Force
            $copied++
        }
    }
}

if ($planned.Count -eq 0 -and $unsupported.Count -eq 0) {
    Write-Host "素材工作区里还没有可导入的文件（只认 $($allowed -join ', ')）。" -ForegroundColor Yellow
    Write-Host "先按 assets\README.md 的规则把素材放进 assets\cards、assets\textures\... 等子目录。"
    exit 0
}

foreach ($item in $planned) {
    $short = $item.From.Replace("$Root\", '')
    $shortTo = $item.To.Replace("$Root\", '')
    if ($Apply) {
        Write-Host "  [已复制] $short  ->  $shortTo" -ForegroundColor Green
    } else {
        Write-Host "  [待复制] $short  ->  $shortTo"
    }
}

Write-Host ""
if (-not $Apply) {
    Write-Host "共 $($planned.Count) 个文件待导入（dry-run）。加 -Apply 才会真的复制。" -ForegroundColor Cyan
} else {
    Write-Host "共导入 $copied 个文件到 src\main\resources\assets\stc\。" -ForegroundColor Cyan
}

# 不能用的音频：必须显眼地喊出来，否则作者会以为"放进去了怎么没声音"
if ($unsupported.Count -gt 0) {
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host "!! 有 $($unsupported.Count) 个音频文件格式不被 Minecraft 支持，已跳过（没有复制）" -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    $unsupported | ForEach-Object { Write-Host "  [跳过] assets\$($_.File)   ($($_.Ext))" -ForegroundColor Red }
    Write-Host ""
    Write-Host "Minecraft 只能播放 Ogg Vorbis（.ogg）。请先转换再导入：" -ForegroundColor Yellow
    Write-Host "  ffmpeg -i 原文件.mp3 -c:a libvorbis -q:a 6 目标.ogg" -ForegroundColor Yellow
    Write-Host "转好后把 .ogg 按原目录结构放回 assets\sounds\ 再跑一次本脚本。" -ForegroundColor Yellow
}

if ($soundEvents.Count -gt 0) {
    Write-Host ""
    Write-Host "检测到 .ogg，对应声音事件名：" -ForegroundColor DarkGray
    $soundEvents | Sort-Object -Unique | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
    Write-Host "非 BOSS 的音效需要在 assets\stc\sounds.json 里补一条事件定义。" -ForegroundColor DarkGray
}

# 语言文件单独提醒
$langDir = Join-Path $sourceRoot 'lang'
if (Test-Path $langDir) {
    $langFiles = Get-ChildItem -Path $langDir -Recurse -File -Filter *.json
    if ($langFiles.Count -gt 0) {
        Write-Host ""
        Write-Host "assets\lang 下有 $($langFiles.Count) 个语言 JSON：这些**不会**被自动复制" -ForegroundColor Yellow
        Write-Host "（避免覆盖已有键）。放好后告诉我，我按语言合并进 zh_cn.json / en_us.json。"
    }
}
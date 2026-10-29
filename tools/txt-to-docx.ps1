# Convert an exported .txt into a real .docx (Word / WPS / LibreOffice can open it).
#
# ASCII-ONLY on purpose (see docs/handover.md section 7: Chinese in a .ps1 needs a BOM and any
# editor rewrite can drop it; mojibake then breaks the script).
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools\txt-to-docx.ps1 -In docs\cards-export.txt -Out docs\cards-export.docx
#
# A .docx is just a ZIP with three XML parts, so no Office / no library is needed offline:
#   [Content_Types].xml, _rels/.rels, word/document.xml
# One paragraph per input line, monospace-ish font so the ASCII layout stays readable.

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$In,
    [Parameter(Mandatory = $true)][string]$Out
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$inPath = (Resolve-Path $In).Path
$outPath = if ([System.IO.Path]::IsPathRooted($Out)) { $Out } else { Join-Path $root $Out }
$outDir = Split-Path -Parent $outPath
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
if (Test-Path $outPath) { Remove-Item $outPath -Force }

Add-Type -AssemblyName System.IO.Compression | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem | Out-Null

function ConvertTo-XmlText([string]$text) {
    return $text.Replace('&', '&amp;').Replace('<', '&lt;').Replace('>', '&gt;')
}

$contentTypes = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>
'@

$rels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
'@

# ---- build word/document.xml (one paragraph per line)
$body = New-Object System.Text.StringBuilder
foreach ($line in [System.IO.File]::ReadAllLines($inPath, [System.Text.Encoding]::UTF8)) {
    $escaped = ConvertTo-XmlText $line
    [void]$body.Append('<w:p><w:pPr><w:spacing w:after="0" w:line="240" w:lineRule="auto"/></w:pPr>')
    [void]$body.Append('<w:r><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:eastAsia="Microsoft YaHei"/><w:sz w:val="18"/></w:rPr>')
    [void]$body.Append('<w:t xml:space="preserve">').Append($escaped).Append('</w:t></w:r></w:p>')
    [void]$body.Append("`n")
}
$document = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' + "`n" +
    '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>' + "`n" +
    $body.ToString() + '</w:body></w:document>'

# ---- pack the three parts
$utf8 = New-Object System.Text.UTF8Encoding($false)
$zip = [System.IO.Compression.ZipFile]::Open($outPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $parts = @(
        @{ Name = '[Content_Types].xml'; Text = $contentTypes },
        @{ Name = '_rels/.rels'; Text = $rels },
        @{ Name = 'word/document.xml'; Text = $document }
    )
    foreach ($part in $parts) {
        $entry = $zip.CreateEntry($part.Name, [System.IO.Compression.CompressionLevel]::Optimal)
        $stream = $entry.Open()
        $writer = New-Object System.IO.StreamWriter($stream, $utf8)
        try { $writer.Write($part.Text) } finally { $writer.Dispose(); $stream.Dispose() }
    }
} finally {
    $zip.Dispose()
}

# ---- validate what we just wrote (a malformed docx would make Word complain)
$check = [System.IO.Compression.ZipFile]::OpenRead($outPath)
try {
    $names = $check.Entries | ForEach-Object { $_.FullName }
    foreach ($required in @('[Content_Types].xml', '_rels/.rels', 'word/document.xml')) {
        if ($names -notcontains $required) {
            Write-Host "missing part: $required" -ForegroundColor Red
            exit 1
        }
    }
    $reader = New-Object System.IO.StreamReader($check.GetEntry('word/document.xml').Open(), [System.Text.Encoding]::UTF8)
    try { $xml = $reader.ReadToEnd() } finally { $reader.Dispose() }
    [xml]$parsed = $xml   # throws when the XML is not well formed
    $paragraphs = ($parsed.document.body.p | Measure-Object).Count
    Write-Host ("wrote " + $outPath)
    Write-Host ("paragraphs: " + $paragraphs + "  size: " + (Get-Item $outPath).Length + " bytes")
} finally {
    $check.Dispose()
}
Write-Host "ok: open the .docx in Word / WPS / LibreOffice." -ForegroundColor Green

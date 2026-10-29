# D-506: insert the localized pack-base notice into every values-xx/strings.xml.
# ASCII-only driver on purpose - the translations live in the UTF-8 JSON next to it.

$ErrorActionPreference = "Stop"
$res = Join-Path $PSScriptRoot "..\app\src\main\res"
$json = Get-Content (Join-Path $PSScriptRoot "d506_translations.json") -Raw -Encoding UTF8 | ConvertFrom-Json
$utf8 = New-Object System.Text.UTF8Encoding($false)

$done = 0
Get-ChildItem $res -Directory | Where-Object { $_.Name -like "values*" -and $_.Name -ne "values-night" } | ForEach-Object {
    $dir  = $_.Name
    $code = if ($dir -eq "values") { "en" } else { $dir.Substring("values-".Length) }
    $file = Join-Path $_.FullName "strings.xml"
    if (-not (Test-Path $file)) { Write-Output "SKIP (no strings.xml): $dir"; return }
    if ($code -eq "de") { Write-Output "SKIP (already edited by hand): $dir"; return }
    $text = if ($code -eq "en") {
        "Note: today only the German pack is hand-curated. Every other language - including the bundled English - is an automatically built base: missing words, wrong or unwanted corrections and similar glitches usually come from the pack data, not from the app itself. If you know a language well, you can help improve its pack (see the contribution guide in the project repository)."
    } elseif ($json.PSObject.Properties.Name -contains $code) {
        $json.$code
    } else {
        Write-Output "SKIP (no translation): $dir"; return
    }
    $raw = [System.IO.File]::ReadAllText($file)
    if ($raw.Contains("d506_pack_base_notice")) { Write-Output "SKIP (already present): $dir"; return }
    $nl = if ($raw.Contains("`r`n")) { "`r`n" } else { "`n" }
    $insert = "    <string name=""d506_pack_base_notice"">$text</string>$nl"
    $new = $raw -replace "</resources>", ($insert + "</resources>")
    if (-not $new.Contains("d506_pack_base_notice")) { Write-Output "ERROR (anchor not found): $dir"; return }
    [System.IO.File]::WriteAllText($file, $new, $utf8)
    $done++
    Write-Output "OK: $dir"
}
Write-Output "inserted into $done files"

# D-500: insert the C-28 auto-capitalisation switch strings into every values-*/strings.xml.
# ASCII-only driver on purpose - the drafts live in the UTF-8 JSON next to it (established D-478 rule;
# UiLocaleConsistencyTest enforces that every locale defines exactly English's string names).

$ErrorActionPreference = "Stop"
$res = Join-Path $PSScriptRoot "..\app\src\main\res"
$json = Get-Content (Join-Path $PSScriptRoot "d500_translations.json") -Raw -Encoding UTF8 | ConvertFrom-Json
$utf8 = New-Object System.Text.UTF8Encoding($false)

$done = 0
Get-ChildItem $res -Directory | Where-Object { $_.Name -like "values*" -and $_.Name -ne "values-night" } | ForEach-Object {
    $dir  = $_.Name
    $code = if ($dir -eq "values") { "en" } else { $dir.Substring("values-".Length) }
    $file = Join-Path $_.FullName "strings.xml"
    if (-not (Test-Path $file)) { Write-Output "SKIP (no strings.xml): $dir"; return }
    $entry = $json.PSObject.Properties[$code]
    if ($null -eq $entry) { Write-Output "SKIP (no translation): $dir"; return }
    $raw = [System.IO.File]::ReadAllText($file)
    if ($raw.Contains("d500_auto_capitalisation_title")) { Write-Output "SKIP (already present): $dir"; return }
    $anchor = "    <string name=""c10_title"">"
    $idx = $raw.IndexOf($anchor)
    if ($idx -lt 0) { Write-Output "ERROR (anchor not found): $dir"; return }
    $nl = if ($raw.Contains("`r`n")) { "`r`n" } else { "`n" }
    $title = $entry.Value.title.Replace("'", "\'")
    $summary = $entry.Value.summary.Replace("'", "\'")
    $insert = "    <string name=""d500_auto_capitalisation_title"">$title</string>$nl" +
              "    <string name=""d500_auto_capitalisation_summary"">$summary</string>$nl"
    $new = $raw.Insert($idx, $insert)
    [System.IO.File]::WriteAllText($file, $new, $utf8)
    $done++
    Write-Output "OK: $dir"
}
Write-Output "inserted into $done files"

# D-507: replaces d59_title/d59_summary with the five d507_combined_key_* strings in every locale
# strings.xml (the English base file is edited by hand). Translations come from d507_strings.json
# (UTF-8, read with an explicit encoding because PowerShell 5.1 would otherwise read the script's
# own non-ASCII payload as ANSI - hence the JSON side file and this ASCII-only script).
$ErrorActionPreference = "Stop"

$root = Join-Path $PSScriptRoot "..\app\src\main\res"
$jsonPath = Join-Path $PSScriptRoot "d507_strings.json"
$translations = [System.IO.File]::ReadAllText($jsonPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$updated = 0

foreach ($property in $translations.PSObject.Properties) {
    $dir = $property.Name
    $strings = $property.Value
    if ($strings.Count -ne 5) { throw "$dir : expected 5 strings, got $($strings.Count)" }

    $path = Join-Path $root (Join-Path $dir "strings.xml")
    if (-not (Test-Path -LiteralPath $path)) { throw "$dir : strings.xml not found" }
    $content = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)

    if ($content -notmatch 'name="d59_title"') { throw "$dir : d59_title not found" }
    if ($content -notmatch 'name="d59_summary"') { throw "$dir : d59_summary not found" }
    if ($content -match 'name="d507_') { throw "$dir : d507 strings already present" }

    $newLines = @(
        "    <string name=""d507_combined_key_title"">$($strings[0])</string>",
        "    <string name=""d507_combined_key_summary"">$($strings[1])</string>",
        "    <string name=""d507_mode_off"">$($strings[2])</string>",
        "    <string name=""d507_mode_symbols"">$($strings[3])</string>",
        "    <string name=""d507_mode_emoji"">$($strings[4])</string>"
    ) -join "`r`n"

    # Replace the d59_title line in place (keeps its position in the file), drop the d59_summary line.
    $pattern = "(?m)^[ `t]*<string name=""d59_title"">.*</string>[ `t]*\r?\n"
    if ($content -notmatch $pattern) { throw "$dir : d59_title line did not match" }
    $content = [regex]::Replace($content, $pattern, ($newLines + "`r`n"), 1)

    $pattern2 = "(?m)^[ `t]*<string name=""d59_summary"">.*</string>[ `t]*\r?\n"
    if ($content -notmatch $pattern2) { throw "$dir : d59_summary line did not match" }
    $content = [regex]::Replace($content, $pattern2, "", 1)

    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
    $updated++
}

Write-Host "updated $updated locale files"

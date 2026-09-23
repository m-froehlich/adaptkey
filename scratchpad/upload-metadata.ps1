# Uploads gitlab_api_payload.json (same folder) as metadata/de.froehlichmedia.adaptkey.yml to the
# F-Droid MR branch in the user's fdroiddata fork. Run it in your own PowerShell window:
#   powershell -ExecutionPolicy Bypass -File .\upload-metadata.ps1
# The token is typed in hidden at the prompt; it is never written to disk and never shared with anyone.

$payload = Join-Path $PSScriptRoot "gitlab_api_payload.json"
if (-not (Test-Path $payload)) {
    Write-Error "gitlab_api_payload.json not found next to this script: $payload"
    exit 1
}

$secure = Read-Host "GitLab personal access token (legacy token, scope api)" -AsSecureString
$token = [System.Net.NetworkCredential]::new("", $secure).Password

$uri = "https://gitlab.com/api/v4/projects/m-froehlich%2Ffdroiddata/repository/files/metadata%2Fde.froehlichmedia.adaptkey.yml"
$body = Get-Content -Raw -Encoding utf8 -Path $payload

try {
    $result = Invoke-RestMethod -Method Put -Uri $uri -Headers @{ "PRIVATE-TOKEN" = $token } -ContentType "application/json" -Body $body
    Write-Host "OK: updated $($result.file_path) on branch $($result.branch)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.ErrorDetails) { Write-Host $_.ErrorDetails.Message }
} finally {
    $token = $null
    $secure = $null
}

Write-Host "Now revoke the token in GitLab (Access -> Personal access tokens -> Revoke)."

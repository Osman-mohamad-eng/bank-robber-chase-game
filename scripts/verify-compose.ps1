param([string]$DashboardUrl = 'http://localhost:3000')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location $taskRoot
try {
    if (-not (Test-Path -LiteralPath '.env')) {
        throw 'Copy .env.example to .env and choose DB_PASSWORD first.'
    }
    & docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Compose configuration failed.' }
    & docker compose up --build --wait --wait-timeout 180
    if ($LASTEXITCODE -ne 0) { throw 'Compose services did not become healthy.' }
    & node scripts/smoke-api.mjs
    if ($LASTEXITCODE -ne 0) { throw 'API smoke checks failed.' }
    $taskResponse = Invoke-WebRequest -UseBasicParsing -Uri $DashboardUrl
    if ($taskResponse.StatusCode -ne 200) { throw 'Dashboard did not return HTTP 200.' }
    Write-Host 'Compose health, API smoke checks and dashboard HTTP checks passed.'
    Write-Host 'Services remain running. Use docker compose down to stop them without deleting database data.'
} finally {
    Pop-Location
}

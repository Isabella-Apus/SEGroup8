param(
    [string]$ComposeFile = (Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..\..")) "compose.yml"),
    [string]$ComposeOverrideFile = "",
    [string]$ProjectName = "segroup8-platform",
    [string]$DatabaseService = "database",
    [string]$DatabaseName = "segroup8_platform",
    [string]$DatabaseUser = "segroup8",
    [string]$DatabasePassword = "segroup8_dev_password"
)

$ErrorActionPreference = "Stop"
$seedFile = Join-Path $PSScriptRoot "data\monolith-start-seed.sql"
if (-not (Test-Path -LiteralPath $ComposeFile)) {
    throw "Compose file not found: $ComposeFile"
}
if (-not (Test-Path -LiteralPath $seedFile)) {
    throw "Performance seed file not found: $seedFile"
}

$composeArgs = @("compose", "-p", $ProjectName, "-f", (Resolve-Path $ComposeFile).Path)
if ($ComposeOverrideFile) {
    if (-not (Test-Path -LiteralPath $ComposeOverrideFile)) {
        throw "Compose override file not found: $ComposeOverrideFile"
    }
    $composeArgs += @("-f", (Resolve-Path $ComposeOverrideFile).Path)
}

$mysqlArgs = @(
    "exec", "-T", $DatabaseService,
    "mysql", "--default-character-set=utf8mb4",
    "-u$DatabaseUser", "-p$DatabasePassword", $DatabaseName
)

Write-Host "Resetting reserved performance rows in project '$ProjectName'..."
Get-Content -LiteralPath $seedFile -Raw -Encoding UTF8 | & docker @composeArgs @mysqlArgs
if ($LASTEXITCODE -ne 0) {
    throw "Performance data reset failed with exit code $LASTEXITCODE"
}

$verifySql = @"
SELECT 'users' AS item, COUNT(*) AS count FROM user WHERE id IN (950001,950002,950003)
UNION ALL SELECT 'new_products', COUNT(*) FROM product WHERE id=980001
UNION ALL SELECT 'secondhand_products', COUNT(*) FROM secondhand_product WHERE id BETWEEN 990001 AND 990042
UNION ALL SELECT 'ongoing_auctions', COUNT(*) FROM product_auction WHERE id BETWEEN 999001 AND 999002 AND status='ONGOING';
"@

& docker @composeArgs exec -T $DatabaseService mysql -N -B "-u$DatabaseUser" "-p$DatabasePassword" $DatabaseName -e $verifySql
if ($LASTEXITCODE -ne 0) {
    throw "Performance data verification failed with exit code $LASTEXITCODE"
}

[CmdletBinding()]
param(
    [string]$ComposeFile = (Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..\..")) "microservices\secondhand-service\compose.acceptance.yml"),
    [string]$ComposeOverrideFile = "",
    [string]$ProjectName = "segroup8-secondhand-performance",
    [string]$DatabaseService = "database",
    [string]$DatabaseName = "secondhand_db",
    [string]$DatabaseUser = "secondhand_app",
    [string]$DatabasePassword = "secondhand_app"
)

$ErrorActionPreference = "Stop"
$seedFile = Join-Path $PSScriptRoot "data\secondhand-service-auction-seed.sql"
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

Write-Host "Resetting reserved secondhand-service performance rows in '$ProjectName'..."
Get-Content -LiteralPath $seedFile -Raw -Encoding UTF8 | & docker @composeArgs @mysqlArgs
if ($LASTEXITCODE -ne 0) {
    throw "secondhand-service performance data reset failed with exit code $LASTEXITCODE"
}

$verifySql = @"
SELECT 'secondhand_products' AS item, COUNT(*) AS count
FROM secondhand_product WHERE id BETWEEN 990041 AND 990050 AND status=1 AND risk_status='APPROVED'
UNION ALL
SELECT 'ongoing_auctions', COUNT(*)
FROM product_auction WHERE id BETWEEN 999001 AND 999010 AND status='ONGOING';
"@
$verification = & docker @composeArgs exec -T $DatabaseService mysql -N -B `
    "-u$DatabaseUser" "-p$DatabasePassword" $DatabaseName -e $verifySql
if ($LASTEXITCODE -ne 0) {
    throw "secondhand-service performance data verification failed with exit code $LASTEXITCODE"
}
$verification | Write-Host
$counts = @{}
foreach ($line in $verification) {
    $parts = $line -split "\s+"
    if ($parts.Count -ge 2) {
        $counts[$parts[0]] = [int]$parts[1]
    }
}
if ($counts["secondhand_products"] -ne 10 -or $counts["ongoing_auctions"] -ne 10) {
    throw "Expected 10 approved products and 10 ongoing auctions; actual: $($verification -join '; ')"
}

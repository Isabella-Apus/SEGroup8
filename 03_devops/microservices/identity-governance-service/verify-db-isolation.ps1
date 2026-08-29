param(
    [string]$ComposeFile = "microservices/identity-governance-service/compose.local.yml"
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($env:IDENTITY_DB_PASSWORD)) {
    throw "IDENTITY_DB_PASSWORD is required"
}

docker compose -f $ComposeFile exec -T -e MYSQL_PWD=$env:IDENTITY_DB_PASSWORD identity-mysql `
    mysql -uidentity_governance_app -e "SELECT COUNT(*) FROM identity_governance_db.user;"
if ($LASTEXITCODE -ne 0) {
    throw "Service account cannot read its own schema"
}

docker compose -f $ComposeFile exec -T -e MYSQL_PWD=$env:IDENTITY_DB_PASSWORD identity-mysql `
    mysql -uidentity_governance_app -e "SELECT COUNT(*) FROM order_db.order_info;"
if ($LASTEXITCODE -eq 0) {
    throw "FAIL: cross-schema query unexpectedly succeeded"
}

Write-Output "PASS: own-schema access succeeded and cross-schema access was denied"
exit 0

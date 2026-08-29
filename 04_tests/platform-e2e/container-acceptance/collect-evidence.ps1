$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$evidenceRoot = Join-Path $PSScriptRoot 'evidence'
$logsRoot = Join-Path $evidenceRoot 'logs'
$rawReportsRoot = Join-Path $evidenceRoot 'raw-reports'
New-Item -ItemType Directory -Force -Path $logsRoot | Out-Null
New-Item -ItemType Directory -Force -Path $rawReportsRoot | Out-Null

Push-Location $repositoryRoot
try {
    $timestamp = Get-Date -Format 'yyyy-MM-ddTHH:mm:ssK'
    "Issue #65 acceptance run: $timestamp" | Set-Content -Encoding utf8 (Join-Path $logsRoot 'run-metadata.txt')

    docker compose -f compose.yml config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'TC-65-01 Compose configuration validation failed.' }
    'PASS: docker compose config --quiet' | Set-Content -Encoding utf8 (Join-Path $logsRoot 'compose-config.txt')

    docker compose -f compose.yml ps | Tee-Object -FilePath (Join-Path $logsRoot 'compose-ps.txt')
    if ($LASTEXITCODE -ne 0) { throw 'TC-65-02 Failed to inspect Compose services.' }

    $serviceIds = @(docker compose -f compose.yml ps -q)
    if ($LASTEXITCODE -ne 0 -or $serviceIds.Count -ne 3) {
        throw "TC-65-02 Expected 3 running services, found $($serviceIds.Count)."
    }
    foreach ($containerId in $serviceIds) {
        $state = docker inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $containerId
        if ($LASTEXITCODE -ne 0 -or $state -ne 'running|healthy') {
            throw "TC-65-02 Container $containerId is not running and healthy: $state"
        }
    }

    $frontendHealth = curl.exe --noproxy '*' --fail --silent --show-error http://127.0.0.1:8088/health
    if ($LASTEXITCODE -ne 0 -or $frontendHealth.Trim() -ne 'ok') { throw 'TC-65-04 Frontend health check failed.' }
    $frontendHealth | Set-Content -Encoding utf8 (Join-Path $logsRoot 'frontend-health.txt')

    curl.exe --noproxy '*' --fail --silent --show-error --output (Join-Path $logsRoot 'frontend-index.html') http://127.0.0.1:8088/
    if ($LASTEXITCODE -ne 0) { throw 'TC-65-04 Frontend homepage request failed.' }

    $backendHealth = curl.exe --noproxy '*' --fail --silent --show-error http://127.0.0.1:8089/actuator/health
    if ($LASTEXITCODE -ne 0 -or $backendHealth -notmatch '"status"\s*:\s*"UP"') { throw 'TC-65-05 Backend health check failed.' }
    $backendHealth | Set-Content -Encoding utf8 (Join-Path $logsRoot 'backend-health.txt')

    $databaseUser = (docker compose -f compose.yml exec -T database printenv MYSQL_USER).Trim()
    $databasePassword = (docker compose -f compose.yml exec -T database printenv MYSQL_PASSWORD).Trim()
    $databaseName = (docker compose -f compose.yml exec -T database printenv MYSQL_DATABASE).Trim()
    $rawDatabaseQuery = @(docker compose -f compose.yml exec -T database mysql "-u$databaseUser" "-p$databasePassword" "-D$databaseName" -N -e 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE(); SELECT COUNT(*) FROM user;')
    if ($LASTEXITCODE -ne 0) { throw 'TC-65-03 Database query failed.' }
    $tableCount = [int]$rawDatabaseQuery[0]
    $userCount = [int]$rawDatabaseQuery[1]
    $databaseQuery = @("table_count=$tableCount", "user_count=$userCount")
    $databaseQuery | Set-Content -Encoding utf8 (Join-Path $logsRoot 'database-query.txt')
    if ($tableCount -le 0 -or $userCount -le 0) { throw 'TC-65-03/06 Database was not initialized with schema and seed data.' }

    docker compose -f compose.yml logs --no-color | Set-Content -Encoding utf8 (Join-Path $logsRoot 'compose-logs.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Failed to collect Compose logs.' }
    docker image ls --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}' 'segroup8/*' | Set-Content -Encoding utf8 (Join-Path $logsRoot 'images.txt')

    $surefireRoot = Join-Path $repositoryRoot 'backend\target\surefire-reports'
    if (Test-Path $surefireRoot) {
        Get-ChildItem -Path (Join-Path $surefireRoot '*') -File -Include '*.xml', '*.txt' |
            Copy-Item -Destination $rawReportsRoot -Force
    }

    @{
        issue = 65
        executedAt = $timestamp
        result = 'PASS'
        services = 3
        tableCount = $tableCount
        userCount = $userCount
        frontendHealth = $frontendHealth.Trim()
        backendHealth = ($backendHealth | ConvertFrom-Json).status
    } | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $evidenceRoot 'result-summary.json')

    Write-Host 'ISSUE-65 ACCEPTANCE: PASS'
}
finally {
    Pop-Location
}

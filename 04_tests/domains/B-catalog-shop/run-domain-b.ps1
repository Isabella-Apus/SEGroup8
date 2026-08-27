[CmdletBinding(DefaultParameterSetName = 'Integration')]
param(
    [Parameter(ParameterSetName = 'Integration')]
    [switch] $Integration,
    [Parameter(ParameterSetName = 'Browser')]
    [switch] $Browser,
    [Parameter(ParameterSetName = 'All')]
    [switch] $All
)

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Resolve-Path (Join-Path $scriptRoot '..\..\..')
$evidenceRoot = Join-Path $scriptRoot 'evidence'
$rawReportsRoot = Join-Path $evidenceRoot 'raw-reports'
$logsRoot = Join-Path $evidenceRoot 'logs'
$schemaPath = Join-Path $workspaceRoot 'backend\src\main\resources\schema.sql'
$seedPath = Join-Path $workspaceRoot 'docker\mysql\02-seed.sql'

New-Item -ItemType Directory -Force -Path $rawReportsRoot, $logsRoot | Out-Null

function Invoke-IntegrationTests {
    Push-Location $workspaceRoot
    try {
        $integrationLog = Join-Path $logsRoot 'api-integration.log'
        & mvn -B --no-transfer-progress -f microservices/pom.xml -Pdomain-b clean test |
            ForEach-Object { $_.TrimEnd() } |
            Tee-Object -FilePath $integrationLog
        if ($LASTEXITCODE -ne 0) { throw "Domain B integration tests failed with exit code $LASTEXITCODE" }

        Get-ChildItem 'microservices/*/target/surefire-reports/*' -File |
            Where-Object { $_.Extension -in '.xml', '.txt' } |
            Copy-Item -Destination $rawReportsRoot -Force

        $suites = Get-ChildItem $rawReportsRoot -Filter 'TEST-*.xml' | ForEach-Object {
            [xml] $report = Get-Content -Raw $_.FullName
            [pscustomobject]@{
                name = [string] $report.testsuite.name
                tests = [int] $report.testsuite.tests
                failures = [int] $report.testsuite.failures
                errors = [int] $report.testsuite.errors
                skipped = [int] $report.testsuite.skipped
                rawReport = $_.Name
            }
        }
        $summary = [ordered]@{
            generatedAt = (Get-Date).ToString('o')
            layer = 'API_INTEGRATION'
            command = 'mvn -B -f microservices/pom.xml -Pdomain-b clean test'
            tests = ($suites | Measure-Object tests -Sum).Sum
            failures = ($suites | Measure-Object failures -Sum).Sum
            errors = ($suites | Measure-Object errors -Sum).Sum
            skipped = ($suites | Measure-Object skipped -Sum).Sum
            suites = @($suites)
            browserE2E = [ordered]@{
                layer = 'BROWSER_E2E'
                result = 'NOT_RUN_BY_INTEGRATION_COMMAND'
            }
        }
        $summary | ConvertTo-Json -Depth 5 |
            Set-Content -Encoding UTF8 (Join-Path $evidenceRoot 'result-summary.json')
    }
    finally {
        Pop-Location
    }
}

function Invoke-BrowserTests {
    Push-Location $workspaceRoot
    try {
        Push-Location frontend
        try {
            & npm run build:real |
                Tee-Object -FilePath (Join-Path $logsRoot 'frontend-build.log')
            if ($LASTEXITCODE -ne 0) { throw "Real frontend build failed with exit code $LASTEXITCODE" }
        }
        finally {
            Pop-Location
        }

        & docker compose up --build --wait --wait-timeout 180
        if ($LASTEXITCODE -ne 0) { throw "Compose startup failed with exit code $LASTEXITCODE" }

        # MySQL only runs docker-entrypoint-initdb.d on a new volume. Reapply the
        # idempotent schema and stable E2E fixtures so an existing local volume
        # exercises the same contract as CI's fresh database.
        Get-Content -Raw $schemaPath | & docker compose exec -T database sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
        if ($LASTEXITCODE -ne 0) { throw "Compose schema refresh failed with exit code $LASTEXITCODE" }
        Get-Content -Raw $seedPath | & docker compose exec -T database sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
        if ($LASTEXITCODE -ne 0) { throw "Compose E2E fixture refresh failed with exit code $LASTEXITCODE" }

        Push-Location frontend
        try {
            $previousE2EOutputDir = $env:E2E_OUTPUT_DIR
            $env:E2E_OUTPUT_DIR = $evidenceRoot
            & npm run e2e:domain-b |
                Tee-Object -FilePath (Join-Path $logsRoot 'browser-e2e.log')
            $browserExitCode = $LASTEXITCODE
        }
        finally {
            $env:E2E_OUTPUT_DIR = $previousE2EOutputDir
            Pop-Location
        }

        $junitPath = Join-Path $evidenceRoot 'playwright-results.xml'
        if (Test-Path $junitPath) {
            Copy-Item $junitPath (Join-Path $rawReportsRoot 'playwright-domain-b-results.xml') -Force
            [xml] $junit = Get-Content -Raw $junitPath
            $browserSummary = [ordered]@{
                layer = 'BROWSER_E2E'
                command = 'npm run e2e:domain-b'
                tests = [int] $junit.testsuites.tests
                failures = [int] $junit.testsuites.failures
                errors = [int] $junit.testsuites.errors
                skipped = [int] $junit.testsuites.skipped
                rawReport = 'playwright-domain-b-results.xml'
            }
            $summaryPath = Join-Path $evidenceRoot 'result-summary.json'
            if (Test-Path $summaryPath) {
                $summary = Get-Content -Raw $summaryPath | ConvertFrom-Json
                $summary.browserE2E = $browserSummary
            }
            else {
                $summary = [ordered]@{
                    generatedAt = (Get-Date).ToString('o')
                    layer = 'BROWSER_E2E'
                    browserE2E = $browserSummary
                }
            }
            $summary | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 $summaryPath
        }

        if ($browserExitCode -ne 0) {
            throw "Domain B browser E2E failed with exit code $browserExitCode"
        }
    }
    finally {
        & docker compose down
        Pop-Location
    }
}

if ($All) {
    Invoke-IntegrationTests
    Invoke-BrowserTests
}
elseif ($Browser) {
    Invoke-BrowserTests
}
else {
    Invoke-IntegrationTests
}

[CmdletBinding()]
param(
    [switch]$KeepFinanceService,
    [switch]$SkipBrowserInstall
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$evidenceRoot = if ($env:E2E_EVIDENCE_ROOT) {
    [System.IO.Path]::GetFullPath($env:E2E_EVIDENCE_ROOT)
} else {
    Join-Path $PSScriptRoot 'evidence\compose-e2e'
}
$logsRoot = Join-Path $evidenceRoot 'logs'
$containerName = 'segroup8-benefits-finance-e2e'
$financeImage = if ($env:FINANCE_IMAGE) { $env:FINANCE_IMAGE } else { 'segroup8/benefits-finance:e2e' }
New-Item -ItemType Directory -Force -Path $logsRoot | Out-Null

if (-not $env:JWT_SECRET) { $env:JWT_SECRET = 'SEGROUP8_E2E_JWT_SIGNING_KEY_2026_V1' }
if (-not $env:E2E_INTERNAL_SERVICE_TOKEN) { $env:E2E_INTERNAL_SERVICE_TOKEN = 'SEGROUP8_E2E_INTERNAL_SERVICE_TOKEN_2026' }
if (-not $env:E2E_FINANCE_DB_PASSWORD) { $env:E2E_FINANCE_DB_PASSWORD = 'benefits_finance_e2e_password_2026' }
if (-not $env:E2E_FINANCE_MIGRATOR_PASSWORD) { $env:E2E_FINANCE_MIGRATOR_PASSWORD = 'benefits_finance_migrator_e2e_2026' }
if (-not $env:E2E_BASE_URL) { $env:E2E_BASE_URL = 'http://127.0.0.1:8088' }
if (-not $env:E2E_FINANCE_BASE_URL) { $env:E2E_FINANCE_BASE_URL = 'http://127.0.0.1:8085' }
if (-not $env:E2E_USERNAME) { $env:E2E_USERNAME = 'user' }
if (-not $env:E2E_PASSWORD) { $env:E2E_PASSWORD = 'user123' }
if (-not $env:E2E_OFFICIAL_SELLER_USERNAME) { $env:E2E_OFFICIAL_SELLER_USERNAME = 'seller' }
if (-not $env:E2E_OFFICIAL_SELLER_PASSWORD) { $env:E2E_OFFICIAL_SELLER_PASSWORD = 'seller123' }
$env:E2E_OUTPUT_DIR = $evidenceRoot

function Invoke-Logged([string]$Name, [scriptblock]$Command) {
    & $Command 2>&1 | Tee-Object -FilePath (Join-Path $logsRoot $Name)
    if ($LASTEXITCODE -ne 0) { throw "$Name failed with exit code $LASTEXITCODE" }
}

function Write-SanitizedImageInspect([string]$Image) {
    $raw = & docker image inspect $Image 2>$null
    if ($LASTEXITCODE -ne 0) { throw "candidate image does not exist: $Image" }
    $item = (($raw -join [Environment]::NewLine) | ConvertFrom-Json)[0]
    $labels = [ordered]@{
        title       = $item.Config.Labels.'org.opencontainers.image.title'
        source      = $item.Config.Labels.'org.opencontainers.image.source'
        revision    = $item.Config.Labels.'org.opencontainers.image.revision'
        created     = $item.Config.Labels.'org.opencontainers.image.created'
        jarSha256   = $item.Config.Labels.'com.segroup8.jar.sha256'
    }
    [ordered]@{
        id           = $item.Id
        repoTags     = $item.RepoTags
        repoDigests  = $item.RepoDigests
        created      = $item.Created
        architecture = $item.Architecture
        os           = $item.Os
        size         = $item.Size
        user         = $item.Config.User
        healthcheck  = $item.Config.Healthcheck
        labels       = $labels
    } | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $logsRoot 'candidate-image-inspect.json')
}

function Write-SanitizedContainerInspect {
    $raw = & docker inspect $containerName 2>$null
    if ($LASTEXITCODE -ne 0) { return }
    $item = (($raw -join [Environment]::NewLine) | ConvertFrom-Json)[0]
    [ordered]@{
        id    = $item.Id
        name  = $item.Name
        image = $item.Config.Image
        state = [ordered]@{
            status     = $item.State.Status
            running    = $item.State.Running
            exitCode   = $item.State.ExitCode
            startedAt  = $item.State.StartedAt
            finishedAt = $item.State.FinishedAt
            health     = $item.State.Health
        }
    } | ConvertTo-Json -Depth 10 | Set-Content -Encoding utf8 (Join-Path $logsRoot 'benefits-finance-inspect.json')
}

Push-Location $repositoryRoot
try {
    $gitSha = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'git rev-parse HEAD failed' }
    $appVersion = if ($env:APP_VERSION) { $env:APP_VERSION } else { 'e2e' }
    $appCommit = if ($env:APP_COMMIT) { $env:APP_COMMIT } else { $gitSha }
    $appBuildTime = if ($env:APP_BUILD_TIME) {
        $env:APP_BUILD_TIME
    } else {
        (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
    }

    Invoke-Logged 'compose-config.log' { docker compose config --quiet }
    Invoke-Logged 'compose-up.log' { docker compose up -d --build --wait database backend frontend }

    $databaseSql = @"
CREATE DATABASE IF NOT EXISTS benefits_finance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'benefits_finance_app'@'%' IDENTIFIED BY '$($env:E2E_FINANCE_DB_PASSWORD)';
ALTER USER 'benefits_finance_app'@'%' IDENTIFIED BY '$($env:E2E_FINANCE_DB_PASSWORD)';
CREATE USER IF NOT EXISTS 'benefits_finance_migrator'@'%' IDENTIFIED BY '$($env:E2E_FINANCE_MIGRATOR_PASSWORD)';
ALTER USER 'benefits_finance_migrator'@'%' IDENTIFIED BY '$($env:E2E_FINANCE_MIGRATOR_PASSWORD)';
GRANT SELECT, INSERT, UPDATE, DELETE ON benefits_finance_db.* TO 'benefits_finance_app'@'%';
GRANT ALL PRIVILEGES ON benefits_finance_db.* TO 'benefits_finance_migrator'@'%';
FLUSH PRIVILEGES;
"@
    $databaseSql | docker compose exec -T database sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' 2>&1 |
        Tee-Object -FilePath (Join-Path $logsRoot 'database-init.log')
    if ($LASTEXITCODE -ne 0) { throw "database-init failed with exit code $LASTEXITCODE" }

    if ($env:SKIP_MAVEN_VERIFY -ne 'true') {
        Invoke-Logged 'maven-verify.log' {
            mvn -B --no-transfer-progress -f microservices/pom.xml -pl benefits-finance-service -am clean verify
        }
    }

    $jarSha256 = 'not-applicable-prebuilt-image'
    if ($env:SKIP_FINANCE_IMAGE_BUILD -ne 'true') {
        $jars = @(Get-ChildItem 'microservices\benefits-finance-service\target\benefits-finance-service-*.jar' -File |
            Where-Object { $_.Name -notlike '*.jar.original' })
        if ($jars.Count -ne 1) { throw "expected exactly one verified candidate JAR, found $($jars.Count)" }
        $jarSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $jars[0].FullName).Hash.ToLowerInvariant()
        Invoke-Logged 'docker-build.log' {
            docker build --pull=false -t $financeImage `
                --build-arg "VCS_REF=$appCommit" `
                --build-arg "BUILD_TIME=$appBuildTime" `
                --build-arg "JAR_SHA256=$jarSha256" `
                microservices/benefits-finance-service
        }
    }
    Write-SanitizedImageInspect $financeImage
    @(
        "gitSha=$gitSha"
        "appVersion=$appVersion"
        "appCommit=$appCommit"
        "appBuildTime=$appBuildTime"
        "jarSha256=$jarSha256"
        "financeImage=$financeImage"
    ) | Set-Content -Encoding utf8 (Join-Path $logsRoot 'candidate-metadata.txt')

    docker rm -f $containerName 2>$null | Out-Null
    $serviceId = docker run -d --name $containerName --network segroup8-platform `
        -p 127.0.0.1:8085:8085 `
        -e 'DB_URL=jdbc:mysql://database:3306/benefits_finance_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' `
        -e DB_USERNAME=benefits_finance_app `
        -e "DB_PASSWORD=$($env:E2E_FINANCE_DB_PASSWORD)" `
        -e FLYWAY_DB_USERNAME=benefits_finance_migrator `
        -e "FLYWAY_DB_PASSWORD=$($env:E2E_FINANCE_MIGRATOR_PASSWORD)" `
        -e "JWT_SECRET=$($env:JWT_SECRET)" `
        -e "INTERNAL_SERVICE_TOKEN=$($env:E2E_INTERNAL_SERVICE_TOKEN)" `
        -e "APP_VERSION=$appVersion" `
        -e "APP_COMMIT=$appCommit" `
        -e "APP_BUILD_TIME=$appBuildTime" `
        $financeImage
    if ($LASTEXITCODE -ne 0) { throw 'benefits-finance docker run failed' }
    $serviceId | Set-Content -Encoding utf8 (Join-Path $logsRoot 'benefits-finance-container-id.txt')

    $ready = $false
    $deadline = (Get-Date).AddSeconds(180)
    do {
        $body = curl.exe --noproxy '*' --fail --silent "$($env:E2E_FINANCE_BASE_URL)/actuator/health/readiness" 2>$null
        if ($LASTEXITCODE -eq 0 -and ($body -join [Environment]::NewLine) -match '"status"\s*:\s*"UP"') {
            $body | Set-Content -Encoding utf8 (Join-Path $logsRoot 'readiness.json')
            $ready = $true
            break
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    if (-not $ready) { throw 'benefits-finance readiness did not become UP' }
    Invoke-Logged 'info.json' { curl.exe --noproxy '*' --fail --silent "$($env:E2E_FINANCE_BASE_URL)/actuator/info" }

    Push-Location (Join-Path $repositoryRoot 'frontend')
    try {
        Invoke-Logged 'npm-ci.log' { npm.cmd ci }
        if (-not $SkipBrowserInstall -and $env:SKIP_BROWSER_INSTALL -ne 'true') {
            Invoke-Logged 'playwright-install.log' { npx.cmd playwright install chromium }
        }
        Invoke-Logged 'playwright.log' {
            npx.cmd playwright test `
                e2e/domain-e/uc21-voucher-lifecycle.spec.ts `
                e2e/domain-e/uc22-claim-checkout.spec.ts `
                e2e/domain-e/uc23-wallet-settlement.spec.ts `
                --workers=1
        }
    } finally {
        Pop-Location
    }
} finally {
    docker logs $containerName 2>&1 | Set-Content -Encoding utf8 (Join-Path $logsRoot 'benefits-finance.log')
    Write-SanitizedContainerInspect
    docker compose ps --all 2>&1 | Set-Content -Encoding utf8 (Join-Path $logsRoot 'compose-ps.txt')
    if (-not $KeepFinanceService -and $env:KEEP_FINANCE_SERVICE -ne 'true') {
        docker rm -f $containerName 2>&1 | Set-Content -Encoding utf8 (Join-Path $logsRoot 'benefits-finance-remove.log')
    }
    Pop-Location
}

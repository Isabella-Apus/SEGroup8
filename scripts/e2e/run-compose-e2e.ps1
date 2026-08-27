[CmdletBinding()]
param(
    [switch]$KeepServices,
    [switch]$ResetDatabase,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$PlaywrightArgs
)

$ErrorActionPreference = 'Continue'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$defaultEvidenceRoot = Join-Path $repositoryRoot '04_tests\platform-e2e\evidence'

function Resolve-ConfiguredPath([string]$Value, [string]$Fallback) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return [System.IO.Path]::GetFullPath($Fallback)
    }

    if ([System.IO.Path]::IsPathRooted($Value)) {
        return [System.IO.Path]::GetFullPath($Value)
    }

    return [System.IO.Path]::GetFullPath(
        (Join-Path $repositoryRoot $Value)
    )
}

$configuredOutputRoot = $env:E2E_OUTPUT_DIR

# 优先使用统一脚手架变量。
# 如果只有 E2E_OUTPUT_DIR，则兼容旧脚本：它同时作为 Evidence 根目录。
$evidenceRoot = if ($env:E2E_EVIDENCE_ROOT) {
    Resolve-ConfiguredPath $env:E2E_EVIDENCE_ROOT $defaultEvidenceRoot
} elseif ($configuredOutputRoot) {
    Resolve-ConfiguredPath $configuredOutputRoot $defaultEvidenceRoot
} else {
    Resolve-ConfiguredPath $null $defaultEvidenceRoot
}

# 只有同时配置两个变量时，才把 Playwright 输出与日志 Evidence 分开。
$playwrightOutputRoot = if ($env:E2E_EVIDENCE_ROOT -and $configuredOutputRoot) {
    Resolve-ConfiguredPath $configuredOutputRoot $evidenceRoot
} else {
    $evidenceRoot
}

$logsRoot = Join-Path $evidenceRoot 'logs'
$logsRoot = Join-Path $evidenceRoot 'logs'
New-Item -ItemType Directory -Force -Path $logsRoot | Out-Null
$failureStagePath = Join-Path $logsRoot 'failure-stage.txt'
if (Test-Path -LiteralPath $failureStagePath) {
    Remove-Item -LiteralPath $failureStagePath -Force
}

Push-Location $repositoryRoot
$scriptExitCode = 0
$composeTouched = $true
$currentStage = 'initialization'

function Save-ComposeDiagnostics {
    try {
        & docker compose ps --all 2>&1 | Set-Content -Encoding utf8 (Join-Path $logsRoot 'compose-ps.txt')
        & docker compose config 2>&1 | Set-Content -Encoding utf8 (Join-Path $logsRoot 'compose-config.yml')
        & docker compose logs --no-color --timestamps 2>&1 | Set-Content -Encoding utf8 (Join-Path $logsRoot 'compose.log')
        foreach ($service in @('frontend', 'backend', 'database')) {
            & docker compose logs --no-color --timestamps $service 2>&1 |
                Set-Content -Encoding utf8 (Join-Path $logsRoot "$service.log")
        }
    } catch {
        Write-Warning "Could not collect complete Compose diagnostics: $($_.Exception.Message)"
    }
}

function Invoke-Logged([string]$LogName, [scriptblock]$Command) {
    $logPath = Join-Path $logsRoot $LogName
    New-Item -ItemType File -Force -Path $logPath | Out-Null
    & $Command 2>&1 | Tee-Object -FilePath $logPath
    $commandExitCode = $LASTEXITCODE
    if ($commandExitCode -ne 0) {
        throw "Command failed ($commandExitCode): $LogName"
    }
}

function Wait-ContainerHealthy([string]$Service, [int]$TimeoutSeconds = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $containerId = (& docker compose ps -q $Service 2>$null).Trim()
        if ($containerId) {
            $state = (& docker inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $containerId 2>$null).Trim()
            Write-Host "${Service}: $state"
            if ($state -eq 'running|healthy') { return }
            if ($state -like 'exited|*' -or $state -like 'dead|*') {
                throw "$Service stopped before becoming healthy ($state)."
            }
        } else {
            Write-Host "${Service}: container not created yet"
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for Compose service '$Service' to become healthy."
}

function Wait-HttpReady([string]$Name, [string]$Url, [string]$Pattern, [int]$TimeoutSeconds = 120) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $responseBody = & curl.exe --noproxy '*' --fail --silent --show-error $Url 2>$null
        if ($LASTEXITCODE -eq 0 -and ($responseBody -join [Environment]::NewLine) -match $Pattern) {
            Write-Host "${Name}: HTTP ready ($Url)"
            return
        }
        # The service is still starting; the bounded loop is the wait strategy.
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for $Name HTTP endpoint: $Url"
}

try {
    if (-not $env:E2E_USERNAME) { $env:E2E_USERNAME = 'user' }
    if (-not $env:E2E_PASSWORD) { $env:E2E_PASSWORD = 'user123' }
    if (-not $env:E2E_ROLE) { $env:E2E_ROLE = 'USER' }
    if (-not $env:E2E_BASE_URL) { $env:E2E_BASE_URL = 'http://127.0.0.1:8088' }
    $env:E2E_OUTPUT_DIR = $playwrightOutputRoot

    if ($ResetDatabase) {
        $currentStage = 'database-reset'
        Write-Warning 'ResetDatabase requested: removing only the Compose project and its named database volume.'
        Invoke-Logged 'compose-reset.log' { docker compose down -v --remove-orphans }
    }

    $currentStage = 'compose-config'
    Invoke-Logged 'compose-config-check.log' { docker compose config --quiet }
    $currentStage = 'compose-build'
    Invoke-Logged 'compose-build.log' { docker compose build backend frontend }
    $currentStage = 'database-start'
    Invoke-Logged 'database-start.log' { docker compose up -d database }
    $composeTouched = $true
    $currentStage = 'database-health'
    Wait-ContainerHealthy 'database'
    $currentStage = 'backend-start'
    Invoke-Logged 'backend-start.log' { docker compose up -d backend }
    $currentStage = 'backend-health'
    Wait-ContainerHealthy 'backend'
    $currentStage = 'backend-http-health'
    Wait-HttpReady 'backend' 'http://127.0.0.1:8089/actuator/health' '"status"\s*:\s*"UP"'
    $currentStage = 'frontend-start'
    Invoke-Logged 'frontend-start.log' { docker compose up -d frontend }
    $currentStage = 'frontend-health'
    Wait-ContainerHealthy 'frontend'
    Wait-HttpReady 'frontend' 'http://127.0.0.1:8088/health' '^ok'

    Push-Location (Join-Path $repositoryRoot 'frontend')
    try {
        $currentStage = 'npm-install'
        Invoke-Logged 'npm-ci.log' { npm.cmd ci }
        $currentStage = 'browser-install'
        Invoke-Logged 'playwright-install.log' { npx.cmd playwright install chromium }
        $currentStage = 'playwright'
        & npx.cmd playwright test @PlaywrightArgs 2>&1 | Tee-Object -FilePath (Join-Path $logsRoot 'playwright.log')
        $scriptExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($scriptExitCode -ne 0) {
        throw "Playwright failed with exit code $scriptExitCode. See evidence and logs."
    }
} catch {
    $scriptExitCode = 1
    @("stage=$currentStage", "error=$($_.Exception.Message)") |
        Set-Content -Encoding utf8 (Join-Path $logsRoot 'failure-stage.txt')
    Write-Error $_
} finally {
    if ($composeTouched) {
        Save-ComposeDiagnostics
        if (-not $KeepServices) {
            & docker compose down --remove-orphans 2>&1 |
                Set-Content -Encoding utf8 (Join-Path $logsRoot 'compose-down.log')
        }
    }
    Pop-Location
}

exit $scriptExitCode

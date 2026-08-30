[CmdletBinding()]
param(
    [string]$ProjectName = "segroup8-secondhand-fault-drill",
    [ValidateRange(30, 180)]
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$baseCompose = Join-Path $repoRoot "microservices\secondhand-service\compose.acceptance.yml"
$overrideCompose = Join-Path $PSScriptRoot "compose.fault-drill.yml"
$seedFile = Join-Path $PSScriptRoot "fault-drill-seed.sql"
$stubPath = Join-Path $repoRoot "scripts\e2e\stubs\secondhand-order-contract-stub.mjs"
$evidenceDir = Join-Path $repoRoot "04_tests\microservices\secondhand-service\evidence\fault-drill"
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryPath = Join-Path $evidenceDir "$runId-summary.json"
$responsesPath = Join-Path $evidenceDir "$runId-api-responses.json"
$logsPath = Join-Path $evidenceDir "$runId-compose.log"
$stubLogPath = Join-Path $evidenceDir "$runId-order-stub.log"
$stubErrorPath = Join-Path $evidenceDir "$runId-order-stub-error.log"
$serviceSecret = "test-jwt-secret-must-have-at-least-thirty-two-bytes"
$originalServicePort = $env:SECONDHAND_ACCEPTANCE_PORT
$env:SECONDHAND_ACCEPTANCE_PORT = "18080"
$composeArgs = @("compose", "-p", $ProjectName, "-f", $baseCompose, "-f", $overrideCompose)
$stubProcess = $null
$resultStatus = "FAILED"

New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null
foreach ($path in @($baseCompose, $overrideCompose, $seedFile, $stubPath)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required fault drill input not found: $path"
    }
}

function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, [System.Text.UTF8Encoding]::new($false))
}

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)
    return [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-TestJwt {
    param([long]$UserId, [string]$Username, [string]$Secret)
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $header = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}'))
    $payload = [ordered]@{
        uid = $UserId
        username = $Username
        role = "USER"
        iat = $now
        exp = $now + 3600
    } | ConvertTo-Json -Compress
    $encodedPayload = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payload))
    $unsigned = "$header.$encodedPayload"
    $hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
    try {
        $signature = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsigned)))
    } finally {
        $hmac.Dispose()
    }
    return "$unsigned.$signature"
}

function Invoke-CheckedDocker {
    param([string[]]$Arguments)
    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Wait-HealthyHttp {
    param([string]$Url, [int]$Timeout = 180)
    $deadline = (Get-Date).AddSeconds($Timeout)
    do {
        try {
            $response = Invoke-RestMethod -Uri $Url -TimeoutSec 3
            if ($response.status -eq "UP") {
                return $response
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for healthy endpoint: $Url"
}

function Wait-TcpPort {
    param([string]$HostName, [int]$Port, [int]$Timeout = 30)
    $deadline = (Get-Date).AddSeconds($Timeout)
    do {
        $client = [Net.Sockets.TcpClient]::new()
        try {
            $async = $client.BeginConnect($HostName, $Port, $null, $null)
            if ($async.AsyncWaitHandle.WaitOne(500) -and $client.Connected) {
                $client.EndConnect($async)
                return
            }
        } catch {
        } finally {
            $client.Dispose()
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for TCP endpoint $HostName`:$Port"
}

function Invoke-DbQuery {
    param([string]$Sql)
    $output = & docker @composeArgs exec -T database mysql -N -B `
        -usecondhand_app -psecondhand_app secondhand_db -e $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "Database query failed: $Sql"
    }
    return @($output)
}

function Get-DbScalar {
    param([string]$Sql)
    $output = @(Invoke-DbQuery -Sql $Sql)
    return if ($output.Count -eq 0) { $null } else { [string]$output[0] }
}

function Wait-DbValue {
    param([string]$Sql, [string]$Expected, [int]$Timeout = 60)
    $deadline = (Get-Date).AddSeconds($Timeout)
    do {
        $actual = Get-DbScalar -Sql $Sql
        if ($actual -eq $Expected) {
            return $actual
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for database value '$Expected'; last value was '$actual'."
}

function Invoke-Buy {
    param([long]$ProductId, [string]$Jwt, [string]$Remark)
    return Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:18080/api/secondhand/$ProductId/buy" `
        -Headers @{ Authorization = "Bearer $Jwt" } -ContentType "application/json" `
        -Body (@{ addressId = 100; remark = $Remark } | ConvertTo-Json -Compress) -TimeoutSec 10
}

try {
    & docker @composeArgs down -v --remove-orphans *> $null
    foreach ($port in @(18080, 18085)) {
        if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
            throw "Port $port is already in use; stop the unrelated process before running the isolated drill."
        }
    }

    & mvn -B --no-transfer-progress -f (Join-Path $repoRoot "microservices\pom.xml") `
        -pl secondhand-service -am package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "secondhand-service package failed with exit code $LASTEXITCODE"
    }
    Invoke-CheckedDocker -Arguments ($composeArgs + @("up", "-d", "--build", "database", "secondhand"))
    $initialReadiness = Wait-HealthyHttp -Url "http://127.0.0.1:18080/actuator/health/readiness"

    Get-Content -LiteralPath $seedFile -Raw -Encoding UTF8 | & docker @composeArgs exec -T database `
        mysql --default-character-set=utf8mb4 -usecondhand_app -psecondhand_app secondhand_db
    if ($LASTEXITCODE -ne 0) {
        throw "Fault drill seed failed with exit code $LASTEXITCODE"
    }

    $jwt = New-TestJwt -UserId 950002 -Username "fault_buyer" -Secret $serviceSecret
    $initialResponse = Invoke-Buy -ProductId 991001 -Jwt $jwt -Remark "order dependency unavailable"
    if ($initialResponse.code -ne 0 -or $initialResponse.data.requestStatus -ne "RETRY") {
        throw "Expected first buy to return RETRY while order dependency is down."
    }
    if ((Get-DbScalar "select status from secondhand_product where id=991001") -ne "4") {
        throw "Product 991001 was not frozen in TRADE_PENDING after the uncertain order request."
    }

    $stubProcess = Start-Process -FilePath "node" -ArgumentList @($stubPath) -WorkingDirectory $repoRoot `
        -WindowStyle Hidden -RedirectStandardOutput $stubLogPath -RedirectStandardError $stubErrorPath -PassThru
    Wait-TcpPort -HostName "127.0.0.1" -Port 18085
    Wait-DbValue -Sql "select request_status from trade_order_request where product_id=991001" `
        -Expected "CREATED" -Timeout $TimeoutSeconds | Out-Null

    $createdRequest = (Invoke-DbQuery -Sql "select order_business_key,order_id,order_no,attempts from trade_order_request where product_id=991001") -join "`t"
    $createdParts = $createdRequest -split "`t"
    $repeatedResponse = Invoke-Buy -ProductId 991001 -Jwt $jwt -Remark "repeat idempotency check"
    $requestCount = [int](Get-DbScalar "select count(*) from trade_order_request where product_id=991001")
    $createdProductStatus = [int](Get-DbScalar "select status from secondhand_product where id=991001")
    if ($repeatedResponse.data.orderBusinessKey -ne $createdParts[0] -or
        [string]$repeatedResponse.data.orderId -ne $createdParts[1] -or
        $requestCount -ne 1 -or $createdProductStatus -ne 3) {
        throw "Recovered purchase was not idempotent or did not finish in SOLD state."
    }

    Stop-Process -Id $stubProcess.Id -Force
    Wait-Process -Id $stubProcess.Id -ErrorAction SilentlyContinue
    $stubProcess = $null
    Start-Sleep -Seconds 1

    $failedInitialResponse = Invoke-Buy -ProductId 991002 -Jwt $jwt -Remark "retry exhaustion"
    if ($failedInitialResponse.code -ne 0 -or $failedInitialResponse.data.requestStatus -ne "RETRY") {
        throw "Expected second product to enter RETRY while order dependency remains down."
    }
    Wait-DbValue -Sql "select request_status from trade_order_request where product_id=991002" `
        -Expected "FAILED" -Timeout $TimeoutSeconds | Out-Null
    $failedProductStatus = [int](Get-DbScalar "select status from secondhand_product where id=991002")
    $failedOutboxCount = [int](Get-DbScalar "select count(*) from outbox_event where event_type='SecondhandTradeOrderFailed.v1' and payload like '%991002%'")
    $failedRequestCount = [int](Get-DbScalar "select count(*) from trade_order_request where product_id=991002")
    $failedAttempts = [int](Get-DbScalar "select attempts from trade_order_request where product_id=991002")
    if ($failedProductStatus -ne 1 -or $failedOutboxCount -ne 1 -or $failedRequestCount -ne 1) {
        throw "Retry exhaustion did not release the product exactly once."
    }

    $finalReadiness = Wait-HealthyHttp -Url "http://127.0.0.1:18080/actuator/health/readiness"
    $responses = [ordered]@{
        unavailable = $initialResponse
        recoveredAndRepeated = $repeatedResponse
        retryExhaustion = $failedInitialResponse
    }
    Write-Utf8NoBom -Path $responsesPath -Content ($responses | ConvertTo-Json -Depth 12)
    $summary = [ordered]@{
        executedAt = (Get-Date).ToString("o")
        status = "PASSED"
        environment = "Docker Compose / real secondhand-service / MySQL 8.4.6 / process-level order stub"
        dependencyEndpoint = "http://host.docker.internal:18085"
        readiness = @{ before = $initialReadiness.status; whileDependencyDown = $finalReadiness.status }
        recovery = @{
            firstStatus = $initialResponse.data.requestStatus
            finalStatus = "CREATED"
            orderBusinessKey = $createdParts[0]
            orderId = [long]$createdParts[1]
            orderNo = $createdParts[2]
            storedAttemptsBeforeRecovery = [int]$createdParts[3]
            repeatedRequestCount = $requestCount
            finalProductStatus = "SOLD"
        }
        exhaustion = @{
            firstStatus = $failedInitialResponse.data.requestStatus
            finalStatus = "FAILED"
            storedRetryAttempts = $failedAttempts
            requestCount = $failedRequestCount
            failedOutboxCount = $failedOutboxCount
            finalProductStatus = "ON_SHELF"
        }
        evidence = @{
            apiResponses = "$(Split-Path $responsesPath -Leaf)"
            composeLog = "$(Split-Path $logsPath -Leaf)"
            orderStubLog = "$(Split-Path $stubLogPath -Leaf)"
        }
    }
    Write-Utf8NoBom -Path $summaryPath -Content ($summary | ConvertTo-Json -Depth 12)
    $resultStatus = "PASSED"
    Write-Host "Order dependency fault drill passed. Evidence: $summaryPath"
} finally {
    if ($stubProcess -and -not $stubProcess.HasExited) {
        Stop-Process -Id $stubProcess.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $stubProcess.Id -ErrorAction SilentlyContinue
    }
    & docker @composeArgs logs --no-color --timestamps 2>&1 | Out-File -LiteralPath $logsPath -Encoding utf8
    & docker @composeArgs down -v --remove-orphans *> $null
    if ($null -eq $originalServicePort) {
        Remove-Item Env:SECONDHAND_ACCEPTANCE_PORT -ErrorAction SilentlyContinue
    } else {
        $env:SECONDHAND_ACCEPTANCE_PORT = $originalServicePort
    }
    if ($resultStatus -ne "PASSED") {
        Write-Warning "Fault drill did not pass. Diagnostics: $logsPath"
    }
}

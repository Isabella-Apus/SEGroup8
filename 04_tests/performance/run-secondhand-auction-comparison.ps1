[CmdletBinding()]
param(
    [ValidateRange(3, 10)]
    [int]$Rounds = 3,
    [ValidateRange(1, 10)]
    [int]$VUs = 10,
    [string]$Duration = "30s",
    [string]$MonolithWorktree = (Join-Path $env:USERPROFILE "Desktop\SEGroup8-monolith-start"),
    [string]$MonolithProject = "segroup8-monolith-baseline",
    [string]$ServiceProject = "segroup8-secondhand-performance"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$monolithCompose = Join-Path $MonolithWorktree "compose.yml"
$monolithOverride = Join-Path $PSScriptRoot "monolith-start.compose.override.yml"
$serviceCompose = Join-Path $repoRoot "microservices\secondhand-service\compose.acceptance.yml"
$runK6 = Join-Path $PSScriptRoot "run-k6.ps1"
$resetMonolith = Join-Path $PSScriptRoot "reset-performance-data.ps1"
$resetService = Join-Path $PSScriptRoot "reset-secondhand-service-performance-data.ps1"
$resultDir = Join-Path $PSScriptRoot "results"
$experimentId = "$(Get-Date -Format 'yyyyMMdd-HHmmss')-formal"
$aggregateJson = Join-Path $resultDir "$experimentId-secondhand-auction-comparison.json"
$aggregateCsv = Join-Path $resultDir "$experimentId-secondhand-auction-comparison.csv"
$reportPath = Join-Path $PSScriptRoot "secondhand-auction-formal-comparison.md"
$monolithSecret = "SEGROUP8_DOCKER_DEMO_SECRET_CHANGE_ME_2026"
$serviceSecret = "test-jwt-secret-must-have-at-least-thirty-two-bytes"
$results = [System.Collections.Generic.List[object]]::new()

foreach ($path in @($monolithCompose, $monolithOverride, $serviceCompose, $runK6, $resetMonolith, $resetService)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required comparison input not found: $path"
    }
}
if ((& git -C $MonolithWorktree describe --tags --exact-match 2>$null) -ne "monolith-start") {
    throw "Monolith worktree must be checked out at the immutable monolith-start tag: $MonolithWorktree"
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
    $payloadObject = [ordered]@{
        uid = $UserId
        username = $Username
        role = "USER"
        iat = $now
        exp = $now + 7200
    }
    $payload = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes(($payloadObject | ConvertTo-Json -Compress)))
    $unsigned = "$header.$payload"
    $hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
    try {
        $signature = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsigned)))
    } finally {
        $hmac.Dispose()
    }
    return "$unsigned.$signature"
}

function Wait-HealthyHttp {
    param([string]$Url, [int]$TimeoutSeconds = 180)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-RestMethod -Uri $Url -TimeoutSec 3
            if ($response.status -eq "UP") {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for healthy endpoint: $Url"
}

function Invoke-CheckedDocker {
    param([string[]]$Arguments)
    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Invoke-DockerBestEffort {
    param([string[]]$Arguments)
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker @Arguments *> $null
    $ErrorActionPreference = $previousErrorActionPreference
}

function Read-K6Result {
    param([string]$Target, [int]$Run, [string]$Prefix)
    $summaryName = "$Prefix-summary.json"
    $metadataName = "$Prefix-metadata.json"
    $summary = Get-Content -LiteralPath (Join-Path $resultDir $summaryName) -Raw -Encoding UTF8 | ConvertFrom-Json
    $metadata = Get-Content -LiteralPath (Join-Path $resultDir $metadataName) -Raw -Encoding UTF8 | ConvertFrom-Json
    return [pscustomobject][ordered]@{
        experimentId = $experimentId
        target = $Target
        run = $Run
        vus = $VUs
        duration = $Duration
        requests = [int]$summary.metrics.http_reqs.count
        requestsPerSecond = [double]$summary.metrics.http_reqs.rate
        averageDurationMs = [double]$summary.metrics.http_req_duration.avg
        p95DurationMs = [double]$summary.metrics.http_req_duration.'p(95)'
        maximumDurationMs = [double]$summary.metrics.http_req_duration.max
        httpFailureRate = [double]$summary.metrics.http_req_failed.value
        businessSuccessRate = [double]$summary.metrics.business_success.value
        businessGuardedRate = [double]$summary.metrics.business_guarded.value
        checkSuccessRate = [double]$summary.metrics.checks.value
        summary = "results/$summaryName"
        rawOutput = "results/$($metadata.rawOutput)"
        consoleLog = "results/$($metadata.consoleLog)"
        metadata = "results/$metadataName"
    }
}

function Remove-WarmupEvidence {
    param([string]$Prefix)
    foreach ($path in Get-ChildItem -LiteralPath $resultDir -File | Where-Object { $_.Name.StartsWith("$Prefix-") }) {
        Remove-Item -LiteralPath $path.FullName
    }
}

function Invoke-TargetRuns {
    param(
        [string]$Target,
        [string]$TargetVersion,
        [string]$BaseUrl,
        [scriptblock]$ResetData,
        [hashtable]$Environment
    )
    & $ResetData
    $warmupPrefix = "$experimentId-warmup-$Target-secondhand-auction-bid"
    & $runK6 -Scenario secondhand-auction-bid -BaseUrl $BaseUrl -VUs $VUs -Duration "5s" `
        -Environment $Environment -TargetVersion $TargetVersion -ResultPrefix $warmupPrefix -CompressRaw
    Remove-WarmupEvidence -Prefix $warmupPrefix

    for ($run = 1; $run -le $Rounds; $run++) {
        & $ResetData
        $prefix = "$experimentId-$Target-secondhand-auction-bid-run$run"
        & $runK6 -Scenario secondhand-auction-bid -BaseUrl $BaseUrl -VUs $VUs -Duration $Duration `
            -Environment $Environment -TargetVersion $TargetVersion -ResultPrefix $prefix -CompressRaw
        $results.Add((Read-K6Result -Target $Target -Run $run -Prefix $prefix))
    }
}

function Restore-EnvironmentValue {
    param([string]$Name, [object]$OriginalValue)
    if ($null -eq $OriginalValue) {
        Remove-Item -Path "Env:$Name" -ErrorAction SilentlyContinue
    } else {
        Set-Item -Path "Env:$Name" -Value $OriginalValue
    }
}

$monolithComposeArgs = @("compose", "-p", $MonolithProject, "-f", $monolithCompose, "-f", $monolithOverride)
$serviceComposeArgs = @("compose", "-p", $ServiceProject, "-f", $serviceCompose)
$regularComposeArgs = @("compose", "-p", "segroup8-platform", "-f", (Join-Path $repoRoot "compose.yml"))
$regularWasRunning = @(& docker @regularComposeArgs ps --services --filter status=running 2>$null)
$monolithWasRunning = @(& docker @monolithComposeArgs ps --services --filter status=running 2>$null)
$originalFrontendPort = $env:FRONTEND_HOST_PORT
$originalBackendPort = $env:BACKEND_HOST_PORT
$originalMysqlPort = $env:MYSQL_HOST_PORT
$originalServicePort = $env:SECONDHAND_ACCEPTANCE_PORT

$env:FRONTEND_HOST_PORT = "18088"
$env:BACKEND_HOST_PORT = "18089"
$env:MYSQL_HOST_PORT = "13307"
$env:SECONDHAND_ACCEPTANCE_PORT = "18080"

try {
    if ($regularWasRunning.Count -gt 0) {
        Invoke-CheckedDocker -Arguments ($regularComposeArgs + @("stop"))
    }

    Invoke-CheckedDocker -Arguments ($monolithComposeArgs + @("up", "-d", "database", "backend"))
    Invoke-CheckedDocker -Arguments ($monolithComposeArgs + @("stop", "frontend"))
    Wait-HealthyHttp -Url "http://127.0.0.1:18089/actuator/health"
    $monolithTokens = for ($userId = 950002; $userId -le 950021; $userId++) {
        New-TestJwt -UserId $userId -Username "perf_bidder_$userId" -Secret $monolithSecret
    }
    $monolithEnvironment = @{
        BUYER_TOKENS = $monolithTokens -join ";"
        AUCTION_FIRST_ID = "999001"
        AUCTION_COUNT = "10"
        BID_BASE_AMOUNT = "1000"
        BID_STEP = "5"
        SLEEP = "0.2"
    }
    $resetMonolithData = {
        & $resetMonolith -ComposeFile $monolithCompose -ComposeOverrideFile $monolithOverride `
            -ProjectName $MonolithProject
    }
    Invoke-TargetRuns -Target "monolith-start" -TargetVersion "monolith-start" `
        -BaseUrl "http://host.docker.internal:18089/api" -ResetData $resetMonolithData `
        -Environment $monolithEnvironment

    Invoke-CheckedDocker -Arguments ($monolithComposeArgs + @("stop"))

    & mvn -B --no-transfer-progress -f (Join-Path $repoRoot "microservices\pom.xml") `
        -pl secondhand-service -am package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "secondhand-service package failed with exit code $LASTEXITCODE"
    }
    Invoke-CheckedDocker -Arguments ($serviceComposeArgs + @("up", "-d", "--build", "database", "secondhand"))
    Wait-HealthyHttp -Url "http://127.0.0.1:18080/actuator/health/readiness"
    $serviceTokens = for ($userId = 950002; $userId -le 950021; $userId++) {
        New-TestJwt -UserId $userId -Username "perf_bidder_$userId" -Secret $serviceSecret
    }
    $serviceEnvironment = @{
        BUYER_TOKENS = $serviceTokens -join ";"
        AUCTION_FIRST_ID = "999001"
        AUCTION_COUNT = "10"
        BID_BASE_AMOUNT = "1000"
        BID_STEP = "5"
        SLEEP = "0.2"
    }
    $resetServiceData = {
        & $resetService -ComposeFile $serviceCompose -ProjectName $ServiceProject
    }
    $branch = (& git -C $repoRoot branch --show-current).Trim()
    Invoke-TargetRuns -Target "secondhand-service" -TargetVersion $branch `
        -BaseUrl "http://host.docker.internal:18080/api" -ResetData $resetServiceData `
        -Environment $serviceEnvironment

    $orderedResults = @($results | Sort-Object target, run)
    Write-Utf8NoBom -Path $aggregateJson -Content ($orderedResults | ConvertTo-Json -Depth 8)
    Write-Utf8NoBom -Path $aggregateCsv -Content (($orderedResults | ConvertTo-Csv -NoTypeInformation) -join "`n")

    $averages = foreach ($target in @("monolith-start", "secondhand-service")) {
        $targetRows = @($orderedResults | Where-Object target -eq $target)
        [pscustomobject]@{
            target = $target
            requestsPerSecond = ($targetRows | Measure-Object requestsPerSecond -Average).Average
            averageDurationMs = ($targetRows | Measure-Object averageDurationMs -Average).Average
            p95DurationMs = ($targetRows | Measure-Object p95DurationMs -Average).Average
            httpFailureRate = ($targetRows | Measure-Object httpFailureRate -Average).Average
            businessSuccessRate = ($targetRows | Measure-Object businessSuccessRate -Average).Average
        }
    }
    $monolithCommit = (& git -C $MonolithWorktree rev-parse HEAD).Trim()
    $serviceCommit = (& git -C $repoRoot rev-parse HEAD).Trim()
    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add("# 二手拍卖出价三轮正式性能对比")
    $lines.Add("")
    $lines.Add("执行时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')")
    $lines.Add("")
    $lines.Add("## 公平性约束")
    $lines.Add("")
    $lines.Add("- 同一台 Docker Desktop 主机，两个版本按顺序隔离运行，日常 Compose 栈在实验期间停止并在结束后恢复。")
    $lines.Add("- 相同 k6 脚本、$VUs VU、$Duration、10 个独立拍卖；每个 VU 使用一对独立买家交替出价，避免跨拍卖共享余额行造成额外锁竞争。")
    $lines.Add("- 单体基线：``monolith-start`` / ``$monolithCommit``；微服务版本：``$serviceCommit``。")
    $lines.Add("- 每个版本先进行 5 秒预热，预热结果不计入正式三轮。JWT 仅在内存中生成，证据已脱敏。")
    $lines.Add("")
    $lines.Add("## 三轮原始结果")
    $lines.Add("")
    $lines.Add("| 版本 | 轮次 | 请求数 | RPS | Avg (ms) | P95 (ms) | Max (ms) | HTTP 失败 | 业务成功 |")
    $lines.Add("|---|---:|---:|---:|---:|---:|---:|---:|---:|")
    foreach ($row in $orderedResults) {
        $lines.Add(("| {0} | {1} | {2} | {3:N2} | {4:N2} | {5:N2} | {6:N2} | {7:P2} | {8:P2} |" -f `
            $row.target, $row.run, $row.requests, $row.requestsPerSecond, $row.averageDurationMs,
            $row.p95DurationMs, $row.maximumDurationMs, $row.httpFailureRate, $row.businessSuccessRate))
    }
    $lines.Add("")
    $lines.Add("## 三轮均值")
    $lines.Add("")
    $lines.Add("| 版本 | 平均 RPS | 平均响应 (ms) | 平均 P95 (ms) | HTTP 失败 | 业务成功 |")
    $lines.Add("|---|---:|---:|---:|---:|---:|")
    foreach ($row in $averages) {
        $lines.Add(("| {0} | {1:N2} | {2:N2} | {3:N2} | {4:P2} | {5:P2} |" -f `
            $row.target, $row.requestsPerSecond, $row.averageDurationMs, $row.p95DurationMs,
            $row.httpFailureRate, $row.businessSuccessRate))
    }
    $lines.Add("")
    $lines.Add("## 结论边界")
    $lines.Add("")
    $lines.Add("本实验只比较二手拍卖出价这一条接口在本机固定条件下的表现。结果可用于说明当前实现的吞吐、延迟和错误率，不能外推为整个平台或所有微服务的总体性能结论，也不会在数据不支持时宣称性能提升。")
    $lines.Add("")
    $lines.Add("聚合证据：``results/$(Split-Path $aggregateJson -Leaf)``、``results/$(Split-Path $aggregateCsv -Leaf)``。每轮的 summary、console、metadata 和压缩 raw JSON 均位于 ``04_tests/performance/results/``。")
    Write-Utf8NoBom -Path $reportPath -Content ($lines -join "`n")

    Write-Host "Formal comparison passed. Report: $reportPath"
} finally {
    Invoke-DockerBestEffort -Arguments ($serviceComposeArgs + @("down", "-v", "--remove-orphans"))
    Invoke-DockerBestEffort -Arguments ($monolithComposeArgs + @("stop"))
    if ($monolithWasRunning.Count -gt 0) {
        Invoke-DockerBestEffort -Arguments ($monolithComposeArgs + @("up", "-d") + $monolithWasRunning)
    }
    if ($regularWasRunning.Count -gt 0) {
        Invoke-DockerBestEffort -Arguments ($regularComposeArgs + @("start") + $regularWasRunning)
    }
    Restore-EnvironmentValue -Name "FRONTEND_HOST_PORT" -OriginalValue $originalFrontendPort
    Restore-EnvironmentValue -Name "BACKEND_HOST_PORT" -OriginalValue $originalBackendPort
    Restore-EnvironmentValue -Name "MYSQL_HOST_PORT" -OriginalValue $originalMysqlPort
    Restore-EnvironmentValue -Name "SECONDHAND_ACCEPTANCE_PORT" -OriginalValue $originalServicePort
}

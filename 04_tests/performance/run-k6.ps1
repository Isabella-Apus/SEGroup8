param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("product-search", "new-order-create", "secondhand-buy", "secondhand-auction-bid")]
    [string]$Scenario,
    [string]$BaseUrl = "http://host.docker.internal:18089/api",
    [int]$VUs = 2,
    [string]$Duration = "10s",
    [hashtable]$Environment = @{},
    [string]$TargetVersion = "monolith-start",
    [string]$K6Image = "grafana/k6:latest",
    [string]$ResultPrefix = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$resultDir = Join-Path $PSScriptRoot "results"
New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

$scriptName = "$Scenario.k6.js"
$scriptPath = Join-Path $PSScriptRoot "k6\$scriptName"
if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "k6 script not found: $scriptPath"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$prefix = if ($ResultPrefix) { $ResultPrefix } else { "$timestamp-$TargetVersion-$Scenario-run1" }
$summaryName = "$prefix-summary.json"
$rawName = "$prefix-raw.json"
$logName = "$prefix-console.log"
$metadataName = "$prefix-metadata.json"

$allEnvironment = [ordered]@{
    BASE_URL = $BaseUrl
    VUS = $VUs
    DURATION = $Duration
}
foreach ($key in $Environment.Keys) {
    $allEnvironment[$key] = $Environment[$key]
}

$dockerArgs = @(
    "run", "--rm",
    "--add-host", "host.docker.internal:host-gateway",
    "--mount", "type=bind,source=$repoRoot,target=/work",
    "-w", "/work",
    $K6Image,
    "run",
    "--quiet",
    "--summary-export", "/work/04_tests/performance/results/$summaryName",
    "--out", "json=/work/04_tests/performance/results/$rawName",
    "--tag", "target=$TargetVersion",
    "--tag", "scenario=$Scenario"
)
foreach ($entry in $allEnvironment.GetEnumerator()) {
    $dockerArgs += @("--env", "$($entry.Key)=$($entry.Value)")
}
$dockerArgs += "/work/04_tests/performance/k6/$scriptName"

$logPath = Join-Path $resultDir $logName
$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& docker image inspect $K6Image *> $null
$inspectExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference
if ($inspectExitCode -ne 0) {
    Write-Host "Pulling k6 image '$K6Image'..."
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker pull $K6Image
    $pullExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($pullExitCode -ne 0) {
        throw "Unable to pull k6 image '$K6Image' (exit code $pullExitCode)"
    }
}

Write-Host "Running k6 scenario '$Scenario' against $BaseUrl..."
$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& docker @dockerArgs 2>&1 | Tee-Object -FilePath $logPath
$exitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference

$imageId = (& docker image inspect $K6Image --format "{{.Id}}" 2>$null)
$k6Version = (& docker run --rm $K6Image version 2>$null | Select-Object -First 1)
$safeEnvironment = [ordered]@{}
foreach ($entry in $allEnvironment.GetEnumerator()) {
    $safeEnvironment[$entry.Key] = if ($entry.Key -match "PASSWORD|TOKEN|SECRET") { "<redacted>" } else { $entry.Value }
}
$metadata = [ordered]@{
    executedAt = (Get-Date).ToString("o")
    scenario = $Scenario
    targetVersion = $TargetVersion
    targetCommit = (& git -C $repoRoot rev-list -n 1 $TargetVersion 2>$null)
    baseUrl = $BaseUrl
    vus = $VUs
    duration = $Duration
    k6Image = $K6Image
    k6ImageId = $imageId
    k6Version = $k6Version
    environment = $safeEnvironment
    exitCode = $exitCode
    summary = $summaryName
    rawOutput = $rawName
    consoleLog = $logName
}
$metadata | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $resultDir $metadataName) -Encoding UTF8

if ($exitCode -ne 0) {
    throw "k6 exited with code $exitCode. See $logPath"
}
Write-Host "Saved k6 evidence under $resultDir"

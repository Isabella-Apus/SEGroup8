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
    [string]$ResultPrefix = "",
    [switch]$CompressRaw
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

function Protect-SensitiveValue {
    param(
        [object]$Value,
        [string]$KeyName = ""
    )
    if ($KeyName -match "(?i)password|token|secret") {
        return "<redacted>"
    }
    if ($null -eq $Value -or $Value -is [string] -or $Value -is [ValueType]) {
        return $Value
    }
    if ($Value -is [System.Collections.IDictionary]) {
        $safeMap = [ordered]@{}
        foreach ($key in $Value.Keys) {
            $safeMap[$key] = Protect-SensitiveValue -Value $Value[$key] -KeyName ([string]$key)
        }
        return $safeMap
    }
    if ($Value -is [System.Collections.IEnumerable]) {
        return @($Value | ForEach-Object { Protect-SensitiveValue -Value $_ })
    }
    $safeObject = [ordered]@{}
    foreach ($property in $Value.PSObject.Properties) {
        $safeObject[$property.Name] = Protect-SensitiveValue -Value $property.Value -KeyName $property.Name
    }
    return $safeObject
}

function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, [System.Text.UTF8Encoding]::new($false))
}

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

$summaryPath = Join-Path $resultDir $summaryName
if (Test-Path -LiteralPath $summaryPath) {
    $summary = Get-Content -LiteralPath $summaryPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $safeSummary = Protect-SensitiveValue -Value $summary
    Write-Utf8NoBom -Path $summaryPath -Content ($safeSummary | ConvertTo-Json -Depth 30)
}

$rawOutputName = $rawName
$rawPath = Join-Path $resultDir $rawName
if ($CompressRaw -and (Test-Path -LiteralPath $rawPath)) {
    $compressedName = "$rawName.gz"
    $compressedPath = Join-Path $resultDir $compressedName
    $inputStream = [System.IO.File]::OpenRead($rawPath)
    try {
        $outputStream = [System.IO.File]::Create($compressedPath)
        try {
            $gzip = [System.IO.Compression.GZipStream]::new(
                $outputStream,
                [System.IO.Compression.CompressionLevel]::Optimal,
                $true
            )
            try {
                $inputStream.CopyTo($gzip)
            } finally {
                $gzip.Dispose()
            }
        } finally {
            $outputStream.Dispose()
        }
    } finally {
        $inputStream.Dispose()
    }
    Remove-Item -LiteralPath $rawPath
    $rawOutputName = $compressedName
}

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
    rawOutput = $rawOutputName
    consoleLog = $logName
}
Write-Utf8NoBom -Path (Join-Path $resultDir $metadataName) -Content ($metadata | ConvertTo-Json -Depth 6)

if ($exitCode -ne 0) {
    throw "k6 exited with code $exitCode. See $logPath"
}
Write-Host "Saved k6 evidence under $resultDir"

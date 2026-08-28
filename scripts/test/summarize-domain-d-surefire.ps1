param(
    [Parameter(Mandatory = $true)]
    [string]$ReportsDirectory,

    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory
)

$ErrorActionPreference = "Stop"

$expectedSuites = @(
    @{ Class = "com.segroup8.platform.service.impl.SecondhandProductServiceImplTest"; Layer = "service-unit"; UseCases = @("UC16", "UC17") },
    @{ Class = "com.segroup8.platform.service.impl.SecondhandTradeServiceImplTest"; Layer = "service-unit"; UseCases = @("UC18", "UC19") },
    @{ Class = "com.segroup8.platform.integration.SecondhandOrderFlowIntegrationTest"; Layer = "spring-h2-integration"; UseCases = @("UC20") },
    @{ Class = "com.segroup8.platform.integration.SecondhandAuctionIntegrationTest"; Layer = "spring-h2-integration"; UseCases = @("UC19") },
    @{ Class = "com.segroup8.platform.controller.SecondhandProductControllerUc16WebMvcTest"; Layer = "mockmvc-contract"; UseCases = @("UC16") },
    @{ Class = "com.segroup8.platform.controller.SecondhandProductControllerUc17WebMvcTest"; Layer = "mockmvc-contract"; UseCases = @("UC17") },
    @{ Class = "com.segroup8.platform.controller.SecondhandTradeControllerUc18WebMvcTest"; Layer = "mockmvc-contract"; UseCases = @("UC18") },
    @{ Class = "com.segroup8.platform.controller.SecondhandTradeControllerUc19WebMvcTest"; Layer = "mockmvc-contract"; UseCases = @("UC19") },
    @{ Class = "com.segroup8.platform.controller.OrderControllerUc20WebMvcTest"; Layer = "mockmvc-contract"; UseCases = @("UC20") }
)

$reportsPath = [System.IO.Path]::GetFullPath($ReportsDirectory)
$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

$suiteResults = @()
foreach ($expected in $expectedSuites) {
    $reportPath = Join-Path $reportsPath ("TEST-{0}.xml" -f $expected.Class)
    if (-not (Test-Path -LiteralPath $reportPath)) {
        $suiteResults += [pscustomobject][ordered]@{
            class = $expected.Class
            layer = $expected.Layer
            useCases = $expected.UseCases
            tests = 0
            passed = 0
            failed = 0
            errors = 0
            skipped = 0
            status = "MISSING"
            report = Split-Path -Leaf $reportPath
        }
        continue
    }

    [xml]$report = Get-Content -Raw -LiteralPath $reportPath
    $suite = $report.testsuite
    $tests = [int]$suite.tests
    $failed = [int]$suite.failures
    $errors = [int]$suite.errors
    $skipped = [int]$suite.skipped
    $passed = $tests - $failed - $errors - $skipped
    $status = if (($failed + $errors) -eq 0) { "API_PASS" } else { "API_FAIL" }

    $suiteResults += [pscustomobject][ordered]@{
        class = $expected.Class
        layer = $expected.Layer
        useCases = $expected.UseCases
        tests = $tests
        passed = $passed
        failed = $failed
        errors = $errors
        skipped = $skipped
        status = $status
        report = Split-Path -Leaf $reportPath
    }
}

$totals = [ordered]@{
    expectedClasses = $expectedSuites.Count
    reportedClasses = @($suiteResults | Where-Object { $_.status -ne "MISSING" }).Count
    tests = ($suiteResults | Measure-Object -Property tests -Sum).Sum
    passed = ($suiteResults | Measure-Object -Property passed -Sum).Sum
    failed = ($suiteResults | Measure-Object -Property failed -Sum).Sum
    errors = ($suiteResults | Measure-Object -Property errors -Sum).Sum
    skipped = ($suiteResults | Measure-Object -Property skipped -Sum).Sum
}

$apiStatus = if (
    $totals.reportedClasses -eq $totals.expectedClasses -and
    ($totals.failed + $totals.errors) -eq 0
) { "API_PASS" } else { "API_FAIL" }

$summary = [ordered]@{
    schemaVersion = 1
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    scope = "DOMAIN_D"
    classifications = [ordered]@{
        api = $apiStatus
        uiWalkthrough = "UI_WALKTHROUGH_PASS"
        e2e = "E2E_PENDING"
    }
    totals = $totals
    suites = $suiteResults
}

$jsonPath = Join-Path $outputPath "domain-d-test-summary.json"
$markdownPath = Join-Path $outputPath "domain-d-test-summary.md"
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
$json = $summary | ConvertTo-Json -Depth 8
[System.IO.File]::WriteAllText($jsonPath, $json, $utf8WithoutBom)

$markdown = @(
    "# Domain-D Surefire summary",
    "",
    "- API: **$apiStatus**",
    "- UI walkthrough: **UI_WALKTHROUGH_PASS** (Vite mock evidence only)",
    "- Real Compose Playwright E2E: **E2E_PENDING**",
    "- Classes: $($totals.reportedClasses)/$($totals.expectedClasses)",
    "- Tests: $($totals.tests); Passed: $($totals.passed); Failed: $($totals.failed); Errors: $($totals.errors); Skipped: $($totals.skipped)",
    "",
    "| Test class | Layer | UC | Tests | Passed | Failed | Errors | Skipped | Status |",
    "|---|---|---|---:|---:|---:|---:|---:|---|"
)

foreach ($suite in $suiteResults) {
    $markdown += "| ``$($suite.class)`` | $($suite.layer) | $($suite.useCases -join ', ') | $($suite.tests) | $($suite.passed) | $($suite.failed) | $($suite.errors) | $($suite.skipped) | $($suite.status) |"
}

$markdownText = $markdown -join [Environment]::NewLine
[System.IO.File]::WriteAllText($markdownPath, $markdownText, $utf8WithoutBom)

if ($env:GITHUB_STEP_SUMMARY) {
    [System.IO.File]::AppendAllText(
        $env:GITHUB_STEP_SUMMARY,
        $markdownText + [Environment]::NewLine,
        $utf8WithoutBom
    )
}

Write-Host "Domain-D summary: $($totals.reportedClasses)/$($totals.expectedClasses) classes, $($totals.tests) tests, $($totals.passed) passed, $($totals.failed) failed, $($totals.errors) errors, $($totals.skipped) skipped."
Write-Host "Classification: API=$apiStatus, UI=UI_WALKTHROUGH_PASS, E2E=E2E_PENDING"

if ($apiStatus -ne "API_PASS") {
    exit 1
}

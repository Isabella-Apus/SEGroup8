param(
    [Parameter(Mandatory = $true)]
    [string]$DomainEvidenceRoot,

    [Parameter(Mandatory = $true)]
    [string]$PlaywrightEvidenceRoot,

    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory,

    [Parameter(Mandatory = $true)]
    [string]$E2EOutcome
)

$ErrorActionPreference = "Stop"

$domainEvidencePath = [System.IO.Path]::GetFullPath($DomainEvidenceRoot)
$playwrightEvidencePath = [System.IO.Path]::GetFullPath($PlaywrightEvidenceRoot)
$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

$domains = @()
foreach ($domain in @("A", "B", "C", "D", "E")) {
    $fileName = "domain-$($domain.ToLowerInvariant())-test-summary.json"
    $matches = @()
    if (Test-Path -LiteralPath $domainEvidencePath) {
        $matches = @(
            Get-ChildItem -LiteralPath $domainEvidencePath -Recurse -File -Filter $fileName |
                Sort-Object FullName
        )
    }

    if ($matches.Count -eq 1) {
        $domainSummary = Get-Content -Raw -LiteralPath $matches[0].FullName | ConvertFrom-Json
        $domains += [pscustomobject][ordered]@{
            scope = $domainSummary.scope
            result = $domainSummary.result
            suites = $domainSummary.totals.suites
            tests = $domainSummary.totals.tests
            passed = $domainSummary.totals.passed
            failures = $domainSummary.totals.failures
            errors = $domainSummary.totals.errors
            skipped = $domainSummary.totals.skipped
            durationSeconds = $domainSummary.totals.durationSeconds
        }
    } else {
        $domains += [pscustomobject][ordered]@{
            scope = "DOMAIN_$domain"
            result = "MISSING"
            suites = 0
            tests = 0
            passed = 0
            failures = 0
            errors = 0
            skipped = 0
            durationSeconds = 0
        }
    }
}

$playwright = [ordered]@{
    result = "FAIL"
    outcome = $E2EOutcome
    reports = 0
    validReports = 0
    tests = 0
    expected = 0
    unexpected = 0
    flaky = 0
    skipped = 0
    durationSeconds = 0
    scopes = @("smoke", "domain-a", "domain-b", "domain-c", "domain-d", "domain-e")
}
$playwrightResultFiles = @()
if (Test-Path -LiteralPath $playwrightEvidencePath) {
    $playwrightResultFiles = @(
        Get-ChildItem -LiteralPath $playwrightEvidencePath -Recurse -File -Filter "playwright-results.json" |
            Sort-Object FullName
    )
}
$playwright.reports = $playwrightResultFiles.Count
foreach ($playwrightResultFile in $playwrightResultFiles) {
    $playwrightResultsJson = Get-Content -Raw -LiteralPath $playwrightResultFile.FullName | ConvertFrom-Json
    if ($playwrightResultsJson.stats) {
        $playwright.validReports += 1
        $playwright.expected += [int]$playwrightResultsJson.stats.expected
        $playwright.unexpected += [int]$playwrightResultsJson.stats.unexpected
        $playwright.flaky += [int]$playwrightResultsJson.stats.flaky
        $playwright.skipped += [int]$playwrightResultsJson.stats.skipped
        $playwright.tests = $playwright.expected + $playwright.unexpected + $playwright.flaky + $playwright.skipped
        $playwright.durationSeconds += ([double]$playwrightResultsJson.stats.duration / 1000)
    }
}
$playwright.durationSeconds = [Math]::Round($playwright.durationSeconds, 3)
if (
    $E2EOutcome -eq "success" -and
    $playwright.reports -eq 2 -and
    $playwright.validReports -eq 2 -and
    $playwright.expected -gt 0 -and
    $playwright.unexpected -eq 0 -and
    (Test-Path -LiteralPath $playwrightEvidencePath)
) {
    $playwright.result = "PASS"
}

$domainTotals = [ordered]@{
    suites = ($domains | Measure-Object -Property suites -Sum).Sum
    tests = ($domains | Measure-Object -Property tests -Sum).Sum
    passed = ($domains | Measure-Object -Property passed -Sum).Sum
    failures = ($domains | Measure-Object -Property failures -Sum).Sum
    errors = ($domains | Measure-Object -Property errors -Sum).Sum
    skipped = ($domains | Measure-Object -Property skipped -Sum).Sum
    durationSeconds = [Math]::Round(($domains | Measure-Object -Property durationSeconds -Sum).Sum, 3)
}
$allDomainsPassed = @($domains | Where-Object { $_.result -ne "PASS" }).Count -eq 0
$result = if ($allDomainsPassed -and $playwright.result -eq "PASS") { "PASS" } else { "FAIL" }

$summary = [ordered]@{
    schemaVersion = 1
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    result = $result
    domainTotals = $domainTotals
    domains = $domains
    e2e = $playwright
}

$jsonPath = Join-Path $outputPath "final-test-summary.json"
$markdownPath = Join-Path $outputPath "final-test-summary.md"
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    $jsonPath,
    ($summary | ConvertTo-Json -Depth 8),
    $utf8WithoutBom
)

$markdown = @(
    "# Kinda Goods final test summary",
    "",
    "- Overall result: **$result**",
    "- Domain tests: $($domainTotals.tests); Passed: $($domainTotals.passed); Failures: $($domainTotals.failures); Errors: $($domainTotals.errors); Skipped: $($domainTotals.skipped)",
    "- Playwright: **$($playwright.result)**; Reports: $($playwright.reports)/2; Tests: $($playwright.tests); Expected: $($playwright.expected); Unexpected: $($playwright.unexpected); Flaky: $($playwright.flaky); Skipped: $($playwright.skipped)",
    "",
    "| Domain | Result | Suites | Tests | Passed | Failures | Errors | Skipped | Seconds |",
    "|---|---|---:|---:|---:|---:|---:|---:|---:|"
)
foreach ($domain in $domains) {
    $markdown += "| $($domain.scope) | $($domain.result) | $($domain.suites) | $($domain.tests) | $($domain.passed) | $($domain.failures) | $($domain.errors) | $($domain.skipped) | $($domain.durationSeconds) |"
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

Write-Host "Final test summary: domainTests=$($domainTotals.tests), playwrightExpected=$($playwright.expected), result=$result"
if ($result -ne "PASS") {
    exit 1
}

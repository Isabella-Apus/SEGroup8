param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("A", "B", "C", "D", "E")]
    [string]$Domain,

    [Parameter(Mandatory = $true)]
    [string]$ReportsRoot,

    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory
)

$ErrorActionPreference = "Stop"

$domainName = "DOMAIN_$($Domain.ToUpperInvariant())"
$reportsPath = [System.IO.Path]::GetFullPath($ReportsRoot)
$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

$reportFiles = @()
if (Test-Path -LiteralPath $reportsPath) {
    $reportFiles = @(
        Get-ChildItem -LiteralPath $reportsPath -Recurse -File -Filter "TEST-*.xml" |
            Sort-Object FullName
    )
}

$suites = @()
foreach ($reportFile in $reportFiles) {
    [xml]$document = Get-Content -Raw -LiteralPath $reportFile.FullName
    $suite = $document.testsuite
    if ($null -eq $suite) {
        continue
    }

    $tests = [int]$suite.tests
    $failures = [int]$suite.failures
    $errors = [int]$suite.errors
    $skipped = [int]$suite.skipped
    $passed = $tests - $failures - $errors - $skipped
    $durationSeconds = if ($suite.time) {
        [double]::Parse([string]$suite.time, [System.Globalization.CultureInfo]::InvariantCulture)
    } else {
        0.0
    }

    $relativeReport = $reportFile.FullName.Substring($reportsPath.Length).TrimStart(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar
    )
    $suites += [pscustomobject][ordered]@{
        name = [string]$suite.name
        tests = $tests
        passed = $passed
        failures = $failures
        errors = $errors
        skipped = $skipped
        durationSeconds = [Math]::Round($durationSeconds, 3)
        status = if (($failures + $errors) -eq 0) { "PASS" } else { "FAIL" }
        report = $relativeReport.Replace([System.IO.Path]::DirectorySeparatorChar, "/")
    }
}

$totals = [ordered]@{
    reportFiles = $reportFiles.Count
    suites = $suites.Count
    tests = [int]($suites | Measure-Object -Property tests -Sum).Sum
    passed = [int]($suites | Measure-Object -Property passed -Sum).Sum
    failures = [int]($suites | Measure-Object -Property failures -Sum).Sum
    errors = [int]($suites | Measure-Object -Property errors -Sum).Sum
    skipped = [int]($suites | Measure-Object -Property skipped -Sum).Sum
    durationSeconds = [Math]::Round([double]($suites | Measure-Object -Property durationSeconds -Sum).Sum, 3)
}

$result = if (
    $totals.suites -gt 0 -and
    $totals.tests -gt 0 -and
    ($totals.failures + $totals.errors) -eq 0
) { "PASS" } else { "FAIL" }

$summary = [ordered]@{
    schemaVersion = 1
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    scope = $domainName
    result = $result
    totals = $totals
    suites = $suites
}

$baseName = "domain-$($Domain.ToLowerInvariant())-test-summary"
$jsonPath = Join-Path $outputPath "$baseName.json"
$markdownPath = Join-Path $outputPath "$baseName.md"
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    $jsonPath,
    ($summary | ConvertTo-Json -Depth 8),
    $utf8WithoutBom
)

$markdown = @(
    "# $domainName test summary",
    "",
    "- Result: **$result**",
    "- Suites: $($totals.suites)",
    "- Tests: $($totals.tests); Passed: $($totals.passed); Failures: $($totals.failures); Errors: $($totals.errors); Skipped: $($totals.skipped)",
    "- Surefire time: $($totals.durationSeconds) seconds",
    "",
    "| Suite | Tests | Passed | Failures | Errors | Skipped | Seconds | Status |",
    "|---|---:|---:|---:|---:|---:|---:|---|"
)
foreach ($suite in $suites) {
    $markdown += "| ``$($suite.name)`` | $($suite.tests) | $($suite.passed) | $($suite.failures) | $($suite.errors) | $($suite.skipped) | $($suite.durationSeconds) | $($suite.status) |"
}
if ($suites.Count -eq 0) {
    $markdown += "| _No Surefire reports found_ | 0 | 0 | 0 | 0 | 0 | 0 | FAIL |"
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

Write-Host "$domainName summary: $($totals.tests) tests, result=$result"
if ($result -ne "PASS") {
    exit 1
}

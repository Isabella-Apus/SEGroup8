[CmdletBinding()]
param(
    [ValidateSet('DOMAIN_C', 'PLATFORM', 'UC11', 'UC12', 'UC13', 'UC14', 'UC15')]
    [string]$Suite = 'DOMAIN_C',

    [ValidateSet('test', 'verify')]
    [string]$Goal = 'verify',

    [string]$MavenRepository = '',

    [string]$TestClasses = '',

    [switch]$NoClean
)

$ErrorActionPreference = 'Stop'
$arguments = @($PSScriptRoot + '\run-domain-c-tests.mjs', '--suite', $Suite, '--goal', $Goal)
if ($MavenRepository) {
    $arguments += @('--maven-repository', $MavenRepository)
}
if ($TestClasses) {
    $arguments += @('--test-classes', $TestClasses)
}
if ($NoClean) {
    $arguments += '--no-clean'
}

& node @arguments
exit $LASTEXITCODE

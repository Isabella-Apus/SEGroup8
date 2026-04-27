$localConfigPath = Join-Path $PSScriptRoot "src\main\resources\application-local.yml"
$localExamplePath = Join-Path $PSScriptRoot "src\main\resources\application-local.example.yml"

Write-Host "[HIGH RISK] reset-all will delete existing business data in current database."
$confirm1 = Read-Host "Step 1/2: Type YES to continue"
if ($confirm1 -ne "YES") {
    Write-Host "Cancelled."
    exit 0
}

$confirm2 = Read-Host "Step 2/2: Type RESET-ALL to confirm data wipe"
if ($confirm2 -ne "RESET-ALL") {
    Write-Host "Cancelled."
    exit 0
}

if (!(Test-Path $localConfigPath) -and (Test-Path $localExamplePath)) {
    Write-Host "application-local.yml not found. Creating from example..."
    Copy-Item $localExamplePath $localConfigPath -Force
    Write-Host "Created src/main/resources/application-local.yml"
    Write-Host "Please edit database username/password in application-local.yml before first run."
}

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
$pomPath = Join-Path $PSScriptRoot "pom.xml"
if ($mvn) {
    Write-Host "Using Maven to start backend with reset-all profile..."
    $env:SPRING_PROFILES_ACTIVE = "reset-all"
    mvn -f $pomPath spring-boot:run
    exit $LASTEXITCODE
}

$jarPath = Join-Path $PSScriptRoot "target\\platform-backend-0.0.1-SNAPSHOT.jar"
if (Test-Path $jarPath) {
    Write-Host "Maven not found. Starting packaged jar with reset-all profile instead..."
    java -jar $jarPath --spring.profiles.active=reset-all
    exit $LASTEXITCODE
}

Write-Error "Maven is not installed and target/platform-backend-0.0.1-SNAPSHOT.jar was not found. Please install Maven or build the project in IntelliJ IDEA first."

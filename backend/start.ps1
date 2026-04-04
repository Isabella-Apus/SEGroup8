$localConfigPath = Join-Path $PSScriptRoot "src\main\resources\application-local.yml"
$localExamplePath = Join-Path $PSScriptRoot "src\main\resources\application-local.example.yml"

if (!(Test-Path $localConfigPath) -and (Test-Path $localExamplePath)) {
    Write-Host "application-local.yml not found. Creating from example..."
    Copy-Item $localExamplePath $localConfigPath -Force
    Write-Host "Created src/main/resources/application-local.yml"
    Write-Host "Please edit database username/password in application-local.yml before first run."
}

$mvn = Get-Command mvn -ErrorAction SilentlyContinue

if ($mvn) {
    Write-Host "Using Maven to start backend..."
    mvn spring-boot:run
    exit $LASTEXITCODE
}

$jarPath = Join-Path $PSScriptRoot "target\\platform-backend-0.0.1-SNAPSHOT.jar"

if (Test-Path $jarPath) {
    Write-Host "Maven not found. Starting packaged jar instead..."
    java -jar $jarPath
    exit $LASTEXITCODE
}

Write-Error "Maven is not installed and target/platform-backend-0.0.1-SNAPSHOT.jar was not found. Please install Maven or build the project in IntelliJ IDEA first."

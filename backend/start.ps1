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

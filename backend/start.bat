@echo off
where mvn >nul 2>nul
if %errorlevel%==0 (
  echo Using Maven to start backend...
  mvn spring-boot:run
  exit /b %errorlevel%
)

if exist "%~dp0target\platform-backend-0.0.1-SNAPSHOT.jar" (
  echo Maven not found. Starting packaged jar instead...
  java -jar "%~dp0target\platform-backend-0.0.1-SNAPSHOT.jar"
  exit /b %errorlevel%
)

echo Maven is not installed and target\platform-backend-0.0.1-SNAPSHOT.jar was not found.
echo Please install Maven or build the project in IntelliJ IDEA first.
exit /b 1

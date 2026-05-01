@echo off

if not exist "%~dp0src\main\resources\application-local.yml" (
  if exist "%~dp0src\main\resources\application-local.example.yml" (
    echo application-local.yml not found. Creating from example...
    copy /Y "%~dp0src\main\resources\application-local.example.yml" "%~dp0src\main\resources\application-local.yml" >nul
    echo Created src\main\resources\application-local.yml
    echo Please edit database username/password in application-local.yml before first run.
  )
)

where mvn >nul 2>nul
if %errorlevel%==0 (
  echo Using Maven to start backend with schema auto-sync...
  set "SPRING_PROFILES_ACTIVE=start-schema"
  mvn -f "%~dp0pom.xml" spring-boot:run
  exit /b %errorlevel%
)

if exist "%~dp0target\platform-backend-0.0.1-SNAPSHOT.jar" (
  echo Maven not found. Starting packaged jar with schema auto-sync instead...
  java -jar "%~dp0target\platform-backend-0.0.1-SNAPSHOT.jar" --spring.profiles.active=start-schema
  exit /b %errorlevel%
)

echo Maven is not installed and target\platform-backend-0.0.1-SNAPSHOT.jar was not found.
echo Please install Maven or build the project in IntelliJ IDEA first.
exit /b 1

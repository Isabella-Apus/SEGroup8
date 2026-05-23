@echo off

echo [HIGH RISK] reset-all will delete existing business data in current database.
set /p confirm1=Step 1/2: Type YES to continue: 
if /I not "%confirm1%"=="YES" (
  echo Cancelled.
  exit /b 0
)

set /p confirm2=Step 2/2: Type RESET-ALL to confirm data wipe: 
if /I not "%confirm2%"=="RESET-ALL" (
  echo Cancelled.
  exit /b 0
)

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
  echo Using Maven to start backend with reset-all profile...
  set "SPRING_PROFILES_ACTIVE=reset-all"
  mvn -f "%~dp0pom.xml" spring-boot:run
  exit /b %errorlevel%
)

if exist "%~dp0target\platform-backend-0.0.1-SNAPSHOT.jar" (
  echo Maven not found. Starting packaged jar with reset-all profile instead...
  java -jar "%~dp0target\platform-backend-0.0.1-SNAPSHOT.jar" --spring.profiles.active=reset-all
  exit /b %errorlevel%
)

echo Maven is not installed and target\platform-backend-0.0.1-SNAPSHOT.jar was not found.
echo Please install Maven or build the project in IntelliJ IDEA first.
exit /b 1

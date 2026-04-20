@echo off
setlocal

set "APP_HOME=%~dp0app"
set "BACKEND_JAR=%~dp0backend\backend-spring-0.1.0.jar"
set "RUNTIME_JAVA=%~dp0runtime\bin\javaw.exe"
set "RUNTIME_JAVA_CLI=%~dp0runtime\bin\java.exe"
set "API_PORT=8081"
set "API_URL=http://localhost:%API_PORT%/health"
set "API_LOG=%TEMP%\dental_clinic_api.log"

if exist "%RUNTIME_JAVA_CLI%" (
    set "JAVA_CMD=%RUNTIME_JAVA_CLI%"
    set "JAVAW_CMD=%RUNTIME_JAVA%"
) else (
    where java >nul 2>&1
    if errorlevel 1 (
        echo Embedded runtime is missing and Java is not installed.
        echo Please reinstall Dental Clinic System.
        pause
        exit /b 1
    )
    set "JAVA_CMD=java"
    set "JAVAW_CMD=javaw"
)

REM --- Start Spring backend ---
echo Starting backend server...
start /b "" "%JAVA_CMD%" -jar "%BACKEND_JAR%" --server.port=%API_PORT% >> "%API_LOG%" 2>&1

REM --- Wait for backend to be ready (max 40 seconds) ---
set /a ATTEMPTS=0
:WAIT_LOOP
if %ATTEMPTS% GEQ 40 (
    echo Backend did not start in time. Check log: %API_LOG%
    pause
    exit /b 1
)
powershell -Command "try { Invoke-WebRequest -Uri '%API_URL%' -UseBasicParsing -TimeoutSec 1 -ErrorAction Stop; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% == 0 goto BACKEND_READY
timeout /t 1 /nobreak >nul
set /a ATTEMPTS+=1
goto WAIT_LOOP

:BACKEND_READY
echo Backend is ready.

REM --- Launch desktop app ---
"%JAVAW_CMD%" -Dfile.encoding=UTF-8 -DAPI_BASE_URL=http://localhost:%API_PORT% -jar "%APP_HOME%\DentalClinicAppointment_ManagementSystem.jar"

if errorlevel 1 pause

endlocal

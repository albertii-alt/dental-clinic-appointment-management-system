@echo off
setlocal

set "APP_HOME=%~dp0app"
set "RUNTIME_JAVA=%~dp0runtime\bin\javaw.exe"
set "RUNTIME_JAVA_CLI=%~dp0runtime\bin\java.exe"
set "APP_LOG=%TEMP%\dental_clinic_app.log"

REM --- Use bundled JRE if available, otherwise fall back to system Java ---
if exist "%RUNTIME_JAVA_CLI%" (
    set "JAVA_CMD=%RUNTIME_JAVA_CLI%"
    set "JAVAW_CMD=%RUNTIME_JAVA%"
) else (
    where java >nul 2>&1
    if errorlevel 1 (
        echo Java is not installed. Please reinstall Dental Clinic System.
        pause
        exit /b 1
    )
    set "JAVA_CMD=java"
    set "JAVAW_CMD=javaw"
)

REM --- Launch desktop app ---
"%JAVAW_CMD%" -Dfile.encoding=UTF-8 -jar "%APP_HOME%\DentalClinicAppointment_ManagementSystem.jar" >> "%APP_LOG%" 2>&1

if errorlevel 1 (
    echo Application failed to start. Check log: %APP_LOG%
    pause
)

endlocal

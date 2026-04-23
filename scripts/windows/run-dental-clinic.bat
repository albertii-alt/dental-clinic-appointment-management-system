@echo off
setlocal

set "APP_DIR=%~dp0"
set "APP_HOME=%APP_DIR%app"
set "RUNTIME_JAVA=%APP_DIR%runtime\bin\javaw.exe"
set "RUNTIME_JAVA_CLI=%APP_DIR%runtime\bin\java.exe"
set "APP_LOG=%TEMP%\dental_clinic_app.log"
set "CONFIG_DIR=%USERPROFILE%\.dental_clinic"
set "CONFIG_FILE=%CONFIG_DIR%\db.properties"
set "CONFIG_TEMPLATE=%APP_DIR%db.properties.template"

REM --- Write db.properties if missing (fallback in case Inno Setup missed it) ---
if not exist "%CONFIG_DIR%" mkdir "%CONFIG_DIR%"
if not exist "%CONFIG_FILE%" (
    if exist "%CONFIG_TEMPLATE%" (
        copy /Y "%CONFIG_TEMPLATE%" "%CONFIG_FILE%" >nul
    )
)

REM --- Use bundled JRE if available, otherwise fall back to system Java ---
if exist "%RUNTIME_JAVA_CLI%" (
    set "JAVA_CMD=%RUNTIME_JAVA_CLI%"
    set "JAVAW_CMD=%RUNTIME_JAVA%"
) else (
    where java >nul 2>&1
    if errorlevel 1 (
        msg * "Java is not installed. Please reinstall Dental Clinic System."
        exit /b 1
    )
    set "JAVA_CMD=java"
    set "JAVAW_CMD=javaw"
)

REM --- Launch desktop app silently (no cmd window) ---
start "" "%JAVAW_CMD%" -Dfile.encoding=UTF-8 -jar "%APP_HOME%\DentalClinicAppointment_ManagementSystem.jar"

endlocal

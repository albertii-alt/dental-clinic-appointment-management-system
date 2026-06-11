@echo off
setlocal

set "APP_DIR=%~dp0"
set "APP_HOME=%APP_DIR%app"
set "RUNTIME_JAVA=%APP_DIR%runtime\bin\javaw.exe"
set "RUNTIME_JAVA_CLI=%APP_DIR%runtime\bin\java.exe"
set "CONFIG_DIR=%USERPROFILE%\.dental_clinic"
set "CONFIG_FILE=%CONFIG_DIR%\db.properties"
set "CONFIG_TEMPLATE=%APP_DIR%db.properties.template"

REM --- Sync db.properties from template (create if missing, update keys if exists) ---
if not exist "%CONFIG_DIR%" mkdir "%CONFIG_DIR%"
if exist "%CONFIG_TEMPLATE%" (
    powershell -NoProfile -Command "
        $template = Get-Content '%CONFIG_TEMPLATE%';
        if (-not (Test-Path '%CONFIG_FILE%')) {
            Copy-Item '%CONFIG_TEMPLATE%' '%CONFIG_FILE%';
        } else {
            $existing = Get-Content '%CONFIG_FILE%';
            $map = @{};
            foreach ($line in $existing) { if ($line -match '^([^#=]+)=(.*)$') { $map[$Matches[1].Trim()] = $line } };
            foreach ($line in $template) { if ($line -match '^([^#=]+)=(.*)$') { $map[$Matches[1].Trim()] = $line } };
            $result = foreach ($line in $existing) { if ($line -match '^([^#=]+)=(.*)$') { $map[$Matches[1].Trim()] } else { $line } };
            $result | Set-Content '%CONFIG_FILE%';
        }
    "
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

REM --- Launch desktop app silently ---
start "" "%JAVAW_CMD%" -Dfile.encoding=UTF-8 -jar "%APP_HOME%\DentalClinicAppointment_ManagementSystem.jar"

endlocal

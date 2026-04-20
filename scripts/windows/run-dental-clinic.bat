@echo off
setlocal

set "APP_HOME=%~dp0app"
set "RUNTIME_JAVA=%~dp0runtime\bin\javaw.exe"

if exist "%RUNTIME_JAVA%" (
    "%RUNTIME_JAVA%" -Dfile.encoding=UTF-8 -jar "%APP_HOME%\DentalClinicAppointment_ManagementSystem.jar"
) else (
    where java >nul 2>&1
    if errorlevel 1 (
        echo Embedded runtime is missing and Java is not installed.
        echo Please reinstall Dental Clinic System.
        pause
        exit /b 1
    )
    java -Dfile.encoding=UTF-8 -jar "%APP_HOME%\DentalClinicAppointment_ManagementSystem.jar"
)

if errorlevel 1 pause

endlocal

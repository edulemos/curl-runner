@echo off
setlocal

set "JAR=%~dp0curl-runner-1.0.0.jar"

if not exist "%JAR%" (
    echo ERROR: curl-runner-1.0.0.jar was not found in this folder.
    echo Keep this file next to the downloaded JAR and try again.
    pause
    exit /b 1
)

java -jar "%JAR%"

if errorlevel 1 (
    echo.
    echo The application could not be started. The error is shown above.
    pause
)

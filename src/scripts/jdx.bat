@echo off
REM jdx launcher script for Windows

setlocal enabledelayedexpansion

REM Resolve the script directory
set "SCRIPT_DIR=%~dp0"
set "APP_HOME=%SCRIPT_DIR%.."

REM Set up paths
set "LIB_DIR=%APP_HOME%\lib"
set "JAR_FILE=%LIB_DIR%\jdx-0.1.0-SNAPSHOT.jar"

REM Check if custom runtime exists (from jlink)
if exist "%APP_HOME%\runtime\bin\java.exe" (
    set "JAVA_CMD=%APP_HOME%\runtime\bin\java.exe"
) else (
    REM Fall back to system Java
    if defined JAVA_HOME (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    ) else (
        set "JAVA_CMD=java.exe"
    )
)

REM Check if Java is available
where /q "%JAVA_CMD%" 2>nul
if errorlevel 1 (
    echo Error: Java is not installed or not in PATH
    echo Please install JDK 25 or later
    exit /b 1
)

REM Launch the application
"%JAVA_CMD%" -jar "%JAR_FILE%" %*

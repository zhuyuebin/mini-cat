@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo =========================================
echo   MiniCat - 快速启动脚本
echo =========================================
echo.

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install JDK 17 or higher.
    echo Download: https://adoptium.net/
    pause
    exit /b 1
)

REM Show Java version
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%i
)
echo [OK] Java Version: %JAVA_VERSION:"=%
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [WARN] Maven not found, using Maven Wrapper
    REM Use full path for mvnw.cmd to ensure it's found
    set "SCRIPT_DIR=%~dp0"
    set MAVEN_CMD="%SCRIPT_DIR%mvnw.cmd"
) else (
    set MAVEN_CMD=mvn
    echo [OK] Maven installed
)

echo.
echo Building project...
echo.

REM Build project
call %MAVEN_CMD% clean package -DskipTests

if %errorlevel% neq 0 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo.
echo [OK] Build successful!
echo.

REM Find JAR file
for /f "delims=" %%i in ('dir /b /s target\*.jar ^| findstr /v "-sources.jar" ^| findstr /v "-javadoc.jar"') do (
    set JAR_FILE=%%i
    goto :found
)

:found
if "%JAR_FILE%"=="" (
    echo [ERROR] JAR file not found!
    pause
    exit /b 1
)

echo =========================================
echo   Starting MiniCat...
echo =========================================
echo.
echo Access URL: http://localhost:8888
echo.
echo Note: First startup may take a few minutes to download dependencies
echo.
echo Press Ctrl+C to stop the service
echo.

REM Start application with JVM parameters
java -Xms512m -Xmx1024m -jar "%JAR_FILE%"

pause

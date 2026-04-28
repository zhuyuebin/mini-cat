@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo =========================================
echo   MiniCat - 快速启动脚本
echo =========================================
echo.

REM 检查Java是否安装
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 错误: 未检测到Java,请先安装JDK 17或更高版本
    echo    下载地址: https://adoptium.net/
    pause
    exit /b 1
)

REM 显示Java版本
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%i
)
echo ✓ Java版本: %JAVA_VERSION:"=%
echo.

REM 检查Maven是否安装
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ⚠️  警告: 未检测到Maven,将使用Maven Wrapper
    set MAVEN_CMD=mvnw.cmd
) else (
    set MAVEN_CMD=mvn
    echo ✓ Maven已安装
)

echo.
echo 正在构建项目...
echo.

REM 构建项目
call %MAVEN_CMD% clean package -DskipTests

if %errorlevel% neq 0 (
    echo ❌ 构建失败!
    pause
    exit /b 1
)

echo.
echo ✓ 构建成功!
echo.

REM 查找JAR文件
for /f "delims=" %%i in ('dir /b /s target\*.jar ^| findstr /v "-sources.jar" ^| findstr /v "-javadoc.jar"') do (
    set JAR_FILE=%%i
    goto :found
)

:found
if "%JAR_FILE%"=="" (
    echo ❌ 未找到JAR文件!
    pause
    exit /b 1
)

echo =========================================
echo   启动MiniCat...
echo =========================================
echo.
echo 访问地址: http://localhost:8888
echo.
echo 提示: 首次启动需要下载依赖,可能需要几分钟
echo.
echo 按 Ctrl+C 停止服务
echo.

REM 启动应用 (配置JVM参数)
java -Xms512m -Xmx1024m -jar "%JAR_FILE%"

pause

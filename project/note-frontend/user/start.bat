@echo off
chcp 65001 >nul
echo ======================================
echo    Starting Frontend Dev Server...
echo ======================================
echo.

pushd "%~dp0"

if not exist "node_modules\" (
    echo [INFO] Installing dependencies...
    echo.
    call npm install
    if errorlevel 1 (
        echo.
        echo [ERROR] npm install failed
        pause
        exit /b 1
    )
    echo.
    echo [INFO] Done.
    echo.
)

echo Starting dev server...
echo.
call npm run dev -- --host
if errorlevel 1 (
    echo.
    echo [ERROR] Start failed
    pause
    exit /b 1
)

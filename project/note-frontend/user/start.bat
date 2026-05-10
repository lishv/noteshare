@echo off
chcp 65001 >nul
echo ======================================
echo    用户端前端 - 端口80
echo ======================================
echo.

pushd "%~dp0"

if not exist "dist\" (
    echo [INFO] dist 目录不存在，正在构建...
    call npm run build
    if errorlevel 1 (
        echo [ERROR] 构建失败
        pause
        exit /b 1
    )
)

echo [INFO] 同步构建产物到 nginx/html ...
xcopy /E /Y /Q "dist\*" "nginx\html\" >nul

echo [INFO] 启动 nginx...
cd nginx
start "" nginx.exe
cd ..

echo [INFO] 用户端已启动: http://localhost:80
echo [INFO] 停止请运行 stop.bat
timeout /t 3 >nul

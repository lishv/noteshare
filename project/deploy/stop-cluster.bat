@echo off
chcp 65001 >nul
echo ======================================
echo    停止后端集群
echo ======================================
echo.

echo [INFO] 停止实例 8081...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8081.*LISTENING"') do (
    taskkill /F /PID %%a 2>nul
)

echo [INFO] 停止实例 8082...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8082.*LISTENING"') do (
    taskkill /F /PID %%a 2>nul
)

echo [INFO] 停止实例 8083...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8083.*LISTENING"') do (
    taskkill /F /PID %%a 2>nul
)

echo [INFO] 后端集群已停止
timeout /t 3 >nul

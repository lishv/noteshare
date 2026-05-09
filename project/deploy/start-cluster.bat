@echo off
chcp 65001 >nul
echo ======================================
echo    启动后端集群 (3实例: 8081/8082/8083)
echo ======================================
echo.

pushd "%~dp0.."

set JAR_PATH=note-backend\target\note-backend-1.0.0.jar

if not exist "%JAR_PATH%" (
    echo [ERROR] 未找到 %JAR_PATH%
    echo [INFO] 请先在 note-backend 目录执行: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo [INFO] 启动实例 1 (端口 8081)...
start "note-backend-8081" java -jar %JAR_PATH% --server.port=8081 --spring.profiles.active=cluster

echo [INFO] 启动实例 2 (端口 8082)...
start "note-backend-8082" java -jar %JAR_PATH% --server.port=8082 --spring.profiles.active=cluster

echo [INFO] 启动实例 3 (端口 8083)...
start "note-backend-8083" java -jar %JAR_PATH% --server.port=8083 --spring.profiles.active=cluster

echo.
echo [INFO] 3个后端实例已启动 (8081/8082/8083)
echo [INFO] 请配合 deploy\nginx-lb.conf 使用负载均衡
echo [INFO] 停止请运行 stop-cluster.bat
timeout /t 5 >nul

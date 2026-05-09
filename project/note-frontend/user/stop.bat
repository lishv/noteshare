@echo off
chcp 65001 >nul
pushd "%~dp0nginx"
nginx.exe -s quit 2>nul
echo 用户端 nginx 已停止

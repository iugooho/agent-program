@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
start "backend" cmd /c "mvn -f backend\pom.xml compile exec:java"
cd frontend
if not exist node_modules ( call npm install )
call npm run dev

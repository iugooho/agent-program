@echo off
chcp 65001 >nul
cd /d "%~dp0.."
call mvn -q compile exec:java

@echo off
setlocal EnableExtensions
REM Use UTF-8 code page before any output (fixes Chinese garbled text)
chcp 65001 >nul 2>&1

title AgentPlatform - Personal Agent Platform
cd /d "%~dp0.."

REM Prefer Windows PowerShell 5.1+ with UTF-8 console, then pwsh
where pwsh >nul 2>&1
if %errorlevel%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -Command "[Console]::OutputEncoding=[Text.UTF8Encoding]::UTF8; [Console]::InputEncoding=[Console]::OutputEncoding; $OutputEncoding=[Console]::OutputEncoding; chcp 65001 | Out-Null; & '%~dp0启动-个人Agent平台.ps1' %*"
) else (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "[Console]::OutputEncoding=[Text.UTF8Encoding]::UTF8; [Console]::InputEncoding=[Console]::OutputEncoding; $OutputEncoding=[Console]::OutputEncoding; chcp 65001 | Out-Null; & '%~dp0启动-个人Agent平台.ps1' %*"
)

set EXIT_CODE=%errorlevel%
if not "%EXIT_CODE%"=="0" pause
endlocal & exit /b %EXIT_CODE%

@echo off
chcp 65001 >nul 2>&1
title AgentPlatform
cd /d "%~dp0"
call scripts\start-agent-platform.bat %*

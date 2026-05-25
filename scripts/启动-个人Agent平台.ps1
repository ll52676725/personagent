#Requires -Version 5.1
<#
.SYNOPSIS
  一键启动「个人Agent平台」(AgentPlatform)
#>
param(
    [switch]$Dev,
    [switch]$SkipBuild
)

# Fix garbled Chinese in CMD / Windows Terminal (legacy console)
try {
    chcp 65001 | Out-Null
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [Console]::OutputEncoding = $utf8
    [Console]::InputEncoding = $utf8
    $OutputEncoding = $utf8
} catch {
    # ignore
}

$ErrorActionPreference = "Stop"

$ServiceDisplayName = "个人Agent平台"
$ServiceCodeName = "AgentPlatform"
$BackendRoot = Split-Path $PSScriptRoot -Parent
$FrontendDir = Join-Path $BackendRoot "frontend"
$Port = 8080
$DevPort = 5173

function Write-Banner {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  $ServiceDisplayName ($ServiceCodeName)" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

function Resolve-CommandPath([string]$Name, [string[]]$Candidates) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($c in $Candidates) {
        if (Test-Path $c) { return $c }
    }
    return $null
}

function Ensure-NodeModules {
    if (-not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
        Write-Host "[前端] 安装依赖..." -ForegroundColor Yellow
        Push-Location $FrontendDir
        if (Test-Path "package-lock.json") {
            npm ci
        } else {
            npm install
        }
        Pop-Location
    }
}

function Build-Frontend {
    Write-Host "[前端] 构建并打包到 Spring Boot static..." -ForegroundColor Yellow
    Push-Location $FrontendDir
    $env:PYTHONIOENCODING = "utf-8"
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "前端构建失败" }
    Pop-Location
    Write-Host "[前端] 构建完成" -ForegroundColor Green
}

function Start-Backend {
    param([string]$MavenExe)
    Write-Host "[后端] 启动 $ServiceDisplayName -> http://localhost:$Port" -ForegroundColor Yellow
    Push-Location $BackendRoot
    $env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"
    & $MavenExe spring-boot:run "-Dspring-boot.run.jvmArguments=-Dfile.encoding=UTF-8"
    Pop-Location
}

function Start-DevMode {
    param([string]$MavenExe, [string]$NpmExe)
    Write-Host "[开发] 后端: http://localhost:$Port  前端: http://localhost:$DevPort" -ForegroundColor Yellow
    Push-Location $BackendRoot
    $env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"
    $backendJob = Start-Job -ScriptBlock {
        param($Root, $Mvn)
        Set-Location $Root
        $env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"
        & $Mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dfile.encoding=UTF-8" 2>&1
    } -ArgumentList $BackendRoot, $MavenExe

    Start-Sleep -Seconds 8
    Push-Location $FrontendDir
    try {
        & $NpmExe run dev
    } finally {
        Pop-Location
        if ($backendJob.State -eq "Running") {
            Stop-Job $backendJob -Force
            Remove-Job $backendJob -Force
        }
        Pop-Location
    }
}

Write-Banner

$npm = Resolve-CommandPath "npm" @()
$mvn = Resolve-CommandPath "mvn" @(
    "$env:MAVEN_HOME\bin\mvn.cmd",
    "C:\Program Files\Apache\maven\bin\mvn.cmd"
)
$java = Resolve-CommandPath "java" @()

if (-not $npm) { throw "未找到 npm，请先安装 Node.js 18+" }
if (-not $mvn) { throw "未找到 mvn，请先安装 Maven 并加入 PATH" }
if (-not $java) { throw "未找到 java，请先安装 JDK 17+" }

if (-not (Test-Path $FrontendDir)) {
    throw "未找到前端目录: $FrontendDir"
}

Ensure-NodeModules

if ($Dev) {
    Start-DevMode -MavenExe $mvn -NpmExe $npm
} else {
    if (-not $SkipBuild) {
        Build-Frontend
    } else {
        Write-Host "[提示] 跳过前端构建 (-SkipBuild)" -ForegroundColor DarkYellow
    }
    Write-Host ""
    Write-Host "访问地址: http://localhost:$Port" -ForegroundColor Green
    Write-Host "按 Ctrl+C 停止服务" -ForegroundColor DarkGray
    Write-Host ""
    Start-Backend -MavenExe $mvn
}

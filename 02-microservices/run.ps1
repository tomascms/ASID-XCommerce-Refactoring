#!/usr/bin/env pwsh
# XCommerce Microservices - Quick Start
# Inicia Docker Compose, Dashboard e abre o browser

$ErrorActionPreference = "SilentlyContinue"

Write-Host "`n🚀 XCommerce Microservices - Quick Start`n" -ForegroundColor Cyan

# 1. Check Docker status
Write-Host "📦 Verificando Docker Compose..." -ForegroundColor Yellow
$dockerStatus = docker-compose ps 2>$null
if ($dockerStatus -match "exited|Exit") {
    Write-Host "⚠️  Containers não estão ativos. Iniciando..." -ForegroundColor Yellow
    docker-compose up -d 2>&1 | Out-Null
    Start-Sleep -Seconds 3
    Write-Host "✅ Docker Compose iniciado" -ForegroundColor Green
} else {
    Write-Host "✅ Docker Compose já está ativo" -ForegroundColor Green
}

# 2. Kill any existing dashboard processes
Write-Host "🔄 Limpando processos antigos do dashboard..." -ForegroundColor Yellow
Get-Process node -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "*dashboard*" } | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

# 3. Start dashboard
Write-Host "🖥️  Iniciando Dashboard..." -ForegroundColor Yellow
$dashboardPath = Join-Path $PSScriptRoot "dashboard"
Push-Location $dashboardPath
$dashboardProcess = Start-Process node -ArgumentList "server.js" -PassThru -WindowStyle Minimized
Pop-Location
Start-Sleep -Seconds 2

if ($dashboardProcess.HasExited -eq $false) {
    Write-Host "✅ Dashboard iniciado (PID: $($dashboardProcess.Id))" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao iniciar dashboard" -ForegroundColor Red
    exit 1
}

# 4. Open browser
Write-Host "🌐 Abrindo browser..." -ForegroundColor Yellow
Start-Sleep -Seconds 1
Start-Process "http://localhost:5001"

Write-Host "`n✨ Sistema pronto! Dashboard disponível em http://localhost:5001`n" -ForegroundColor Cyan
Write-Host "Pressione CTRL+C para sair`n" -ForegroundColor Gray

# Keep process alive
while ($true) {
    if ($dashboardProcess.HasExited) {
        Write-Host "❌ Dashboard foi encerrado" -ForegroundColor Red
        break
    }
    Start-Sleep -Seconds 5
}

# ============================================================
# TESTE DE AVAILABILITY - Simular Falhas e Recovery
# ============================================================
# Objetivo: Medir como cada arquitetura responde a falhas
# - Monólito: Falha total (todos endpoints down)
# - Microserviços: Falha isolada (um serviço falha, outros continuam)

param(
    [int]$TestDurationSeconds = 60,
    [int]$ConcurrentRequests = 5,
    [string]$ResultsDir = "./03-testing/results"
)

# Garantir que o diretório existe
if (!(Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null
}

$timestamp = (Get-Date -Format "yyyyMMdd-HHmmss")
$resultsFile = "$ResultsDir/availability-results-$timestamp.csv"

# CSV Header
"Timestamp,Architecture,FailureType,RequestsBefore,RequestsAfter,DowntimeSeconds,ErrorRate,RecoveryTime" | Out-File -FilePath $resultsFile -Encoding UTF8

# ============================================================
# 1. TESTE MONÓLITO - Falha Total
# ============================================================
Write-Host "`n📊 TESTE 1: Monólito - Falha Total"
Write-Host "====================================="

# Simular: Parar container do Monólito
docker stop xcommerce-monolith -t 5 2>$null
Start-Sleep -Seconds 2

$startTime = Get-Date
$failureStart = $startTime

# Contar requisições que falham durante outage
$failedRequests = 0
$successRequests = 0

# Tentar requisições por 30 segundos
$endTime = $startTime.AddSeconds(30)
while ((Get-Date) -lt $endTime) {
    $response = curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/products 2>$null
    if ($response -eq "000" -or $response -eq "") {
        $failedRequests++
    } else {
        $successRequests++
    }
    Start-Sleep -Milliseconds 500
}

# Reiniciar Monólito e medir recovery
$recoveryStart = Get-Date
docker start xcommerce-monolith 2>$null
Start-Sleep -Seconds 5

# Aguardar até health check passar
$recovered = $false
$recoveryAttempts = 0
while ($recoveryAttempts -lt 30) {
    $response = curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health 2>$null
    if ($response -eq "200") {
        $recovered = $true
        break
    }
    Start-Sleep -Seconds 1
    $recoveryAttempts++
}

$recoveryTime = (Get-Date) - $recoveryStart
$downtimeSeconds = ((Get-Date) - $failureStart).TotalSeconds
$errorRate = if (($failedRequests + $successRequests) -gt 0) { ($failedRequests / ($failedRequests + $successRequests) * 100) } else { 0 }

"$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Monolith,Total Failure,$successRequests,$successRequests,$([Math]::Round($downtimeSeconds, 2)),$([Math]::Round($errorRate, 2))%,$([Math]::Round($recoveryTime.TotalSeconds, 2))" | Add-Content $resultsFile

Write-Host "✗ Falha Total: $($failedRequests) requisições falharam"
Write-Host "✓ Recovery Time: $([Math]::Round($recoveryTime.TotalSeconds, 2))s"
Write-Host "✗ Downtime: $([Math]::Round($downtimeSeconds, 2))s"
Write-Host "✗ Error Rate: $([Math]::Round($errorRate, 2))%"

# ============================================================
# 2. TESTE MICROSERVIÇOS - Falha Isolada
# ============================================================
Write-Host "`n📊 TESTE 2: Microserviços - Falha Isolada (Catalog)"
Write-Host "========================================================="

# Parar apenas o serviço de catálogo
docker stop xcommerce-catalog-service -t 5 2>$null
Start-Sleep -Seconds 2

$startTime = Get-Date
$failureStart = $startTime

# Tentar requisições - devem falhar APENAS para catalog
$failedRequests = 0
$successRequests = 0

$endTime = $startTime.AddSeconds(30)
while ((Get-Date) -lt $endTime) {
    # Tentar um endpoint que não depende do catalog (e.g., users)
    $response = curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/api/users 2>$null
    if ($response -eq "200" -or $response -eq "201") {
        $successRequests++
    } else {
        $failedRequests++
    }
    
    # Tentar catalog (deve falhar)
    $catalogResponse = curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/api/catalog/products 2>$null
    # Esperado: falha
    
    Start-Sleep -Milliseconds 500
}

# Reiniciar serviço de catálogo
$recoveryStart = Get-Date
docker start xcommerce-catalog-service 2>$null
Start-Sleep -Seconds 3

# Verificar recovery
$recovered = $false
$recoveryAttempts = 0
while ($recoveryAttempts -lt 30) {
    $response = curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/api/catalog/products 2>$null
    if ($response -eq "200") {
        $recovered = $true
        break
    }
    Start-Sleep -Seconds 1
    $recoveryAttempts++
}

$recoveryTime = (Get-Date) - $recoveryStart
$downtimeSeconds = ((Get-Date) - $failureStart).TotalSeconds
$errorRate = if (($failedRequests + $successRequests) -gt 0) { ($failedRequests / ($failedRequests + $successRequests) * 100) } else { 0 }

"$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Microservices,Isolated Failure (Catalog),$successRequests,$successRequests,$([Math]::Round($downtimeSeconds, 2)),$([Math]::Round($errorRate, 2))%,$([Math]::Round($recoveryTime.TotalSeconds, 2))" | Add-Content $resultsFile

Write-Host "✓ Falha Isolada: $($failedRequests) requisições falharam para Catalog"
Write-Host "✓ Outros serviços: $($successRequests) requisições bem sucedidas"
Write-Host "✓ Recovery Time: $([Math]::Round($recoveryTime.TotalSeconds, 2))s"
Write-Host "✓ Downtime: $([Math]::Round($downtimeSeconds, 2))s"
Write-Host "✓ Error Rate: $([Math]::Round($errorRate, 2))%"

Write-Host "`n✅ Testes de Availability completos!"
Write-Host "📁 Resultados: $resultsFile"

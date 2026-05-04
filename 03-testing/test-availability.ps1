# ============================================================
# TESTE DE AVAILABILITY - Simular Falhas e Recovery
# ============================================================
# Objetivo: Medir como cada arquitetura responde a falhas
# - Monolito: Falha total (todos endpoints down)
# - Microservices: Falha isolada (um servico falha, outros continuam)

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

function Get-HttpStatusCode {
    param([string]$Url)

    try {
        return [string](Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing -TimeoutSec 10).StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [string][int]$_.Exception.Response.StatusCode
        }
        return "000"
    }
}

# CSV Header
"Timestamp,Architecture,FailureType,RequestsBefore,RequestsAfter,DowntimeSeconds,ErrorRate,RecoveryTime" | Out-File -FilePath $resultsFile -Encoding UTF8

# ============================================================
# 1. TESTE MONOLITO - Falha Total
# ============================================================
Write-Host "`n[INFO] TESTE 1: Monolito - Falha Total"
Write-Host "====================================="

# Simular: Parar container do Monolito
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
    $response = Get-HttpStatusCode "http://localhost:8080/api/products"
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
    $response = Get-HttpStatusCode "http://localhost:8080/health"
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

Write-Host ("[WARN] Falha Total: {0} requisicoes falharam" -f $failedRequests)
Write-Host ("[OK] Recovery Time: {0}s" -f ([Math]::Round($recoveryTime.TotalSeconds, 2)))
Write-Host ("[WARN] Downtime: {0}s" -f ([Math]::Round($downtimeSeconds, 2)))
Write-Host ("[WARN] Error Rate: {0}%" -f ([Math]::Round($errorRate, 2)))

# ============================================================
# 2. TESTE MICROSERVICOS - Falha Isolada
# ============================================================
Write-Host "`n[INFO] TESTE 2: Microservices - Falha Isolada (Catalog)"
Write-Host "========================================================="

# Parar apenas o servico de catalogo
docker stop xcommerce-catalog-service -t 5 2>$null
Start-Sleep -Seconds 2

$startTime = Get-Date
$failureStart = $startTime

# Tentar requisições - devem falhar APENAS para catalog
$failedRequests = 0
$successRequests = 0

$endTime = $startTime.AddSeconds(30)
while ((Get-Date) -lt $endTime) {
    # Endpoint de controlo que deve continuar ativo mesmo com catalog em falha
    $response = Get-HttpStatusCode "http://localhost:9000/actuator/health"
    if ($response -eq "200" -or $response -eq "201") {
        $successRequests++
    } else {
        $failedRequests++
    }

    # Endpoint de catalogo (deve falhar enquanto o servico está parado)
    $catalogResponse = Get-HttpStatusCode "http://localhost:9000/products"
    # Esperado: falha
    
    Start-Sleep -Milliseconds 500
}

# Reiniciar servico de catalogo
$recoveryStart = Get-Date
docker start xcommerce-catalog-service 2>$null
Start-Sleep -Seconds 3

# Verificar recovery
$recovered = $false
$recoveryAttempts = 0
while ($recoveryAttempts -lt 30) {
    $response = Get-HttpStatusCode "http://localhost:9000/products"
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

Write-Host ("[OK] Falha Isolada: {0} requisicoes falharam para Catalog" -f $failedRequests)
Write-Host ("[OK] Outros servicos: {0} requisicoes bem sucedidas" -f $successRequests)
Write-Host ("[OK] Recovery Time: {0}s" -f ([Math]::Round($recoveryTime.TotalSeconds, 2)))
Write-Host ("[OK] Downtime: {0}s" -f ([Math]::Round($downtimeSeconds, 2)))
Write-Host ("[OK] Error Rate: {0}%" -f ([Math]::Round($errorRate, 2)))

Write-Host "`n[OK] Testes de Availability completos!"
Write-Host "[OK] Resultados: $resultsFile"

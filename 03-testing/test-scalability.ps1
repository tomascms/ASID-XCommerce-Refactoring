# ============================================================
# TESTE DE ESCALABILIDADE - Carga Crescente ate o limite
# ============================================================
# Objetivo: Encontrar o ponto de saturação de cada arquitetura

param(
    [int]$StartConcurrency = 5,
    [int]$MaxConcurrency = 100,
    [int]$ConcurrencyStep = 5,
    [int]$DurationPerLevel = 30,
    [string]$ResultsDir = "./03-testing/results"
)

if (!(Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null
}

$timestamp = (Get-Date -Format "yyyyMMdd-HHmmss")
$resultsFile = "$ResultsDir/scalability-results-$timestamp.csv"

# CSV Header
"Timestamp,Architecture,ConcurrentRequests,TotalRequests,SuccessCount,ErrorCount,AvgLatency,P99Latency,Throughput,CPU,Memory" | Out-File -FilePath $resultsFile -Encoding UTF8

# ============================================================
# Funcao para testar um nivel de carga
# ============================================================
function Test-LoadLevel {
    param(
        [string]$Architecture,
        [string]$Endpoint,
        [int]$ConcurrentRequests,
        [int]$DurationSeconds,
        [string[]]$MonitorContainers
    )
    
    Write-Host "`n[INFO] Testando $Architecture com $ConcurrentRequests requisicoes concorrentes..."
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $jobs = @()
    $sampleLatency = $null
    
    # Criar jobs concorrentes
    for ($i = 0; $i -lt $ConcurrentRequests; $i++) {
        $job = Start-Job -ScriptBlock {
            param($ep, $duration)
            $startTime = Get-Date
            $requestCount = 0
            $errorCount = 0
            $latencies = @()
            
            while (((Get-Date) - $startTime).TotalSeconds -lt $duration) {
                $reqStart = Get-Date
                try {
                    $response = Invoke-WebRequest -Uri "http://localhost:$ep" -UseBasicParsing -Method Get -TimeoutSec 30
                    $statusCode = [int]$response.StatusCode
                    if ($statusCode -eq 200 -or $statusCode -eq 201) {
                        $requestCount++
                    } else {
                        $errorCount++
                    }
                } catch {
                    $errorCount++
                }

                $latency = ((Get-Date) - $reqStart).TotalMilliseconds
                $latencies += $latency
            }
            
                return [pscustomobject]@{
                Requests = $requestCount
                Errors = $errorCount
                Latencies = $latencies
            }
        } -ArgumentList $Endpoint, $DurationSeconds
        
        $jobs += $job
    }
    
    # Aguardar conclusao
    $jobResults = $jobs | Wait-Job | Receive-Job
    $stopwatch.Stop()
    
    # Agregar resultados
        $totalRequests = 0
        $totalErrors = 0
    $allLatencies = @()
        foreach ($result in $jobResults) {
            if ($null -eq $result) {
                continue
            }
            if ($result.PSObject.Properties.Name -contains 'Requests') {
                $totalRequests += [int]$result.Requests
            }
            if ($result.PSObject.Properties.Name -contains 'Errors') {
                $totalErrors += [int]$result.Errors
            }
            if ($result.PSObject.Properties.Name -contains 'Latencies') {
                $allLatencies += $result.Latencies
            }
    }
    
    $avgLatency = if ($allLatencies.Count -gt 0) { ($allLatencies | Measure-Object -Average).Average } else { 0 }
    $p99Latency = if ($allLatencies.Count -gt 0) { $allLatencies | Sort-Object | Select-Object -Skip ([Math]::Floor($allLatencies.Count * 0.99)) | Select-Object -First 1 } else { 0 }
        $throughput = if ($stopwatch.Elapsed.TotalSeconds -gt 0) { $totalRequests / $stopwatch.Elapsed.TotalSeconds } else { 0 }
    
    # Coletar metricas de CPU/Memory
    $stats = docker stats --no-stream $MonitorContainers --format "table {{.CPUPerc}}\t{{.MemUsage}}" 2>$null
    $avgCPU = 0
    $avgMemory = 0
    
    if ($stats) {
        # Parse Docker stats output
        $statsLines = $stats | Select-Object -Skip 1
        foreach ($line in $statsLines) {
            if ($line -match '(\d+\.\d+)%.*(\d+\.\d+)') {
                $avgCPU = [double]$matches[1]
                $avgMemory = [double]$matches[2]
            }
        }
    }
    
    Write-Host "   [OK] Requisicoes:   $totalRequests"
    Write-Host ("   [OK] Erros:        {0}" -f $totalErrors)
    Write-Host ("   [OK] Latencia Med: {0}ms" -f ([Math]::Round($avgLatency, 2)))
    Write-Host ("   [OK] Latencia P99: {0}ms" -f ([Math]::Round($p99Latency, 2)))
    Write-Host ("   [OK] Throughput:   {0} req/s" -f ([Math]::Round($throughput, 2)))
    Write-Host ("   [OK] CPU:          {0}%" -f ([Math]::Round($avgCPU, 2)))
    Write-Host ("   [OK] Memory:       {0}MiB" -f ([Math]::Round($avgMemory, 2)))
    
    return @{
        TotalRequests = $totalRequests
        ErrorCount = $totalErrors
        SuccessCount = $totalRequests - $totalErrors
        AvgLatency = $avgLatency
        P99Latency = $p99Latency
        Throughput = $throughput
        CPU = $avgCPU
        Memory = $avgMemory
    }
}

# ============================================================
# TESTE 1: Monolito - Escalabilidade
# ============================================================
Write-Host "`n[INFO] TESTE 1: Monolito - Escalabilidade"
Write-Host "======================================"

if ((Get-Command docker -ErrorAction SilentlyContinue) -and (docker ps --format "{{.Names}}" | Select-String -SimpleMatch "xcommerce-monolith")) {
    $concurrency = $StartConcurrency
    while ($concurrency -le $MaxConcurrency) {
        $result = Test-LoadLevel -Architecture "Monolith" -Endpoint "8080/api/products" -ConcurrentRequests $concurrency -DurationSeconds $DurationPerLevel -MonitorContainers "xcommerce-monolith"
    
        "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Monolith,$concurrency,$($result.TotalRequests),$($result.SuccessCount),$($result.ErrorCount),$([Math]::Round($result.AvgLatency, 2)),$([Math]::Round($result.P99Latency, 2)),$([Math]::Round($result.Throughput, 2)),$([Math]::Round($result.CPU, 2)),$([Math]::Round($result.Memory, 2))" | Add-Content $resultsFile
    
        # Se taxa de erro > 10%, parar de aumentar
            $totalAttempts = $result.SuccessCount + $result.ErrorCount
            if ($totalAttempts -gt 0 -and ((($result.ErrorCount / $totalAttempts) * 100) -gt 10)) {
            Write-Host "[WARN] Taxa de erro > 10%, finalizando escalabilidade para Monolito"
            break
        }
    
        $concurrency += $ConcurrencyStep
        Start-Sleep -Seconds 5
    }
} else {
    Write-Host "[WARN] Monolith nao encontrado; a saltar o teste de monolito"
}

# ============================================================
# TESTE 2: Microservices - Escalabilidade
# ============================================================
Write-Host "`n[INFO] TESTE 2: Microservices - Escalabilidade"
Write-Host "=========================================="

$concurrency = $StartConcurrency
while ($concurrency -le $MaxConcurrency) {
    $result = Test-LoadLevel -Architecture "Microservices" -Endpoint "9000/products" -ConcurrentRequests $concurrency -DurationSeconds $DurationPerLevel -MonitorContainers "xcommerce-gateway", "xcommerce-catalog-service"
    
    "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Microservices,$concurrency,$($result.TotalRequests),$($result.SuccessCount),$($result.ErrorCount),$([Math]::Round($result.AvgLatency, 2)),$([Math]::Round($result.P99Latency, 2)),$([Math]::Round($result.Throughput, 2)),$([Math]::Round($result.CPU, 2)),$([Math]::Round($result.Memory, 2))" | Add-Content $resultsFile
    
        $totalAttempts = $result.SuccessCount + $result.ErrorCount
        if ($totalAttempts -gt 0 -and ((($result.ErrorCount / $totalAttempts) * 100) -gt 10)) {
        Write-Host "[WARN] Taxa de erro > 10%, finalizando escalabilidade para Microservices"
        break
    }
    
    $concurrency += $ConcurrencyStep
    Start-Sleep -Seconds 5
}

Write-Host "`n[OK] Testes de Escalabilidade completos!"
Write-Host "[OK] Resultados: $resultsFile"

# ============================================================
# TESTE DE ESCALABILIDADE - Carga Crescente até o Limite
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
# Função para testar um nível de carga
# ============================================================
function Test-LoadLevel {
    param(
        [string]$Architecture,
        [string]$Endpoint,
        [int]$ConcurrentRequests,
        [int]$DurationSeconds,
        [string[]]$MonitorContainers
    )
    
    Write-Host "`n🔥 Testando $Architecture com $ConcurrentRequests requisições concorrentes..."
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $results = @()
    $jobs = @()
    
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
                $response = curl -s -o /dev/null -w "%{http_code}" http://localhost:$ep 2>$null
                $latency = ((Get-Date) - $reqStart).TotalMilliseconds
                $latencies += $latency
                
                if ($response -eq "200" -or $response -eq "201") {
                    $requestCount++
                } else {
                    $errorCount++
                }
            }
            
            return @{
                Requests = $requestCount
                Errors = $errorCount
                Latencies = $latencies
            }
        } -ArgumentList $Endpoint, $DurationSeconds
        
        $jobs += $job
    }
    
    # Aguardar conclusão
    $jobResults = $jobs | Wait-Job | Receive-Job
    $stopwatch.Stop()
    
    # Agregar resultados
    $totalRequests = ($jobResults | Measure-Object -Property Requests -Sum).Sum
    $totalErrors = ($jobResults | Measure-Object -Property Errors -Sum).Sum
    $allLatencies = @()
    foreach ($result in $jobResults) {
        $allLatencies += $result.Latencies
    }
    
    $avgLatency = if ($allLatencies.Count -gt 0) { ($allLatencies | Measure-Object -Average).Average } else { 0 }
    $p99Latency = if ($allLatencies.Count -gt 0) { $allLatencies | Sort-Object | Select-Object -Skip ([Math]::Floor($allLatencies.Count * 0.99)) | Select-Object -First 1 } else { 0 }
    $throughput = $totalRequests / $stopwatch.Elapsed.TotalSeconds
    
    # Coletar métricas de CPU/Memory
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
    
    Write-Host "   ✓ Requisições:   $totalRequests"
    Write-Host "   ✓ Erros:         $totalErrors"
    Write-Host "   ✓ Latência Méd:  $([Math]::Round($avgLatency, 2))ms"
    Write-Host "   ✓ Latência P99:  $([Math]::Round($p99Latency, 2))ms"
    Write-Host "   ✓ Throughput:    $([Math]::Round($throughput, 2)) req/s"
    Write-Host "   ✓ CPU:           $([Math]::Round($avgCPU, 2))%"
    Write-Host "   ✓ Memory:        $([Math]::Round($avgMemory, 2))MiB"
    
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
# TESTE 1: Monólito - Escalabilidade
# ============================================================
Write-Host "`n📊 TESTE 1: Monólito - Escalabilidade"
Write-Host "======================================"

$concurrency = $StartConcurrency
while ($concurrency -le $MaxConcurrency) {
    $result = Test-LoadLevel -Architecture "Monolith" -Endpoint "8080/api/products" -ConcurrentRequests $concurrency -DurationSeconds $DurationPerLevel -MonitorContainers "xcommerce-monolith"
    
    "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Monolith,$concurrency,$($result.TotalRequests),$($result.SuccessCount),$($result.ErrorCount),$([Math]::Round($result.AvgLatency, 2)),$([Math]::Round($result.P99Latency, 2)),$([Math]::Round($result.Throughput, 2)),$([Math]::Round($result.CPU, 2)),$([Math]::Round($result.Memory, 2))" | Add-Content $resultsFile
    
    # Se taxa de erro > 10%, parar de aumentar
    if ((($result.ErrorCount / ($result.SuccessCount + $result.ErrorCount)) * 100) -gt 10) {
        Write-Host "⚠️  Taxa de erro > 10%, finalizando escalabilidade para Monólito"
        break
    }
    
    $concurrency += $ConcurrencyStep
    Start-Sleep -Seconds 5
}

# ============================================================
# TESTE 2: Microserviços - Escalabilidade
# ============================================================
Write-Host "`n📊 TESTE 2: Microserviços - Escalabilidade"
Write-Host "=========================================="

$concurrency = $StartConcurrency
while ($concurrency -le $MaxConcurrency) {
    $result = Test-LoadLevel -Architecture "Microservices" -Endpoint "9000/api/catalog/products" -ConcurrentRequests $concurrency -DurationSeconds $DurationPerLevel -MonitorContainers "xcommerce-gateway", "xcommerce-catalog-service"
    
    "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Microservices,$concurrency,$($result.TotalRequests),$($result.SuccessCount),$($result.ErrorCount),$([Math]::Round($result.AvgLatency, 2)),$([Math]::Round($result.P99Latency, 2)),$([Math]::Round($result.Throughput, 2)),$([Math]::Round($result.CPU, 2)),$([Math]::Round($result.Memory, 2))" | Add-Content $resultsFile
    
    if ((($result.ErrorCount / ($result.SuccessCount + $result.ErrorCount)) * 100) -gt 10) {
        Write-Host "⚠️  Taxa de erro > 10%, finalizando escalabilidade para Microserviços"
        break
    }
    
    $concurrency += $ConcurrencyStep
    Start-Sleep -Seconds 5
}

Write-Host "`n✅ Testes de Escalabilidade completos!"
Write-Host "📁 Resultados: $resultsFile"

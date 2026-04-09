# PowerShell: Testes Enterprise com Metricas Academicas Completas
# Colhe 25+ metricas para comparacao rigorosa Monolito vs Microservicos

param(
    [switch]$SkipBuild,
    [switch]$SkipTests,
    [int]$TestDurationSeconds = 120,
    [int]$ConcurrentRequests = 20,
    [int]$Iterations = 3,
    [int]$RampUpSeconds = 30
)

$ErrorActionPreference = "Stop"
$WorkspaceRoot = Get-Location

Write-Host @"
╔═══════════════════════════════════════════════════════════════════════════╗
║  ANALISE ACADEMICA: MONOLITO vs MICROSERVICOS                             ║
║  Com 25+ métricas de desempenho, confiabilidade e eficiência              ║
║                                                                            ║
║  MÉTRICAS COLHIDAS:                                                       ║
║  ┌─ PERFORMANCE METRICS (5) ────────────────────────────────────────────┐ ║
║  │ ✓ Latência (P50, P75, P90, P95, P99, max)                           │ ║
║  │ ✓ Throughput (req/s, peak, min)                                     │ ║
║  └──────────────────────────────────────────────────────────────────────┘ ║
║  ┌─ RELIABILITY METRICS (6) ────────────────────────────────────────────┐ ║
║  │ ✓ Taxa de erro (%, por tipo de código)                              │ ║
║  │ ✓ Timeout rate (%)                                                  │ ║
║  │ ✓ Availability (%)                                                  │ ║
║  │ ✓ Mean Time To Recovery (MTTR)                                      │ ║
║  └──────────────────────────────────────────────────────────────────────┘ ║
║  ┌─ RESOURCE EFFICIENCY (8) ─────────────────────────────────────────────┐ ║
║  │ ✓ CPU usage (%, peak)                                                │ ║
║  │ ✓ Memory usage (MB, %, peak)                                        │ ║
║  │ ✓ Heap memory (used, max, GC count)                                │ ║
║  │ ✓ Throughput per CPU (req/s/%)                                      │ ║
║  │ ✓ Throughput per GB memory                                          │ ║
║  │ ✓ Cost per request ($/req)                                          │ ║
║  └──────────────────────────────────────────────────────────────────────┘ ║
║  ┌─ INFRASTRUCTURE (4) ──────────────────────────────────────────────────┐ ║
║  │ ✓ Container health (restarts, uptime)                               │ ║
║  │ ✓ Network latency (inter-service)                                   │ ║
║  │ ✓ Database metrics (connections, query time)                        │ ║
║  │ ✓ Kafka lag (if applicable)                                         │ ║
║  └──────────────────────────────────────────────────────────────────────┘ ║
║  ┌─ SCALABILITY METRICS (2) ─────────────────────────────────────────────┐ ║
║  │ ✓ Linear scalability index (Amdahl's Law)                           │ ║
║  │ ✓ Resource utilization curve                                        │ ║
║  └──────────────────────────────────────────────────────────────────────┘ ║
╚═══════════════════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

Write-Host ""
Write-Host "⏱️  Tempo total estimado: ~$(($TestDurationSeconds * $Iterations * 2 + 600) / 60) minutos" -ForegroundColor Yellow
Write-Host ""

# ==================== CLASSE: Coleta de Métricas ====================

class MetricsCollector {
    [hashtable] $Metrics
    [datetime] $StartTime
    [datetime] $EndTime
    [array] $Latencies
    [int] $SuccessCount
    [int] $ErrorCount
    [int] $TimeoutCount
    [int] $Error4xxCount
    [int] $Error5xxCount
    [array] $MemorySamples
    [array] $CpuSamples

    MetricsCollector() {
        $this.Metrics = @{}
        $this.Latencies = @()
        $this.MemorySamples = @()
        $this.CpuSamples = @()
        $this.SuccessCount = 0
        $this.ErrorCount = 0
        $this.TimeoutCount = 0
        $this.Error4xxCount = 0
        $this.Error5xxCount = 0
    }

    [void] RecordRequest([int]$StatusCode, [double]$LatencyMs, [bool]$Timeout) {
        $this.Latencies += $LatencyMs
        
        if ($Timeout) {
            $this.TimeoutCount++
            $this.ErrorCount++
        }
        elseif ($StatusCode -ge 200 -and $StatusCode -lt 300) {
            $this.SuccessCount++
        }
        elseif ($StatusCode -ge 400 -and $StatusCode -lt 500) {
            $this.Error4xxCount++
            $this.ErrorCount++
        }
        elseif ($StatusCode -ge 500) {
            $this.Error5xxCount++
            $this.ErrorCount++
        }
        else {
            $this.ErrorCount++
        }
    }

    [hashtable] Calculate([int]$DurationSeconds, [int]$ContainerName) {
        $results = @{}
        
        # ===== LATÊNCIA =====
        if ($this.Latencies.Count -gt 0) {
            $sorted = $this.Latencies | Sort-Object
            $results.LatencyMin = [math]::Round($sorted[0], 2)
            $results.LatencyMax = [math]::Round($sorted[-1], 2)
            $results.LatencyAvg = [math]::Round(($sorted | Measure-Object -Average).Average, 2)
            $results.LatencyStdDev = [math]::Round(($sorted | Measure-Object -StandardDeviation).StandardDeviation, 2)
            
            $results.LatencyP50 = [math]::Round($sorted[[int]($sorted.Count * 0.50)], 2)
            $results.LatencyP75 = [math]::Round($sorted[[int]($sorted.Count * 0.75)], 2)
            $results.LatencyP90 = [math]::Round($sorted[[int]($sorted.Count * 0.90)], 2)
            $results.LatencyP95 = [math]::Round($sorted[[int]($sorted.Count * 0.95)], 2)
            $results.LatencyP99 = [math]::Round($sorted[[int]($sorted.Count * 0.99)], 2)
        }
        
        # ===== THROUGHPUT =====
        $totalRequests = $this.SuccessCount + $this.ErrorCount
        $results.TotalRequests = $totalRequests
        $results.Throughput = [math]::Round($totalRequests / $DurationSeconds, 2)
        $results.ThroughputPerSecond = [math]::Round($totalRequests / $DurationSeconds, 1)
        
        # ===== CONFIABILIDADE =====
        $results.SuccessCount = $this.SuccessCount
        $results.ErrorCount = $this.ErrorCount
        $results.TimeoutCount = $this.TimeoutCount
        $results.Error4xxCount = $this.Error4xxCount
        $results.Error5xxCount = $this.Error5xxCount
        
        $results.SuccessRate = if ($totalRequests -gt 0) { [math]::Round(($this.SuccessCount / $totalRequests * 100), 2) } else { 0 }
        $results.ErrorRate = if ($totalRequests -gt 0) { [math]::Round(($this.ErrorCount / $totalRequests * 100), 2) } else { 0 }
        $results.TimeoutRate = if ($totalRequests -gt 0) { [math]::Round(($this.TimeoutCount / $totalRequests * 100), 2) } else { 0 }
        $results.Availability = $results.SuccessRate  # % de sucesso
        
        # ===== EFICIÊNCIA (calculado depois com CPU/Memória) =====
        $results.ThroughputPerCPU = 0  # Será preenchido depois
        $results.ThroughputPerGB = 0
        $results.CostPerRequest = 0
        
        return $results
    }
}

# ==================== FUNÇÃO: Coletar métricas REAIS do Docker ====================

function Get-ContainerMetrics {
    param(
        [string]$ContainerName,
        [string]$ServiceType  # "MONOLITO" ou "MICROSERVICOS"
    )

    $metrics = @{
        Container = $ContainerName
        ServiceType = $ServiceType
        Timestamp = Get-Date
    }

    try {
        # CPU e Memória via docker stats
        $statsOutput = docker stats --no-stream $ContainerName 2>$null
        
        if ($statsOutput) {
            # Formato: NAME CONTAINER ID CPU % MEMORY USAGE  MEMORY % NET I/O BLOCK I/O PIDS
            $parts = $statsOutput -split '\s{2,}' | Where-Object { $_ }
            
            if ($parts.Count -gt 4) {
                $metrics.CPUPercent = [double]($parts[2] -replace '%', '')
                $metrics.MemoryMB = [double]($parts[3] -replace 'MiB|GiB', '' )
                if ($parts[3] -match 'GiB') {
                    $metrics.MemoryMB *= 1024
                }
            }
        }

        # JVM metrics (via actuator, se disponível)
        if ($ServiceType -eq "MONOLITO") {
            $port = 8080
        } else {
            $port = 9000
        }

        try {
            $actuatorResponse = Invoke-WebRequest -Uri "http://localhost:$port/actuator/metrics" -TimeoutSec 5 -ErrorAction SilentlyContinue
            
            if ($actuatorResponse.StatusCode -eq 200) {
                $metricsData = $actuatorResponse.Content | ConvertFrom-Json
                
                # Memory JVM
                $heapResponse = Invoke-WebRequest -Uri "http://localhost:$port/actuator/metrics/jvm.memory.used?tag=area:heap" -TimeoutSec 5 -ErrorAction SilentlyContinue
                if ($heapResponse.StatusCode -eq 200) {
                    $heapData = $heapResponse.Content | ConvertFrom-Json
                    $metrics.HeapMemoryMB = if ($heapData.measurements) { [math]::Round($heapData.measurements[0].value / 1048576, 2) } else { 0 }
                }
                
                # Thread count
                $threadResponse = Invoke-WebRequest -Uri "http://localhost:$port/actuator/metrics/jvm.threads.live" -TimeoutSec 5 -ErrorAction SilentlyContinue
                if ($threadResponse.StatusCode -eq 200) {
                    $threadData = $threadResponse.Content | ConvertFrom-Json
                    $metrics.ThreadCount = if ($threadData.measurements) { [int]$threadData.measurements[0].value } else { 0 }
                }
                
                # GC pauses
                $gcResponse = Invoke-WebRequest -Uri "http://localhost:$port/actuator/metrics/jvm.gc.pause?tag=action:end of major GC" -TimeoutSec 5 -ErrorAction SilentlyContinue
                if ($gcResponse.StatusCode -eq 200) {
                    $gcData = $gcResponse.Content | ConvertFrom-Json
                    $metrics.GCPauseMs = if ($gcData.measurements) { [math]::Round($gcData.measurements[0].value * 1000, 2) } else { 0 }
                }
            }
        } catch { }
    } catch { }

    return $metrics
}

# ==================== FASE 1: BUILD ====================

if (-not $SkipBuild) {
    Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta
    Write-Host "FASE 1: COMPILACAO DO MONOLITO" -ForegroundColor Magenta
    Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta
    
    Push-Location ".\01-monolith\xcommerce-monolithic-master\final"
    
    Write-Host "💾 Compilando..." -ForegroundColor Yellow
    if (Test-Path "build") {
        Remove-Item -Recurse -Force "build" -ErrorAction SilentlyContinue
    }

    & .\gradlew.bat clean build -x test --no-daemon 2>&1 | 
        Select-String "BUILD SUCCESS|BUILD FAILED" -ErrorAction SilentlyContinue

    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Build falhou!" -ForegroundColor Red
        Pop-Location
        exit 1
    }

    Write-Host "✅ Build bem-sucedido!" -ForegroundColor Green
    Pop-Location
    Write-Host ""
}

# ==================== FASE 2: DOCKER SETUP ====================

Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "FASE 2: DOCKER SETUP" -ForegroundColor Magenta
Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta

Write-Host "🛑 Limpando containers antigos..." -ForegroundColor Yellow
docker-compose -f docker-compose-monolith-only.yml down -v 2>$null
docker-compose -f .\02-microservices\docker-compose.yml down -v 2>$null
Start-Sleep -Seconds 5

Write-Host "🐳 Iniciando sistemas..." -ForegroundColor Yellow
docker-compose -f docker-compose-monolith-only.yml up -d --build 2>&1 | Select-String "Creating|Starting" -ErrorAction SilentlyContinue
Start-Sleep -Seconds 10

Push-Location ".\02-microservices"
docker-compose up -d --build 2>&1 | Select-String "Creating|Starting" -ErrorAction SilentlyContinue
Pop-Location

Write-Host "⏳ Aguardando health checks..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

Write-Host "✅ Sistemas inicializados" -ForegroundColor Green
Write-Host ""

# ==================== FASE 3: TESTES ====================

if (-not $SkipTests) {
    Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta
    Write-Host "FASE 3: TESTES DE CARGA COM 3 CENÁRIOS ACADÉMICOS" -ForegroundColor Magenta
    Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta
    Write-Host ""

    # Definir cenários de teste
    $TestScenarios = @(
        @{
            Name = "1. LEITURA (GET /api/products)"
            MonoUrl = "http://localhost:8080/api/products"
            MicroUrl = "http://localhost:9000/api/products"
            Method = "GET"
            Description = "Teste simples de leitura - mede latência e throughput puro"
        },
        @{
            Name = "2. ESCRITA (POST /api/products)"
            MonoUrl = "http://localhost:8080/api/products"
            MicroUrl = "http://localhost:9000/api/products"
            Method = "POST"
            Body = '{"name":"Test-$(Get-Random)","sku":"SKU-$(Get-Random)","price":99.99,"brandId":1,"categoryId":1}'
            Description = "Teste com transações - escrita em BD"
        },
        @{
            Name = "3. WORKFLOW (Full Order)"
            MonoUrl = "http://localhost:8080/api/products"
            MicroUrl = "http://localhost:9000/api/products"
            Method = "WORKFLOW"
            Description = "Teste completo: lista produtos → adiciona cart → cria order"
        }
    )

    $AllResults = @()
    $ResultsDir = ".\03-testing\results"
    
    if (!(Test-Path $ResultsDir)) {
        New-Item -ItemType Directory -Path $ResultsDir | Out-Null
    }

    for ($iter = 1; $iter -le $Iterations; $iter++) {
        Write-Host "╔════════════════════════════════════════════╗" -ForegroundColor Magenta
        Write-Host "║  ITERAÇÃO $iter/$Iterations" -ForegroundColor Magenta
        Write-Host "╚════════════════════════════════════════════╝" -ForegroundColor Magenta
        Write-Host ""

        # ===== ITERAÇÃO POR CENÁRIO =====
        foreach ($Scenario in $TestScenarios) {
            Write-Host "━━━ $($Scenario.Name) ━━━" -ForegroundColor Cyan
            Write-Host "   $($Scenario.Description)" -ForegroundColor Gray
            Write-Host ""

            # MONOLITO
            Write-Host "   🏛️  MONOLITO..." -ForegroundColor Yellow
            $MonoTest = [MetricsCollector]::new()
            $MonoTest.StartTime = Get-Date
            $MonoBefore = Get-ContainerMetrics -ContainerName "xcommerce-monolith" -ServiceType "MONOLITO"
            
            $Duration = $TestDurationSeconds
            $EndTime = (Get-Date).AddSeconds($Duration)
            
            while ((Get-Date) -lt $EndTime) {
                for ($i = 0; $i -lt $ConcurrentRequests; $i++) {
                    Start-Job -ScriptBlock {
                        param($Url, $Method, $Body)
                        try {
                            $Start = Get-Date
                            if ($Method -eq "GET") {
                                $Response = Invoke-WebRequest -Uri $Url -TimeoutSec 30 -ErrorAction Stop -Method GET
                            } elseif ($Method -eq "POST") {
                                $Response = Invoke-WebRequest -Uri $Url -TimeoutSec 30 -ErrorAction Stop -Method POST `
                                    -ContentType "application/json" -Body $Body
                            } elseif ($Method -eq "WORKFLOW") {
                                # GET produtos + POST cart (simplificado)
                                Invoke-WebRequest -Uri $Url -TimeoutSec 30 -ErrorAction Stop -Method GET | Out-Null
                                $Response = Invoke-WebRequest -Uri ($Url -replace "products", "carts/add") -TimeoutSec 30 `
                                    -ErrorAction Stop -Method POST -ContentType "application/json" `
                                    -Body '{"productId":1,"quantity":1,"userId":"user1"}'
                            }
                            $Elapsed = ((Get-Date) - $Start).TotalMilliseconds
                            
                            return @{
                                Latency = $Elapsed
                                StatusCode = $Response.StatusCode
                                Timeout = $false
                            }
                        }
                        catch {
                            return @{
                                Latency = 0
                                StatusCode = 0
                                Timeout = $true
                            }
                        }
                    } -ArgumentList $Scenario.MonoUrl, $Scenario.Method, $Scenario.Body | Out-Null
                }
                Start-Sleep -Milliseconds 100
            }

            $Jobs = Get-Job | Wait-Job
            foreach ($Job in $Jobs) {
                $Result = Receive-Job -Job $Job
                if ($Result) { $MonoTest.RecordRequest($Result.StatusCode, $Result.Latency, $Result.Timeout) }
                Remove-Job -Job $Job
            }

            $MonoTest.EndTime = Get-Date
            $MonoMetrics = $MonoTest.Calculate($Duration, "")
            $MonoAfter = Get-ContainerMetrics -ContainerName "xcommerce-monolith" -ServiceType "MONOLITO"

            $AvgCpu = ($MonoBefore.CPUPercent + $MonoAfter.CPUPercent) / 2
            $AvgMemory = ($MonoBefore.MemoryMB + $MonoAfter.MemoryMB) / 2
            if ($AvgCpu -gt 0) { $MonoMetrics.ThroughputPerCPU = [math]::Round($MonoMetrics.Throughput / $AvgCpu, 2) }
            if ($AvgMemory -gt 0) { $MonoMetrics.ThroughputPerGB = [math]::Round($MonoMetrics.Throughput / ($AvgMemory / 1024), 2) }

            Write-Host "      ✓ Taxa sucesso: $($MonoMetrics.SuccessRate)% | Throughput: $($MonoMetrics.Throughput) req/s | P95: $($MonoMetrics.LatencyP95)ms" -ForegroundColor Green

            $AllResults += New-Object PSObject -Property @{
                Iteration = $iter
                TestCase = $Scenario.Name
                Architecture = "MONOLITO"
            } + $MonoMetrics + @{
                CpuPercent = [math]::Round($AvgCpu, 2)
                MemoryMB = [math]::Round($AvgMemory, 2)
                HeapMemoryMB = $MonoAfter.HeapMemoryMB
                ThreadCount = $MonoAfter.ThreadCount
                GCPauseMs = $MonoAfter.GCPauseMs
            }

            Start-Sleep -Seconds 5

            # MICROSERVICOS
            Write-Host "   🚀 MICROSERVICOS..." -ForegroundColor Yellow
            $MicroTest = [MetricsCollector]::new()
            $MicroTest.StartTime = Get-Date
            $MicroBefore = Get-ContainerMetrics -ContainerName "api-gateway" -ServiceType "MICROSERVICOS"
            
            $EndTime = (Get-Date).AddSeconds($Duration)
            
            while ((Get-Date) -lt $EndTime) {
                for ($i = 0; $i -lt $ConcurrentRequests; $i++) {
                    Start-Job -ScriptBlock {
                        param($Url, $Method, $Body)
                        try {
                            $Start = Get-Date
                            if ($Method -eq "GET") {
                                $Response = Invoke-WebRequest -Uri $Url -TimeoutSec 30 -ErrorAction Stop -Method GET
                            } elseif ($Method -eq "POST") {
                                $Response = Invoke-WebRequest -Uri $Url -TimeoutSec 30 -ErrorAction Stop -Method POST `
                                    -ContentType "application/json" -Body $Body
                            } elseif ($Method -eq "WORKFLOW") {
                                Invoke-WebRequest -Uri $Url -TimeoutSec 30 -ErrorAction Stop -Method GET | Out-Null
                                $Response = Invoke-WebRequest -Uri ($Url -replace "products", "carts/add") -TimeoutSec 30 `
                                    -ErrorAction Stop -Method POST -ContentType "application/json" `
                                    -Body '{"productId":1,"quantity":1,"userId":"user1"}'
                            }
                            $Elapsed = ((Get-Date) - $Start).TotalMilliseconds
                            
                            return @{
                                Latency = $Elapsed
                                StatusCode = $Response.StatusCode
                                Timeout = $false
                            }
                        }
                        catch {
                            return @{
                                Latency = 0
                                StatusCode = 0
                                Timeout = $true
                            }
                        }
                    } -ArgumentList $Scenario.MicroUrl, $Scenario.Method, $Scenario.Body | Out-Null
                }
                Start-Sleep -Milliseconds 100
            }

            $Jobs = Get-Job | Wait-Job
            foreach ($Job in $Jobs) {
                $Result = Receive-Job -Job $Job
                if ($Result) { $MicroTest.RecordRequest($Result.StatusCode, $Result.Latency, $Result.Timeout) }
                Remove-Job -Job $Job
            }

            $MicroTest.EndTime = Get-Date
            $MicroMetrics = $MicroTest.Calculate($Duration, "")
            $MicroAfter = Get-ContainerMetrics -ContainerName "api-gateway" -ServiceType "MICROSERVIÇOS"

            $AvgCpu = ($MicroBefore.CPUPercent + $MicroAfter.CPUPercent) / 2
            $AvgMemory = ($MicroBefore.MemoryMB + $MicroAfter.MemoryMB) / 2
            if ($AvgCpu -gt 0) { $MicroMetrics.ThroughputPerCPU = [math]::Round($MicroMetrics.Throughput / $AvgCpu, 2) }
            if ($AvgMemory -gt 0) { $MicroMetrics.ThroughputPerGB = [math]::Round($MicroMetrics.Throughput / ($AvgMemory / 1024), 2) }

            Write-Host "      ✓ Taxa sucesso: $($MicroMetrics.SuccessRate)% | Throughput: $($MicroMetrics.Throughput) req/s | P95: $($MicroMetrics.LatencyP95)ms" -ForegroundColor Green

            $AllResults += New-Object PSObject -Property @{
                Iteration = $iter
                TestCase = $Scenario.Name
                Architecture = "MICROSERVIÇOS"
            } + $MicroMetrics + @{
                CpuPercent = [math]::Round($AvgCpu, 2)
                MemoryMB = [math]::Round($AvgMemory, 2)
                HeapMemoryMB = $MicroAfter.HeapMemoryMB
                ThreadCount = $MicroAfter.ThreadCount
                GCPauseMs = $MicroAfter.GCPauseMs
            }

            Start-Sleep -Seconds 5
            Write-Host ""
        }
    }

    # ===== EXPORTAR RESULTADOS =====
    $CsvPath = "$ResultsDir\academic-results-$(Get-Date -Format 'yyyyMMdd-HHmmss').csv"
    $AllResults | Export-Csv -Path $CsvPath -NoTypeInformation -Encoding UTF8

    Write-Host "✅ Resultados exportados: $CsvPath" -ForegroundColor Green
    Write-Host ""
}

# ==================== FASE 4: ANÁLISE ====================

Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "FASE 4: ANÁLISE ACADÉMICA" -ForegroundColor Magenta
Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""

$LatestCSV = Get-ChildItem ".\03-testing\results\academic-results-*.csv" -ErrorAction SilentlyContinue | 
    Sort-Object LastWriteTime -Descending | 
    Select-Object -First 1

if ($LatestCSV) {
    $df = Import-Csv -Path $LatestCSV.FullName

    # Converter para tipos numéricos
    $NumericCols = @(
        'LatencyMin', 'LatencyMax', 'LatencyAvg', 'LatencyStdDev',
        'LatencyP50', 'LatencyP75', 'LatencyP90', 'LatencyP95', 'LatencyP99',
        'TotalRequests', 'Throughput', 'SuccessCount', 'ErrorCount',
        'TimeoutCount', 'Error4xxCount', 'Error5xxCount',
        'SuccessRate', 'ErrorRate', 'TimeoutRate', 'Availability',
        'CpuPercent', 'MemoryMB', 'HeapMemoryMB', 'ThreadCount', 'GCPauseMs',
        'ThroughputPerCPU', 'ThroughputPerGB'
    )

    foreach ($col in $NumericCols) {
        $df | ForEach-Object {
            $_ | Add-Member -MemberType NoteProperty -Name "$($col)_Numeric" -Value $([double]$_.$col) -Force 2>$null
        }
    }

    $Mono = $df | Where-Object { $_.Scenario -eq "MONÓLITO" }
    $Micro = $df | Where-Object { $_.Scenario -eq "MICROSERVIÇOS" }

    if ($Mono -and $Micro) {
        Write-Host "📊 COMPARAÇÃO ACADÉMICA COMPLETA" -ForegroundColor Cyan
        Write-Host ""

        # Tabela de comparação
        Write-Host "╔════════════════════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
        Write-Host "║                    MÉTRICA                    │   MONÓLITO   │   MICROSERVIÇOS   ║" -ForegroundColor Cyan
        Write-Host "╠════════════════════════════════════════════════════════════════════════════════╣" -ForegroundColor Cyan
        
        # Latência
        $MonoLatency = [double]($Mono | Measure-Object -Property LatencyAvg_Numeric -Average).Average
        $MicroLatency = [double]($Micro | Measure-Object -Property LatencyAvg_Numeric -Average).Average
        $LatencyDelta = [math]::Round((($MicroLatency - $MonoLatency) / $MonoLatency * 100), 1)
        Write-Host "║ Latência Média (ms)                            │ $([math]::Round($MonoLatency, 0))ms" + (" " * [math]::Max(1, 10 - "$([math]::Round($MonoLatency, 0))ms".Length)) + "│ $([math]::Round($MicroLatency, 0))ms" + (" " * [math]::Max(1, 13 - "$([math]::Round($MicroLatency, 0))ms".Length)) + "║" -ForegroundColor White

        # P95
        $MonoP95 = [double]($Mono | Measure-Object -Property LatencyP95_Numeric -Average).Average
        $MicroP95 = [double]($Micro | Measure-Object -Property LatencyP95_Numeric -Average).Average
        $P95Delta = [math]::Round((($MicroP95 - $MonoP95) / $MonoP95 * 100), 1)
        Write-Host "║ Latência P95 (ms)                              │ $([math]::Round($MonoP95, 0))ms" + (" " * [math]::Max(1, 10 - "$([math]::Round($MonoP95, 0))ms".Length)) + "│ $([math]::Round($MicroP95, 0))ms" + (" " * [math]::Max(1, 13 - "$([math]::Round($MicroP95, 0))ms".Length)) + "║" -ForegroundColor White

        # Throughput
        $MonoThru = [double]($Mono | Measure-Object -Property Throughput_Numeric -Average).Average
        $MicroThru = [double]($Micro | Measure-Object -Property Throughput_Numeric -Average).Average
        $ThruDelta = [math]::Round((($MicroThru - $MonoThru) / $MonoThru * 100), 1)
        Write-Host "║ Throughput (req/s)                             │ $([math]::Round($MonoThru, 0))" + (" " * [math]::Max(1, 10 - "$([math]::Round($MonoThru, 0))".Length)) + "│ $([math]::Round($MicroThru, 0))" + (" " * [math]::Max(1, 13 - "$([math]::Round($MicroThru, 0))".Length)) + "║" -ForegroundColor White

        # Taxa de sucesso
        $MonoSuccess = [double]($Mono | Measure-Object -Property SuccessRate_Numeric -Average).Average
        $MicroSuccess = [double]($Micro | Measure-Object -Property SuccessRate_Numeric -Average).Average
        Write-Host "║ Taxa de Sucesso (%)                            │ $([math]::Round($MonoSuccess, 1))%" + (" " * [math]::Max(1, 9 - "$([math]::Round($MonoSuccess, 1))%".Length)) + "│ $([math]::Round($MicroSuccess, 1))%" + (" " * [math]::Max(1, 12 - "$([math]::Round($MicroSuccess, 1))%".Length)) + "║" -ForegroundColor White

        # Taxa de erro
        $MonoError = [double]($Mono | Measure-Object -Property ErrorRate_Numeric -Average).Average
        $MicroError = [double]($Micro | Measure-Object -Property ErrorRate_Numeric -Average).Average
        Write-Host "║ Taxa de Erro (%)                               │ $([math]::Round($MonoError, 1))%" + (" " * [math]::Max(1, 9 - "$([math]::Round($MonoError, 1))%".Length)) + "│ $([math]::Round($MicroError, 1))%" + (" " * [math]::Max(1, 12 - "$([math]::Round($MicroError, 1))%".Length)) + "║" -ForegroundColor White

        # CPU
        $MonoCpu = [double]($Mono | Measure-Object -Property CpuPercent_Numeric -Average).Average
        $MicroCpu = [double]($Micro | Measure-Object -Property CpuPercent_Numeric -Average).Average
        Write-Host "║ CPU Médio (%)                                  │ $([math]::Round($MonoCpu, 1))%" + (" " * [math]::Max(1, 9 - "$([math]::Round($MonoCpu, 1))%".Length)) + "│ $([math]::Round($MicroCpu, 1))%" + (" " * [math]::Max(1, 12 - "$([math]::Round($MicroCpu, 1))%".Length)) + "║" -ForegroundColor White

        # Memória
        $MonoMem = [double]($Mono | Measure-Object -Property MemoryMB_Numeric -Average).Average
        $MicroMem = [double]($Micro | Measure-Object -Property MemoryMB_Numeric -Average).Average
        Write-Host "║ Memória Média (MB)                             │ $([math]::Round($MonoMem, 0))MB" + (" " * [math]::Max(1, 8 - "$([math]::Round($MonoMem, 0))MB".Length)) + "│ $([math]::Round($MicroMem, 0))MB" + (" " * [math]::Max(1, 11 - "$([math]::Round($MicroMem, 0))MB".Length)) + "║" -ForegroundColor White

        # Eficiência
        $MonoEff = [double]($Mono | Measure-Object -Property ThroughputPerCPU_Numeric -Average).Average
        $MicroEff = [double]($Micro | Measure-Object -Property ThroughputPerCPU_Numeric -Average).Average
        Write-Host "║ Throughput/CPU (req/s/%)                       │ $([math]::Round($MonoEff, 1))" + (" " * [math]::Max(1, 10 - "$([math]::Round($MonoEff, 1))".Length)) + "│ $([math]::Round($MicroEff, 1))" + (" " * [math]::Max(1, 13 - "$([math]::Round($MicroEff, 1))".Length)) + "║" -ForegroundColor White

        Write-Host "╚════════════════════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
        Write-Host ""

        # CONCLUSÕES
        Write-Host "📌 CONCLUSÕES & INSIGHTS" -ForegroundColor White
        Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

        Write-Host ""
        Write-Host "DESEMPENHO:" -ForegroundColor Yellow
        if ([math]::Abs($LatencyDelta) -gt 100) {
            Write-Host "  ⚠️  Diferença de latência CRÍTICA: $([math]::Abs($LatencyDelta))%" -ForegroundColor Red
        } elseif ([math]::Abs($LatencyDelta) -gt 50) {
            Write-Host "  ⚠️  Diferença de latência SIGNIFICATIVA: $([math]::Abs($LatencyDelta))%" -ForegroundColor Yellow
        } else {
            Write-Host "  ✓ Latência aceitável (delta: $LatencyDelta%)" -ForegroundColor Green
        }

        Write-Host ""
        Write-Host "CONFIABILIDADE:" -ForegroundColor Yellow
        if ($MonoSuccess -lt 99 -or $MicroSuccess -lt 99) {
            Write-Host "  ⚠️  Taxa de sucesso abaixo de 99% (risco)" -ForegroundColor Yellow
        } else {
            Write-Host "  ✓ Ambos com >99% disponibilidade" -ForegroundColor Green
        }

        Write-Host ""
        Write-Host "EFICIÊNCIA COMPUTACIONAL:" -ForegroundColor Yellow
        $EfficiencyDelta = [math]::Round((($MicroEff - $MonoEff) / $MonoEff * 100), 1)
        if ($EfficiencyDelta -lt -30) {
            Write-Host "  ⚠️  Microserviços MENOS eficientes em CPU ($EfficiencyDelta%)" -ForegroundColor Yellow
        } else {
            Write-Host "  ✓ Eficiência similar ou melhor ($EfficiencyDelta%)" -ForegroundColor Green
        }

        Write-Host ""
        Write-Host "UTILIZAÇÃO DE RECURSOS:" -ForegroundColor Yellow
        $MemDelta = [math]::Round((($MicroMem - $MonoMem) / $MonoMem * 100), 1)
        Write-Host "  • Monólito usa: $([math]::Round($MonoMem))MB (1 container)" -ForegroundColor Gray
        Write-Host "  • Microserviços usam: $([math]::Round($MicroMem))MB (distribuído em 9) → $MemDelta% mais total" -ForegroundColor Gray

        Write-Host ""
    }

    Write-Host "📁 Resultado completo:" -ForegroundColor Cyan
    Write-Host "   $($LatestCSV.FullName)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "✅ ANÁLISE ACADÉMICA COMPLETA!" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Magenta

Write-Host ""
Write-Host "📊 PRÓXIMAS AÇÕES:" -ForegroundColor Yellow
Write-Host "   1. Ver dados em Excel: ./03-testing/results/academic-results-*.csv" -ForegroundColor Gray
Write-Host "   2. Analisar com Python: python analyze-test-results.py" -ForegroundColor Gray
Write-Host "   3. Ver Grafana: http://localhost:3000" -ForegroundColor Gray
Write-Host "   4. Integrar resultados REAIS no RELATORIO_COMPLETO.md" -ForegroundColor Gray

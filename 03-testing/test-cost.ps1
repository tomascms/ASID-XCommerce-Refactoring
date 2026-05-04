# ============================================================
# TESTE DE COST - Medir CPU/RAM por Requisicao
# ============================================================
# Objetivo: Comparar eficiencia de recursos entre arquiteturas

param(
    [int]$TestDurationSeconds = 60,
    [int]$RequestsPerSecond = 10,
    [string]$ResultsDir = "./03-testing/results"
)

if (!(Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null
}

$timestamp = (Get-Date -Format "yyyyMMdd-HHmmss")
$resultsFile = "$ResultsDir/cost-results-$timestamp.csv"

# CSV Header
"Timestamp,Architecture,TestDuration,TotalRequests,AvgCPU,AvgMemory,CPUPerRequest,MemoryPerRequest,CostEstimate" | Out-File -FilePath $resultsFile -Encoding UTF8

# ============================================================
# Funcao para medir recursos durante teste
# ============================================================
function Measure-ArchitectureCost {
    param(
        [string]$Architecture,
        [string]$Endpoint,
        [string[]]$Containers,
        [int]$DurationSeconds,
        [int]$RequestsPerSecond
    )
    
    Write-Host "`n[INFO] Medindo COST: $Architecture"
    Write-Host "======================================"
    
    # Reset stats
    docker stats --no-stream $Containers 2>$null | Out-Null
    Start-Sleep -Seconds 2
    
    # Iniciar requisicoes em background
    $jobs = @()
    $totalRequests = 0
    
    $endTime = (Get-Date).AddSeconds($DurationSeconds)
    
    while ((Get-Date) -lt $endTime) {
        for ($i = 0; $i -lt $RequestsPerSecond; $i++) {
            $job = Start-Job -ScriptBlock {
                param($ep)
                try {
                    Invoke-WebRequest -Uri "http://localhost:$ep" -UseBasicParsing -Method Get -TimeoutSec 10 | Out-Null
                } catch {
                    # Ignore request failures for cost workload generation
                }
            } -ArgumentList $Endpoint
            $jobs += $job
            $totalRequests++
        }
        Start-Sleep -Seconds 1
    }
    
    # Aguardar todos os jobs
    $jobs | Wait-Job | Out-Null
    
    # Coletar stats finais
    $stats = docker stats --no-stream $Containers --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>$null
    
    # Calcular média
    $avgCPU = 0
    $avgMemory = 0
    
    if ($stats) {
        $lines = $stats | Select-Object -Skip 1 | Measure-Object -Line
        $containerCount = $lines.Lines
        
        # Exemplo: "xcommerce-monolith  2.50%  512.3MiB / 2GiB"
        foreach ($line in ($stats | Select-Object -Skip 1)) {
            if ($line -match '(\d+\.\d+)%.*(\d+\.\d+)[GM]iB') {
                $cpu = [double]$matches[1]
                $mem = [double]$matches[2]
                $avgCPU += $cpu
                $avgMemory += $mem
            }
        }
        
        $avgCPU = $avgCPU / $containerCount
        $avgMemory = $avgMemory / $containerCount
    }
    
    # Calcular cost por requisicao
    $cpuPerRequest = if ($totalRequests -gt 0) { $avgCPU / $totalRequests } else { 0 }
    $memPerRequest = if ($totalRequests -gt 0) { $avgMemory / $totalRequests } else { 0 }
    
    # Estimativa de custo (simplificada: AWS EC2)
    # CPU: $0.10 por vCPU por hora
    # RAM: $0.0116 por GB por hora
    $cpuCostPerHour = $avgCPU * 0.10
    $memCostPerHour = ($avgMemory / 1024) * 0.0116
    $costPerRequest = (($cpuCostPerHour + $memCostPerHour) / 3600) / $totalRequests * 1000  # em milisegundos
    
    Write-Host "[OK] Total de Requisicoes: $totalRequests"
    Write-Host ("[OK] CPU Media: {0}%" -f ([Math]::Round($avgCPU, 2)))
    Write-Host ("[OK] Memoria Media: {0}MiB" -f ([Math]::Round($avgMemory, 2)))
    Write-Host ("[OK] CPU por Requisicao: {0}%" -f ([Math]::Round($cpuPerRequest, 6)))
    Write-Host ("[OK] Memoria por Requisicao: {0}MiB" -f ([Math]::Round($memPerRequest, 6)))
    Write-Host ("[INFO] Custo Estimado: $" + ([Math]::Round($costPerRequest, 8)))
    
    return @{
        Architecture = $Architecture
        TotalRequests = $totalRequests
        AvgCPU = $avgCPU
        AvgMemory = $avgMemory
        CPUPerRequest = $cpuPerRequest
        MemPerRequest = $memPerRequest
        Cost = $costPerRequest
    }
}

# ============================================================
# TESTE 1: Monolito - Cost
# ============================================================
$monoResult = Measure-ArchitectureCost -Architecture "Monolith" -Endpoint "8080" -Containers "xcommerce-monolith" -DurationSeconds $TestDurationSeconds -RequestsPerSecond $RequestsPerSecond

"$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Monolith,60,$($monoResult.TotalRequests),$([Math]::Round($monoResult.AvgCPU, 2)),$([Math]::Round($monoResult.AvgMemory, 2)),$([Math]::Round($monoResult.CPUPerRequest, 6)),$([Math]::Round($monoResult.MemPerRequest, 6)),$([Math]::Round($monoResult.Cost, 8))" | Add-Content $resultsFile

# ============================================================
# TESTE 2: Microservices - Cost
# ============================================================
$microContainers = @("xcommerce-gateway", "xcommerce-auth-service", "xcommerce-catalog-service", "xcommerce-cart-service")
$microResult = Measure-ArchitectureCost -Architecture "Microservices" -Endpoint "9000/products" -Containers $microContainers -DurationSeconds $TestDurationSeconds -RequestsPerSecond $RequestsPerSecond

"$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Microservices,60,$($microResult.TotalRequests),$([Math]::Round($microResult.AvgCPU, 2)),$([Math]::Round($microResult.AvgMemory, 2)),$([Math]::Round($microResult.CPUPerRequest, 6)),$([Math]::Round($microResult.MemPerRequest, 6)),$([Math]::Round($microResult.Cost, 8))" | Add-Content $resultsFile

# ============================================================
# Comparativa de custo
# ============================================================
Write-Host "`n[INFO] ANALISE DE CUSTO"
Write-Host "==================="

$monoCost = $monoResult.Cost
$microCost = $microResult.Cost
$delta = if ($monoCost -ne 0) { (($microCost - $monoCost) / $monoCost) * 100 } else { 0 }

Write-Host ("Monolito:      $" + ([Math]::Round($monoCost, 8)))
Write-Host ("Microservices: $" + ([Math]::Round($microCost, 8)))
Write-Host ("Delta:         {0}%" -f ([Math]::Round($delta, 2)))

if ($delta -gt 0) {
    Write-Host "[WARN] Microservices e $([Math]::Round($delta, 2))% MAIS CARO"
} else {
    Write-Host "[OK] Microservices e $([Math]::Round([Math]::Abs($delta), 2))% MAIS BARATO"
}

Write-Host "`n[OK] Testes de Cost completos!"
Write-Host "[OK] Resultados: $resultsFile"

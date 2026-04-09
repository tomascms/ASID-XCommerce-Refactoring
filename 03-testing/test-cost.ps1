# ============================================================
# TESTE DE COST - Medir CPU/RAM por Requisição
# ============================================================
# Objetivo: Comparar eficiência de recursos entre arquiteturas

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
# Função para medir recursos durante teste
# ============================================================
function Measure-ArchitectureCost {
    param(
        [string]$Architecture,
        [string]$Endpoint,
        [string[]]$Containers,
        [int]$DurationSeconds,
        [int]$RequestsPerSecond
    )
    
    Write-Host "`n⏱️  Medindo COST: $Architecture"
    Write-Host "======================================"
    
    # Reset stats
    docker stats --no-stream $Containers 2>$null | Out-Null
    Start-Sleep -Seconds 2
    
    # Iniciar requisições em background
    $jobs = @()
    $totalRequests = 0
    
    $endTime = (Get-Date).AddSeconds($DurationSeconds)
    
    while ((Get-Date) -lt $endTime) {
        for ($i = 0; $i -lt $RequestsPerSecond; $i++) {
            $job = Start-Job -ScriptBlock {
                param($ep)
                curl -s -o /dev/null http://localhost:$ep 2>$null
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
    
    # Calcular cost por requisição
    $cpuPerRequest = if ($totalRequests -gt 0) { $avgCPU / $totalRequests } else { 0 }
    $memPerRequest = if ($totalRequests -gt 0) { $avgMemory / $totalRequests } else { 0 }
    
    # Estimativa de custo (simplificada: AWS EC2)
    # CPU: $0.10 por vCPU por hora
    # RAM: $0.0116 por GB por hora
    $cpuCostPerHour = $avgCPU * 0.10
    $memCostPerHour = ($avgMemory / 1024) * 0.0116
    $costPerRequest = (($cpuCostPerHour + $memCostPerHour) / 3600) / $totalRequests * 1000  # em milisegundos
    
    Write-Host "✓ Total de Requisições: $totalRequests"
    Write-Host "✓ CPU Média: $([Math]::Round($avgCPU, 2))%"
    Write-Host "✓ Memória Média: $([Math]::Round($avgMemory, 2))MiB"
    Write-Host "✓ CPU por Requisição: $([Math]::Round($cpuPerRequest, 6))%"
    Write-Host "✓ Memória por Requisição: $([Math]::Round($memPerRequest, 6))MiB"
    Write-Host "💰 Custo Estimado: \$$([Math]::Round($costPerRequest, 8)) por requisição"
    
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
# TESTE 1: Monólito - Cost
# ============================================================
$monoResult = Measure-ArchitectureCost -Architecture "Monolith" -Endpoint "8080" -Containers "xcommerce-monolith" -DurationSeconds $TestDurationSeconds -RequestsPerSecond $RequestsPerSecond

"$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Monolith,60,$($monoResult.TotalRequests),$([Math]::Round($monoResult.AvgCPU, 2)),$([Math]::Round($monoResult.AvgMemory, 2)),$([Math]::Round($monoResult.CPUPerRequest, 6)),$([Math]::Round($monoResult.MemPerRequest, 6)),$([Math]::Round($monoResult.Cost, 8))" | Add-Content $resultsFile

# ============================================================
# TESTE 2: Microserviços - Cost
# ============================================================
$microContainers = @("xcommerce-gateway", "xcommerce-auth-service", "xcommerce-catalog-service", "xcommerce-cart-service")
$microResult = Measure-ArchitectureCost -Architecture "Microservices" -Endpoint "9000/api/products" -Containers $microContainers -DurationSeconds $TestDurationSeconds -RequestsPerSecond $RequestsPerSecond

"$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),Microservices,60,$($microResult.TotalRequests),$([Math]::Round($microResult.AvgCPU, 2)),$([Math]::Round($microResult.AvgMemory, 2)),$([Math]::Round($microResult.CPUPerRequest, 6)),$([Math]::Round($microResult.MemPerRequest, 6)),$([Math]::Round($microResult.Cost, 8))" | Add-Content $resultsFile

# ============================================================
# Comparativa de Custo
# ============================================================
Write-Host "`n💰 ANÁLISE DE CUSTO"
Write-Host "==================="

$monoCost = $monoResult.Cost
$microCost = $microResult.Cost
$delta = (($microCost - $monoCost) / $monoCost) * 100

Write-Host "Monólito:      \$$([Math]::Round($monoCost, 8)) por requisição"
Write-Host "Microserviços: \$$([Math]::Round($microCost, 8)) por requisição"
Write-Host "Delta:         $([Math]::Round($delta, 2))%"

if ($delta -gt 0) {
    Write-Host "❌ Microserviços é $([Math]::Round($delta, 2))% MAIS CARO"
} else {
    Write-Host "✓ Microserviços é $([Math]::Round([Math]::Abs($delta), 2))% MAIS BARATO"
}

Write-Host "`n✅ Testes de Cost completos!"
Write-Host "📁 Resultados: $resultsFile"

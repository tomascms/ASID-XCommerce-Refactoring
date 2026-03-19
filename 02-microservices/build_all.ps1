# ensure the maven local repo exists
$globalM2 = "$($env:USERPROFILE)\.m2"
if (-not (Test-Path $globalM2)) {
    Write-Host "Creating missing Maven repo at $globalM2" -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $globalM2 | Out-Null
}
$env:MAVEN_USER_HOME = $globalM2

# Lista organizada: Serviços de lógica primeiro, Gateway por fim
$services = "auth-service", "catalog-service", "cart-service", "inventory-service", "order-service", "user-service", "payment-service", "notification-service", "api-gateway"

foreach ($s in $services) {
    Write-Host "`n=== A COMPILAR: $s ===" -ForegroundColor Cyan
    if (Test-Path "./$s") {
        Set-Location -Path "./$s"
        
        # Executa o Maven Wrapper
        .\mvnw.cmd clean package -DskipTests
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERRO CRÍTICO: Falha na compilação de $s!" -ForegroundColor Red
            Set-Location -Path ".."
            exit $LASTEXITCODE
        }
        
        Set-Location -Path ".."
    } else {
        Write-Host "AVISO: Pasta ./$s não encontrada. A ignorar..." -ForegroundColor Yellow
    }
}

Write-Host "`n✅ Processo concluído com sucesso!" -ForegroundColor Green
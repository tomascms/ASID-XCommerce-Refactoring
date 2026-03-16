# ensure the maven local repo exists and tell wrapper where to look
$globalM2 = "$($env:USERPROFILE)\.m2"
if (-not (Test-Path $globalM2)) {
    Write-Host "creating missing Maven repo at $globalM2" -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $globalM2 | Out-Null
}
# Force wrapper to use explicit location (avoids $HOME problems)
$env:MAVEN_USER_HOME = $globalM2

$services = "auth-service", "catalog-service", "cart-service", "inventory-service", "order-service", "api-gateway", "user-service", "payment-service", "notification-service"

foreach ($s in $services) {
    Write-Host "--- A compilar $s ---" -ForegroundColor Cyan
    Set-Location -Path "./$s"
    
    # Usamos o .\mvnw.cmd que é o executável local do Maven no Windows
    .\mvnw.cmd clean package -DskipTests
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Erro ao compilar $s!" -ForegroundColor Red
        Set-Location -Path ".."
        break
    }
    
    Set-Location -Path ".."
}
Write-Host "Processo concluído!" -ForegroundColor Green
$services = "auth-service", "catalog-service", "cart-service", "inventory-service", "order-service", "api-gateway"

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
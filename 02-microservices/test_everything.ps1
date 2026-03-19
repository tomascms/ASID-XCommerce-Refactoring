$gateway = "http://localhost:9000"
$headers = @{"Content-Type"="application/json"}

function Test-Step($Message, $ScriptBlock) {
    Write-Host "`n>> $Message" -ForegroundColor Cyan
    try {
        $result = &$ScriptBlock
        Write-Host "✅ SUCESSO" -ForegroundColor Green
        return $result
    } catch {
        Write-Host "❌ FALHA: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "Conteúdo do Erro: $($reader.ReadToEnd())" -ForegroundColor Yellow
        }
        return $null
    }
}

Write-Host "=== INICIANDO TESTE COMPLETO DOS MICROSERVIÇOS ===" -ForegroundColor White -BackgroundColor DarkBlue

# --- AUTH ---
$tokenData = Test-Step "AUTH: Fazer Login" {
    $body = @{ username="admin"; password="1234" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$gateway/auth/login" -Method Post -Body $body -Headers $headers
}

if ($tokenData -eq $null) { Write-Host "Impossível continuar sem Token."; exit }
$headers.Add("Authorization", "Bearer $($tokenData.token)")

# --- CATALOG (CRUD) ---
$prod = Test-Step "CATALOG: Criar Produto (POST)" {
    $body = @{ name="Rato Gamer"; price=45.90; category="Acessórios"; description="RGB 12000 DPI" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$gateway/products" -Method Post -Body $body -Headers $headers
}

Test-Step "CATALOG: Listar Produtos (GET)" {
    Invoke-RestMethod -Uri "$gateway/products" -Method Get -Headers $headers
}

Test-Step "CATALOG: Atualizar Produto (PUT)" {
    $body = @{ name="Rato Gamer Pro"; price=55.00; category="Acessórios" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$gateway/products/$($prod.id)" -Method Put -Body $body -Headers $headers
}

# --- CART ---
Test-Step "CART: Adicionar item ao carrinho" {
    $body = @{ productId=$prod.id; quantity=2 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$gateway/cart" -Method Post -Body $body -Headers $headers
}

Test-Step "CART: Visualizar carrinho" {
    Invoke-RestMethod -Uri "$gateway/cart" -Method Get -Headers $headers
}

# --- ORDERS ---
$order = Test-Step "ORDER: Finalizar Compra (Checkout)" {
    $body = @{ username="admin"; productId=$prod.id; quantity=2 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$gateway/order" -Method Post -Body $body -Headers $headers
}

if ($order -ne $null) {
    Test-Step "ORDER: Consultar estado da encomenda" {
        Invoke-RestMethod -Uri "$gateway/order/$($order.id)" -Method Get -Headers $headers
    }
}

# --- CLEANUP (DELETE) ---
Test-Step "CATALOG: Apagar Produto (DELETE)" {
    Invoke-RestMethod -Uri "$gateway/products/$($prod.id)" -Method Delete -Headers $headers
}

Write-Host "`n=== TESTE FINALIZADO ===" -ForegroundColor White -BackgroundColor DarkGreen
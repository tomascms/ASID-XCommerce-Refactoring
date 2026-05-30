#!/usr/bin/env bash
# T-MICRO-4: Estado Zombie — ausência de Saga compensatória
# Demonstra que parar o payment-service após o checkout cria orders em HANDLING permanente
# com stock decrementado mas sem pagamento processado.
#
# IMPORTANTE: o script NÃO reinicia o payment-service automaticamente.
# Isso é intencional — a order deve ficar em HANDLING para evidência no relatório.
# Para reiniciar manualmente: docker start xcommerce-payment-service
#
# Uso: ./t-micro-4-estado-zombie.sh

set -euo pipefail

BASE_URL="http://localhost:9000"
OUTPUT="resultados/t-micro-4-estado-zombie-$(date +%Y%m%d-%H%M%S).txt"

echo "=== T-MICRO-4: Estado Zombie ===" | tee "$OUTPUT"
echo "Data: $(date)" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Obter token
TOKEN=$(curl -s -X POST "$BASE_URL/rest/user/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "Token obtido." | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Estado inicial
STOCK_ANTES=$(docker exec xcommerce-db psql -U postgres -d xcommerce_inventory \
  -t -c "SELECT quantity FROM inventory WHERE product_id = 1;" | tr -d ' \n')
echo "--- Estado ANTES ---" | tee -a "$OUTPUT"
echo "Stock produto 1: $STOCK_ANTES" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Adicionar produto ao carrinho
curl -s -X PATCH "$BASE_URL/rest/shoppingCart/addProduct" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"productId":1,"quantity":1}' > /dev/null

# Fazer checkout
echo "--- Checkout ---" | tee -a "$OUTPUT"
CHECKOUT_RESP=$(curl -s -X POST "$BASE_URL/rest/order/checkout" \
  -H "Authorization: Bearer $TOKEN")
ORDER_ID=$(echo "$CHECKOUT_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id','?'))")
ORDER_STATUS_INICIAL=$(echo "$CHECKOUT_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','?'))")
echo "Order criada: id=$ORDER_ID · status=$ORDER_STATUS_INICIAL" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Parar payment-service imediatamente (janela: 2s de delay no PaymentProcessor)
CONTAINER_NAME=$(docker ps --format "{{.Names}}" | grep payment | head -1 || true)
if [[ -z "$CONTAINER_NAME" ]]; then
  echo "payment-service container not found — likely removed from compose. Skipping stop step." | tee -a "$OUTPUT"
  echo "NOTE: payment-service removed from stack; this scenario must be adapted in tests." | tee -a "$OUTPUT"
else
  docker stop "$CONTAINER_NAME" > /dev/null
  echo "payment-service ($CONTAINER_NAME) parado. O evento Kafka ficou sem consumidor." | tee -a "$OUTPUT"
fi
echo "" | tee -a "$OUTPUT"

# Aguardar para confirmar que o evento não foi processado
echo "Aguardar 10s (PaymentProcessor tem 2s de delay — janela já passou)..." | tee -a "$OUTPUT"
sleep 10

# Estado após a falha
echo "--- Estado APÓS a falha ---" | tee -a "$OUTPUT"
STOCK_DEPOIS=$(docker exec xcommerce-db psql -U postgres -d xcommerce_inventory \
  -t -c "SELECT quantity FROM inventory WHERE product_id = 1;" | tr -d ' \n')
ORDER_STATUS=$(docker exec xcommerce-db psql -U postgres -d xcommerce_orders \
  -t -c "SELECT status FROM orders WHERE id = $ORDER_ID;" | tr -d ' \n')

echo "Stock produto 1: $STOCK_DEPOIS (era $STOCK_ANTES → decremento: $((STOCK_ANTES - STOCK_DEPOIS)))" | tee -a "$OUTPUT"
echo "Status da order $ORDER_ID: $ORDER_STATUS" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

echo "--- ANÁLISE ---" | tee -a "$OUTPUT"
if [[ "$ORDER_STATUS" == "HANDLING" ]]; then
  echo "*** ESTADO ZOMBIE CONFIRMADO ***" | tee -a "$OUTPUT"
  echo "  - Stock decrementado: $STOCK_ANTES → $STOCK_DEPOIS (−$((STOCK_ANTES - STOCK_DEPOIS)) unidade)" | tee -a "$OUTPUT"
  echo "  - Order $ORDER_ID em HANDLING: pagamento nunca processado" | tee -a "$OUTPUT"
  echo "  - Carrinho esvaziado: irreversível" | tee -a "$OUTPUT"
  echo "  - Sem Saga: stock NÃO será reposto automaticamente" | tee -a "$OUTPUT"
else
  echo "AVISO: order está $ORDER_STATUS — o payment-service processou antes de ser parado." | tee -a "$OUTPUT"
  echo "Tenta correr o script novamente — a janela de 2s pode ter sido perdida." | tee -a "$OUTPUT"
fi

echo "" | tee -a "$OUTPUT"
echo "payment-service continua PARADO para evidência." | tee -a "$OUTPUT"
echo "Para reiniciar: docker start $CONTAINER_NAME" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"
echo "=== FIM ===" | tee -a "$OUTPUT"
echo "Resultado guardado em: $OUTPUT"

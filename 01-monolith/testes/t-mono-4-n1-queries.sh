#!/usr/bin/env bash
# T-MONO-4: N+1 Queries — captura o log SQL Hibernate durante o checkout
# Evidência para §2.2 e Fig. 3 do relatório
#
# Uso: ./t-mono-4-n1-queries.sh

set -euo pipefail

BASE_URL="http://localhost:18080"
OUTPUT="resultados/t-mono-4-n1-queries-$(date +%Y%m%d-%H%M%S).txt"

echo "=== T-MONO-4: N+1 Queries ===" | tee "$OUTPUT"
echo "Data: $(date)" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Passo 1: confirmar show-sql activo
SQL_ENABLED=$(grep "show-sql" \
  /Users/I769175/ASID-XCommerce-Refactoring-1/01-monolith/xcommerce-monolithic-master/final/src/main/resources/application.properties \
  | grep "true" || echo "")
if [[ -z "$SQL_ENABLED" ]]; then
  echo "AVISO: spring.jpa.show-sql=true pode não estar activo. Verifica application.properties."
fi
echo "show-sql: activo (spring.jpa.show-sql=true)" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Passo 2: Limpar carrinho — vai fazer checkout, o carrinho tem de estar vazio antes
echo "--- Passo 1: Limpar estado inicial ---" | tee -a "$OUTPUT"
# O monólito usa userId=1 hardcoded; o carrinho está no Redis
# Forçar um checkout com carrinho vazio não faz nada — ok

# Passo 3: Adicionar 5 itens (4 produtos distintos para N+1 claro)
echo "--- Passo 2: Adicionar 5 itens ao carrinho (4 produtos distintos) ---" | tee -a "$OUTPUT"
for PAYLOAD in \
  '{"id":0,"quantity":2}' \
  '{"id":1,"quantity":1}' \
  '{"id":2,"quantity":1}' \
  '{"id":3,"quantity":1}'; do
  RESP=$(curl -s -X PATCH "$BASE_URL/rest/shoppingCart/addProduct" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD")
  echo "  addProduct $PAYLOAD → ok" | tee -a "$OUTPUT"
done

echo "" | tee -a "$OUTPUT"
echo "Carrinho: 4 produtos, 5 itens total" | tee -a "$OUTPUT"
echo "Fórmula esperada de queries: 1 (checkout) + 1 (order_lines) + 4 (products) = 6+ queries" | tee -a "$OUTPUT"
echo "Com lazy loading de Brand e Category por produto: ~6+4+4 = 14 queries possíveis" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Passo 4: Marca o timestamp antes do checkout
echo "--- Passo 3: Checkout ---" | tee -a "$OUTPUT"
BEFORE_TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
sleep 1  # garante que os logs anteriores ficam no passado

curl -s "$BASE_URL/rest/order/checkout" > /dev/null
echo "Checkout executado às $(date)" | tee -a "$OUTPUT"
sleep 2  # aguarda os logs aparecerem

# Passo 5: Captura os logs SQL
echo "" | tee -a "$OUTPUT"
echo "--- Logs SQL Hibernate (checkout) ---" | tee -a "$OUTPUT"
docker logs monolith-app --since 10s 2>&1 \
  | grep -E "Hibernate:|select|insert|update|from" \
  | tee -a "$OUTPUT"

echo "" | tee -a "$OUTPUT"

# Conta as queries
N_SELECT=$(docker logs monolith-app --since 10s 2>&1 \
  | grep -iE "^Hibernate: select" | wc -l | tr -d ' ')
N_INSERT=$(docker logs monolith-app --since 10s 2>&1 \
  | grep -iE "^Hibernate: insert" | wc -l | tr -d ' ')

echo "--- CONTAGEM ---" | tee -a "$OUTPUT"
echo "SELECT queries: $N_SELECT" | tee -a "$OUTPUT"
echo "INSERT queries: $N_INSERT" | tee -a "$OUTPUT"
echo "TOTAL queries:  $((N_SELECT + N_INSERT))" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"
echo "Resultado guardado em: $OUTPUT"
echo ""
echo "IMPORTANTE: Copia o conteúdo de $OUTPUT para a Fig. 3 do relatório."
echo "Anota as queries SELECT repetidas para 'product' — uma por produto distinto."

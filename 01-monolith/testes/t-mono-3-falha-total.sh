#!/usr/bin/env bash
# T-MONO-3: Falha Total — prova que parar a BD derruba 100% dos endpoints
# Evidência para §5.3 T3 e §2.2 do relatório (ponto único de falha)
#
# Uso: ./t-mono-3-falha-total.sh
#
# REQUER DOIS TERMINAIS:
#   Terminal 1: corre este script (loop de pedidos)
#   Terminal 2: corre o docker stop manualmente quando indicado

set -euo pipefail

BASE_URL="http://localhost:18080"
OUTPUT="resultados/t-mono-3-falha-$(date +%Y%m%d-%H%M%S).txt"

echo "=== T-MONO-3: Falha Total do Monólito ===" | tee "$OUTPUT"
echo "Data: $(date)" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"
echo "Monólito: $BASE_URL" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Obtém token
TOKEN=$(curl -s -X POST "$BASE_URL/rest/user/authenticate" \
  -H "Content-Type: application/json" \
  -d '{"username":"root","password":"root"}' | tr -d '"')

if [[ -z "$TOKEN" || "$TOKEN" == *"error"* ]]; then
  echo "ERRO: Não foi possível obter token. Confirma que o monólito está UP em $BASE_URL"
  exit 1
fi
echo "Token obtido." | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

echo "============================================================" | tee -a "$OUTPUT"
echo "A fazer pedidos contínuos. AGORA abre outro terminal e corre:" | tee -a "$OUTPUT"
echo "   docker stop monolith-db" | tee -a "$OUTPUT"
echo "Aguarda 30s, depois:" | tee -a "$OUTPUT"
echo "   docker start monolith-db" | tee -a "$OUTPUT"
echo "============================================================" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"
echo "Timestamp            | /products | /rest/order/list | /rest/user/authenticate" | tee -a "$OUTPUT"
echo "---------------------|-----------|-----------------|------------------------" | tee -a "$OUTPUT"

DURATION=120  # 2 minutos de loop
END_TIME=$((SECONDS + DURATION))

while [[ $SECONDS -lt $END_TIME ]]; do
  TS=$(date '+%H:%M:%S')

  # Endpoint 1: catálogo (sem auth)
  S1=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 \
    "$BASE_URL/rest/catalog/products" 2>/dev/null || echo "ERR")

  # Endpoint 2: orders (sem auth — hardcoded userId=1)
  S2=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 \
    "$BASE_URL/rest/order/list" 2>/dev/null || echo "ERR")

  # Endpoint 3: auth
  S3=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 \
    -X POST "$BASE_URL/rest/user/authenticate" \
    -H "Content-Type: application/json" \
    -d '{"username":"root","password":"root"}' 2>/dev/null || echo "ERR")

  LINE="$TS              | $S1        | $S2              | $S3"
  echo "$LINE" | tee -a "$OUTPUT"

  sleep 2
done

echo "" | tee -a "$OUTPUT"
echo "=== FIM DO TESTE ===" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Contagem de resultados
echo "--- SUMÁRIO ---" | tee -a "$OUTPUT"
TOTAL=$(grep -c "^[0-9]" "$OUTPUT" 2>/dev/null || echo 0)
OK=$(grep "^[0-9]" "$OUTPUT" 2>/dev/null | grep -c "| 200" || echo 0)
FAIL=$(grep "^[0-9]" "$OUTPUT" 2>/dev/null | grep -cv "| 200" || echo 0)
echo "Total de amostras: $TOTAL" | tee -a "$OUTPUT"
echo "Respostas 200 (OK): $OK" | tee -a "$OUTPUT"
echo "Respostas não-200 (FALHA): $FAIL" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"
echo "Resultado guardado em: $OUTPUT"

#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# T-MICRO-RECURSOS — Captura de RAM/CPU dos microserviços XCommerce
# ─────────────────────────────────────────────────────────────────────────────
# Sustenta a hipótese H2b (recursos proporcionais ao número de processos JVM).
# Captura `docker stats` em estado ocioso e durante carga (T2).
#
# Pré-requisitos: microserviços a correr.
# Saída: testes/k6/output/recursos-idle.txt e recursos-carga.txt
#
# Uso: ./testes-recursos.sh
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K6_DIR="$SCRIPT_DIR/k6"
OUT_DIR="$K6_DIR/output"
BASE_URL="http://localhost:9000"

mkdir -p "$OUT_DIR"

echo "═══════════════════════════════════════════════════════════════"
echo "  TESTE DE RECURSOS — MICROSERVIÇOS"
echo "═══════════════════════════════════════════════════════════════"
echo

if ! curl -fsS -o /dev/null "$BASE_URL/actuator/health"; then
  echo "✗ Gateway não responde. Sobe primeiro: cd 02-microservices && docker compose up -d"
  exit 1
fi

# ─── CAPTURA 1: IDLE ──────────────────────────────────────────────────────────
echo "▸ Aguardar 30s sem tráfego para garantir estado ocioso …"
sleep 30

echo "▸ Captura IDLE (sem carga) …"
date "+  $(date '+%H:%M:%S')"
docker stats --no-stream \
  --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
  $(docker ps --filter "name=xcommerce" --format "{{.Names}}") \
  | tee "$OUT_DIR/recursos-idle.txt"
echo

# ─── CAPTURA 2: SOB CARGA ────────────────────────────────────────────────────
echo "▸ Iniciar T2 em background para gerar carga sustentada …"
TOKEN_MICRO=$(curl -fsS -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  "$BASE_URL/rest/user/login" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory \
  -c "UPDATE inventory SET quantity=10000000, quantity_on_hand=10000000;" >/dev/null
curl -s -X DELETE "$BASE_URL/rest/shoppingCart" \
  -H "Authorization: Bearer $TOKEN_MICRO" >/dev/null

k6 run --quiet -e TOKEN="$TOKEN_MICRO" \
  "$K6_DIR/t-micro-2-checkout.js" >/dev/null 2>&1 &
K6_PID=$!

echo "  k6 PID: $K6_PID — esperar 45s para entrar na janela de medição …"
sleep 45

echo "▸ Captura SOB CARGA …"
date "+  $(date '+%H:%M:%S')"
docker stats --no-stream \
  --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
  $(docker ps --filter "name=xcommerce" --format "{{.Names}}") \
  | tee "$OUT_DIR/recursos-carga.txt"

wait "$K6_PID" 2>/dev/null || true
echo
echo "Ficheiros gerados:"
echo "  • $OUT_DIR/recursos-idle.txt"
echo "  • $OUT_DIR/recursos-carga.txt"

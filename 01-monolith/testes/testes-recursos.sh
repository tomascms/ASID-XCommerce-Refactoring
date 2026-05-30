#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# T-MONO-RECURSOS — Captura de RAM/CPU do monólito XCommerce
# ─────────────────────────────────────────────────────────────────────────────
# Sustenta a hipótese H2b (recursos proporcionais ao número de processos).
# Captura `docker stats` em estado ocioso e durante carga (T2).
#
# Pré-requisitos: monólito a correr.
# Saída: testes/resultados/recursos-idle.txt e recursos-carga.txt
#
# Uso: ./testes-recursos.sh
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/resultados"
BASE_URL="http://localhost:18080"

mkdir -p "$RESULTS_DIR"

echo "═══════════════════════════════════════════════════════════════"
echo "  TESTE DE RECURSOS — MONÓLITO"
echo "═══════════════════════════════════════════════════════════════"
echo

if ! curl -fsS -o /dev/null "$BASE_URL/actuator/health"; then
  echo "✗ Monólito não responde. Sobe primeiro: cd 01-monolith && docker compose up -d"
  exit 1
fi

# ─── CAPTURA 1: IDLE ──────────────────────────────────────────────────────────
echo "▸ Aguardar 30s sem tráfego para garantir estado ocioso …"
sleep 30

echo "▸ Captura IDLE (sem carga) …"
date "+  $(date '+%H:%M:%S')"
docker stats --no-stream \
  --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
  $(docker ps --filter "name=monolith" --format "{{.Names}}") \
  | tee "$RESULTS_DIR/recursos-idle.txt"
echo

# ─── CAPTURA 2: SOB CARGA ────────────────────────────────────────────────────
echo "▸ Iniciar T2 em background para gerar carga sustentada …"
TOKEN_MONO=$(curl -fsS -X POST -H "Content-Type: application/json" \
  -d '{"username":"root","password":"root"}' \
  "$BASE_URL/rest/user/authenticate")
docker exec monolith-redis redis-cli FLUSHALL >/dev/null
docker exec monolith-db psql -U postgres -d xcommerce \
  -c "UPDATE product SET quantity=10000000;" >/dev/null

k6 run --quiet -e TOKEN="$TOKEN_MONO" \
  "$SCRIPT_DIR/t-mono-2-checkout.js" >/dev/null 2>&1 &
K6_PID=$!

echo "  k6 PID: $K6_PID — esperar 45s para entrar na janela de medição …"
sleep 45

echo "▸ Captura SOB CARGA …"
date "+  $(date '+%H:%M:%S')"
docker stats --no-stream \
  --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
  $(docker ps --filter "name=monolith" --format "{{.Names}}") \
  | tee "$RESULTS_DIR/recursos-carga.txt"

wait "$K6_PID" 2>/dev/null || true
echo
echo "Ficheiros gerados:"
echo "  • $RESULTS_DIR/recursos-idle.txt"
echo "  • $RESULTS_DIR/recursos-carga.txt"

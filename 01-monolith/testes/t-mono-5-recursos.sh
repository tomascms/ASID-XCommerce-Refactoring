#!/usr/bin/env bash
# T-MONO-5: Recursos CPU e RAM — idle e sob carga
# Evidência para §5.3 T5 e Fig. 15 do relatório
#
# Uso: ./t-mono-5-recursos.sh

set -euo pipefail

BASE_URL="http://localhost:18080"
TESTES_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT="resultados/t-mono-5-recursos-$(date +%Y%m%d-%H%M%S).txt"

echo "=== T-MONO-5: Recursos CPU e RAM ===" | tee "$OUTPUT"
echo "Data: $(date)" | tee -a "$OUTPUT"
echo "Arquitetura: MONÓLITO (3 containers)" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Informação da máquina
echo "--- Máquina ---" | tee -a "$OUTPUT"
sysctl -n machdep.cpu.brand_string 2>/dev/null | tee -a "$OUTPUT" || true
echo "RAM total: $(( $(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1024 / 1024 / 1024 )) GB" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Confirmação de containers a correr
echo "--- Containers activos ---" | tee -a "$OUTPUT"
docker ps --format "table {{.Names}}\t{{.Status}}" \
  | grep -E "monolith|NAME" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# IDLE — aguarda 10s sem carga antes de medir
echo "--- IDLE (sem carga) — aguardar 10s ---" | tee -a "$OUTPUT"
sleep 10
docker stats --no-stream \
  --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
  monolith-app monolith-db monolith-redis 2>/dev/null | tee -a "$OUTPUT"

echo "" | tee -a "$OUTPUT"

# Calcula RAM total idle
RAM_TOTAL_IDLE=$(docker stats --no-stream \
  --format "{{.MemUsage}}" \
  monolith-app monolith-db monolith-redis 2>/dev/null \
  | awk -F'/' '{print $1}' | tr -d ' MiBGiB' \
  | awk '{sum+=$1} END {printf "%.0f MiB\n", sum}')
echo "RAM total idle (soma): $RAM_TOTAL_IDLE" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# SOB CARGA — obtém token e corre T-MONO-1 em background
echo "--- SOB CARGA (k6 T-MONO-1, 10 VUs, 30s) ---" | tee -a "$OUTPUT"
TOKEN=$(curl -s -X POST "$BASE_URL/rest/user/authenticate" \
  -H "Content-Type: application/json" \
  -d '{"username":"root","password":"root"}' | tr -d '"')

if [[ -z "$TOKEN" || "$TOKEN" == *"error"* ]]; then
  echo "AVISO: Não foi possível obter token. A medir recursos sem carga activa."
else
  k6 run \
    --vus 10 --duration 30s \
    -e TOKEN="$TOKEN" \
    "$TESTES_DIR/t-mono-1-leitura.js" > /dev/null 2>&1 &
  K6_PID=$!

  sleep 15  # mede a meio da carga (JVM já aquecida)
  echo "k6 em execução (PID $K6_PID) — medindo a meio da carga..." | tee -a "$OUTPUT"
  docker stats --no-stream \
    --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
    monolith-app monolith-db monolith-redis 2>/dev/null | tee -a "$OUTPUT"

  wait $K6_PID || true
fi

echo "" | tee -a "$OUTPUT"
echo "=== FIM ===" | tee -a "$OUTPUT"
echo "Resultado guardado em: $OUTPUT"
echo ""
echo "Copiar para relatório §5.3 T5:"
echo "  - RAM idle total"
echo "  - CPU idle (soma dos 3 containers)"
echo "  - RAM sob carga"
echo "  - Número de containers: 3 (vs 11+ nos microserviços)"

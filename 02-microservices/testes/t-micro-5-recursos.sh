#!/usr/bin/env bash
# T-MICRO-5: Recursos CPU e RAM — idle e sob carga (microserviços)
# Evidência para §5.3 T5 — comparar com monólito (3 containers / ~1.31 GiB idle / 1.5% CPU)
#
# Uso: ./t-micro-5-recursos.sh

set -euo pipefail

TESTES_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT="resultados/t-micro-5-recursos-$(date +%Y%m%d-%H%M%S).txt"

echo "=== T-MICRO-5: Recursos CPU e RAM ===" | tee "$OUTPUT"
echo "Data: $(date)" | tee -a "$OUTPUT"
echo "Arquitetura: MICROSERVIÇOS" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

echo "--- Máquina ---" | tee -a "$OUTPUT"
sysctl -n machdep.cpu.brand_string 2>/dev/null | tee -a "$OUTPUT" || true
echo "RAM total: $(( $(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1024 / 1024 / 1024 )) GB" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Lista containers xcommerce activos
CONTAINERS=$(docker ps --format "{{.Names}}" | grep -i xcommerce | tr '\n' ' ')
echo "--- Containers xcommerce activos ---" | tee -a "$OUTPUT"
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -i xcommerce | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# IDLE
echo "--- IDLE (sem carga) — aguardar 10s ---" | tee -a "$OUTPUT"
sleep 10
docker stats --no-stream \
  --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
  $CONTAINERS 2>/dev/null | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# Conta containers
N_CONTAINERS=$(echo $CONTAINERS | wc -w | tr -d ' ')
echo "Total containers xcommerce: $N_CONTAINERS (monólito tinha 3)" | tee -a "$OUTPUT"
echo "" | tee -a "$OUTPUT"

# SOB CARGA — corre T-MICRO-1 em background (endpoint público, sem token)
echo "--- SOB CARGA (k6 T-MICRO-1, 10 VUs, 30s) ---" | tee -a "$OUTPUT"
k6 run \
  --vus 10 --duration 30s \
  "$TESTES_DIR/t-micro-1-leitura.js" > /dev/null 2>&1 &
K6_PID=$!

sleep 15
echo "k6 em execução (PID $K6_PID) — medindo a meio da carga..." | tee -a "$OUTPUT"
docker stats --no-stream \
  --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
  $CONTAINERS 2>/dev/null | tee -a "$OUTPUT"

wait $K6_PID || true

echo "" | tee -a "$OUTPUT"
echo "=== FIM ===" | tee -a "$OUTPUT"
echo "Resultado guardado em: $OUTPUT"
echo ""
echo "Comparação com monólito:"
echo "  Monólito idle:        ~1.31 GiB RAM  /  1.5% CPU  /  3 containers"
echo "  Microserviços idle:   ver $OUTPUT"

#!/usr/bin/env bash
# H2b — Captura recursos em IDLE para ambas as arquiteturas
# Uso: ./capturar-h2b.sh [mono|micro] [run_number]
# Exemplo: ./capturar-h2b.sh mono 1
# Resultado: resultados/{mono,micro}_run{N}.csv
#
# PRÉ-REQUISITO:
#   - Arquitetura alvo já arrancada (docker compose up -d)
#   - Esperar 5 min após arranque antes de correr este script
#   - Seed NÃO é necessário para H2b (mede idle, sem tráfego)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$SCRIPT_DIR/resultados"
mkdir -p "$OUT"

ARQ="${1:-mono}"     # 'mono' ou 'micro'
RUN="${2:-1}"
OUT_FILE="$OUT/${ARQ}_run${RUN}.csv"
SAMPLES=24           # 24 amostras × 5s = 2 min
INTERVAL=5           # segundos entre amostras

if [[ -f "$OUT_FILE" ]]; then
  echo "SKIP (já existe): $OUT_FILE"
  exit 0
fi

echo "container,mem_mb,cpu_pct,sample" > "$OUT_FILE"
echo "A capturar $SAMPLES amostras de $ARQ (intervalo ${INTERVAL}s)..."

for i in $(seq 1 $SAMPLES); do
  docker stats --no-stream --format "{{.Name}},{{.MemUsage}},{{.CPUPerc}}" | while IFS=',' read -r name mem cpu; do
    # Converter "123.4MiB / 7.77GiB" → só o primeiro número em MB
    mem_mb=$(echo "$mem" | awk '{
      val=$1; unit=$2
      gsub(/[^0-9.]/, "", val)
      if (unit ~ /GiB/) val = val * 1024
      if (unit ~ /kB/)  val = val / 1024
      printf "%.1f", val
    }')
    cpu_pct=$(echo "$cpu" | tr -d '%')
    echo "$name,$mem_mb,$cpu_pct,$i"
  done >> "$OUT_FILE"
  sleep "$INTERVAL"
done

echo "Concluído: $OUT_FILE"
echo "Resumo por contentor (média RAM MB):"
awk -F',' 'NR>1 {sum[$1]+=$2; cnt[$1]++} END {for(c in sum) printf "  %-40s %.1f MB\n", c, sum[c]/cnt[c]}' "$OUT_FILE" | sort -k2 -rn

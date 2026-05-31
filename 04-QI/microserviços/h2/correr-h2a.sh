#!/usr/bin/env bash
# Corre os cenários H2a para os MICROSERVIÇOS (QI2 revista)
# Cenários: T1 (catálogo, 1 hop) e T3 (checkout, 5 hops síncronos)
# Carga fixa: 10 VUs, 3 min, 2 runs por cenário
# Resultado: resultados/{t1,t3}_run{1,2}.json
# PRÉ-REQUISITO: microserviços a correr + seed executado (ver 04-QI/seed/seed-microservicos.sh)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$SCRIPT_DIR/resultados"
mkdir -p "$OUT"

SEED_USER="${SEED_USER:-user1}"
SEED_PASS="${SEED_PASS:-password}"
# IDs dos produtos no catalog-service (obtidos após seed-microservicos.sh)
# Sobrepor se necessário: PRODUCT_ID1=X PRODUCT_ID2=Y ./correr-h2a.sh
PRODUCT_ID1="${PRODUCT_ID1:-1}"
PRODUCT_ID2="${PRODUCT_ID2:-2}"

SCENARIOS=(t1-catalogo t3-checkout)
VUS=10
RUNS=2

echo "=== H2a MICROSERVIÇOS — $(date) ==="
echo "Cenários: ${SCENARIOS[*]} | VUs: $VUS | Runs: $RUNS"

for SCRIPT in "${SCENARIOS[@]}"; do
  for RUN in $(seq 1 $RUNS); do
    OUT_FILE="$OUT/${SCRIPT}_VU${VUS}_run${RUN}.json"
    if [[ -f "$OUT_FILE" ]]; then
      echo "SKIP (já existe): $OUT_FILE"
      continue
    fi
    echo ""
    echo ">>> $SCRIPT | VUs=$VUS | run=$RUN"
    k6 run \
      --out "json=$OUT_FILE" \
      -e VUS="$VUS" \
      -e SEED_USER="$SEED_USER" \
      -e SEED_PASS="$SEED_PASS" \
      -e PRODUCT_ID1="$PRODUCT_ID1" \
      -e PRODUCT_ID2="$PRODUCT_ID2" \
      "$SCRIPT_DIR/${SCRIPT}.js"
  done
done

echo ""
echo "=== Concluído: $(ls "$OUT"/*.json 2>/dev/null | wc -l) ficheiros em $OUT ==="

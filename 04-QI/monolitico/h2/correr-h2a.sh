#!/usr/bin/env bash
# Corre os cenários H2a para o MONOLITO (QI2 revista)
# Cenários: T1 (carrinho, 1 hop) e T3 (checkout, 1 transação JPA)
# Carga fixa: 10 VUs, 3 min, 2 runs por cenário
# Resultado: resultados/{t1,t3}_run{1,2}.json
# PRÉ-REQUISITO: monolito a correr + seed executado (ver 04-QI/seed/seed-monolito.sh)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$SCRIPT_DIR/resultados"
mkdir -p "$OUT"

SEED_USER="${SEED_USER:-user1}"
SEED_PASS="${SEED_PASS:-password}"

SCENARIOS=(t1-catalogo t3-checkout)
VUS=10
RUNS=2

echo "=== H2a MONOLITO — $(date) ==="
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
      "$SCRIPT_DIR/${SCRIPT}.js"
  done
done

echo ""
echo "=== Concluído: $(ls "$OUT"/*.json 2>/dev/null | wc -l) ficheiros em $OUT ==="

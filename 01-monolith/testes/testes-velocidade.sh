#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# T-MONO-VELOCIDADE — Teste de velocidade do monólito XCommerce
# ─────────────────────────────────────────────────────────────────────────────
# Sustenta a hipótese H2a (custo super-linear da decomposição).
# Executa T1 (leitura simples) e T2 (checkout) com a ferramenta k6.
#
# Pré-requisitos: monólito a correr (docker compose up -d em 01-monolith/).
# Saída: ficheiros JSON em testes/resultados/ com mediana, p95, throughput.
#
# Uso:
#   ./testes-velocidade.sh
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/resultados"
BASE_URL="http://localhost:18080"

mkdir -p "$RESULTS_DIR"

echo "═══════════════════════════════════════════════════════════════"
echo "  TESTE DE VELOCIDADE — MONÓLITO"
echo "═══════════════════════════════════════════════════════════════"
echo

# ─── Verificar que o monólito está acessível ──────────────────────────────────
echo "▸ Verificar monólito em $BASE_URL …"
if ! curl -fsS -o /dev/null "$BASE_URL/actuator/health"; then
  echo "  ✗ Monólito não responde. Sobe primeiro com:"
  echo "    cd 01-monolith && docker compose up -d"
  exit 1
fi
echo "  ✓ Monólito UP"
echo

# ─── Repor stock para garantir que os checkouts não falham ────────────────────
echo "▸ Repor stock dos produtos a 10 milhões …"
docker exec monolith-db psql -U postgres -d xcommerce \
  -c "UPDATE product SET quantity=10000000;" >/dev/null
docker exec monolith-redis redis-cli FLUSHALL >/dev/null
echo "  ✓ Stock reposto, carrinho Redis limpo"
echo

# ─── Obter token JWT ──────────────────────────────────────────────────────────
echo "▸ Login (root/root) para obter token JWT …"
TOKEN_MONO=$(curl -fsS -X POST -H "Content-Type: application/json" \
  -d '{"username":"root","password":"root"}' \
  "$BASE_URL/rest/user/authenticate")
echo "  ✓ Token obtido (${#TOKEN_MONO} caracteres)"
echo

# ─── T1: Leitura simples ─────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo "  T1 — Leitura de catálogo (10 VUs, sem think time, 100s)"
echo "═══════════════════════════════════════════════════════════════"
date "+  Início: %H:%M:%S"
k6 run --quiet \
  --summary-export="$RESULTS_DIR/t-mono-1.json" \
  "$SCRIPT_DIR/t-mono-1-leitura.js" 2>&1 | tail -8
date "+  Fim:    %H:%M:%S"
echo

# ─── T2: Checkout ────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo "  T2 — Checkout (1 VU, sem think time, 100s)"
echo "═══════════════════════════════════════════════════════════════"
docker exec monolith-redis redis-cli FLUSHALL >/dev/null
docker exec monolith-db psql -U postgres -d xcommerce \
  -c "UPDATE product SET quantity=10000000;" >/dev/null
date "+  Início: %H:%M:%S"
k6 run --quiet \
  --summary-export="$RESULTS_DIR/t-mono-2.json" \
  -e TOKEN="$TOKEN_MONO" \
  "$SCRIPT_DIR/t-mono-2-checkout.js" 2>&1 | tail -8
date "+  Fim:    %H:%M:%S"
echo

# ─── Resumo ──────────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo "  RESUMO"
echo "═══════════════════════════════════════════════════════════════"
python3 - <<EOF
import json
for label, path in [
    ("T1 leitura ", "$RESULTS_DIR/t-mono-1.json"),
    ("T2 checkout", "$RESULTS_DIR/t-mono-2.json"),
]:
    with open(path) as f:
        d = json.load(f)
    m = d["metrics"]["http_req_duration"]
    it = d["metrics"]["iteration_duration"]
    its = d["metrics"]["iterations"]
    print(f"  {label}: p50={it['med']:.2f}ms · p95={it['p(95)']:.2f}ms · "
          f"{int(its['count'])} iter · {its['rate']:.0f}/s")
EOF
echo
echo "Ficheiros gerados:"
echo "  • $RESULTS_DIR/t-mono-1.json"
echo "  • $RESULTS_DIR/t-mono-2.json"

#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# RUN-ALL-TESTS.SH — Pipeline completa de testes do XCommerce
# ─────────────────────────────────────────────────────────────────────────────
# Sobe ambas as arquiteturas, espera healthy, executa os 4 testes em sequência
# (3 com saída numérica + 1 análise estática), e produz relatório resumido.
#
# Atenção: o pipeline completo demora ~15 minutos.
#
# Uso:
#   ./run-all-tests.sh           Corre tudo
#   ./run-all-tests.sh --skip-up Pressupõe que as stacks já estão UP
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONOLITH_DIR="$ROOT/01-monolith"
MICRO_DIR="$ROOT/02-microservices"
SKIP_UP=${1:-}

banner() {
  echo
  echo "╔═══════════════════════════════════════════════════════════════╗"
  printf  "║  %-61s║\n" "$1"
  echo "╚═══════════════════════════════════════════════════════════════╝"
}

# ─── Verificar pré-requisitos ────────────────────────────────────────────────
banner "VERIFICAR PRÉ-REQUISITOS"
for cmd in docker k6 python3 curl; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "✗ Comando obrigatório em falta: $cmd"
    exit 1
  fi
done
echo "✓ docker, k6, python3, curl disponíveis"

# ─── Subir stacks (a menos que --skip-up) ────────────────────────────────────
if [ "$SKIP_UP" != "--skip-up" ]; then
  banner "SUBIR MONÓLITO"
  cd "$MONOLITH_DIR"
  docker compose up -d 2>&1 | tail -3
  cd "$ROOT"

  banner "SUBIR MICROSERVIÇOS"
  cd "$MICRO_DIR"
  docker compose up -d 2>&1 | tail -3
  cd "$ROOT"

  echo
  echo "▸ Aguardar 45s para todas as stacks ficarem healthy …"
  sleep 45
fi

# ─── Análise estática de dependências (H1) ──────────────────────────────────
banner "TESTE D — ANÁLISE DE DEPENDÊNCIAS (H1)"
"$ROOT/analise-dependencias.sh" || true

# ─── Velocidade — Monólito ──────────────────────────────────────────────────
banner "TESTE A1 — VELOCIDADE (Monólito)"
cd "$MONOLITH_DIR/testes"
./testes-velocidade.sh
cd "$ROOT"

# ─── Velocidade — Microserviços ─────────────────────────────────────────────
banner "TESTE A2 — VELOCIDADE (Microserviços)"
cd "$MICRO_DIR/testes"
./testes-velocidade.sh
cd "$ROOT"

# ─── Recursos — Monólito ─────────────────────────────────────────────────────
banner "TESTE B1 — RECURSOS (Monólito)"
cd "$MONOLITH_DIR/testes"
./testes-recursos.sh
cd "$ROOT"

# ─── Recursos — Microserviços ───────────────────────────────────────────────
banner "TESTE B2 — RECURSOS (Microserviços)"
cd "$MICRO_DIR/testes"
./testes-recursos.sh
cd "$ROOT"

# ─── Falha — Monólito (rollback ACID) ────────────────────────────────────────
banner "TESTE C1 — FALHA (Monólito, rollback ACID)"
cd "$MONOLITH_DIR/testes"
./testes-falha.sh
cd "$ROOT"

# ─── Falha — Microserviços (estado zombie) ──────────────────────────────────
banner "TESTE C2 — FALHA (Microserviços, estado zombie)"
cd "$MICRO_DIR/testes"
./testes-falha.sh
cd "$ROOT"

# ─── Resumo final ────────────────────────────────────────────────────────────
banner "RESUMO FINAL"

python3 - <<EOF
import json, os

def safe_load(path):
    try:
        with open(path) as f:
            return json.load(f)
    except Exception:
        return None

print("┌─ Velocidade ─────────────────────────────────────────────────")
for label, path in [
    ("T1 mono ", "$MONOLITH_DIR/testes/resultados/t-mono-1.json"),
    ("T1 micro", "$MICRO_DIR/testes/k6/output/t-micro-1.json"),
    ("T2 mono ", "$MONOLITH_DIR/testes/resultados/t-mono-2.json"),
    ("T2 micro", "$MICRO_DIR/testes/k6/output/t-micro-2.json"),
]:
    d = safe_load(path)
    if not d: continue
    it = d["metrics"]["iteration_duration"]
    its = d["metrics"]["iterations"]
    print(f"│  {label}: p50={it['med']:.2f}ms · p95={it['p(95)']:.2f}ms · "
          f"{int(its['count'])} iter")

print("└──────────────────────────────────────────────────────────────")
print()
print("Ficheiros para anexar ao relatório:")
print("  Monólito:  01-monolith/testes/resultados/")
print("  Micro:     02-microservices/testes/k6/output/")
EOF

echo
echo "✅ Pipeline completa terminada."

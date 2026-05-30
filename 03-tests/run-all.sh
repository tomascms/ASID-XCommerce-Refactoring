#!/bin/bash

target=$(echo "$1" | tr '[:upper:]' '[:lower:]')

if [[ "$target" != "mo" && "$target" != "ms" ]]; then
    echo "Erro: Parâmetro inválido ou ausente."
    echo "Uso: $0 [mo|ms]"
    echo " Opções disponíveis:"
    echo "  mo - Executar cenários para a arquitetura Monolítica"
    echo "  ms - Executar cenários para a arquitetura de Microserviços"
    exit 1
fi

export prefix=$target

TEST_DIR=$(dirname "$0")
OUTPUT_DIR="$TEST_DIR/output"

mkdir -p "$OUTPUT_DIR"

echo "==========================================="
echo "Iniciando execução de todos os testes k6"
echo "==========================================="

SUCCESS_COUNT=0
FAILED_COUNT=0
TIME_PREFIX=$(date +"%Y%m%d%H%M%S")
TEST_FILES=$(find "$TEST_DIR" -name "*.js" -not -path "*/node_modules/*" -not -path "*/common/*" -not -path "*/testes/*")

for FILE in $TEST_FILES; do
    clear
    FILENAME=$(basename "$FILE")
    echo "--- Executando: $FILENAME ---"

    k6 run --summary-export="$OUTPUT_DIR/${TIME_PREFIX}_${prefix}_${FILENAME%.js}.json" "$FILE"
    
    if [ $? -eq 0 ]; then
        echo "✅ $FILENAME finalizado com sucesso."
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "❌ $FILENAME falhou."
        FAILED_COUNT=$((FAILED_COUNT + 1))
        
    fi
    echo ""
done

echo "==========================================="
echo "Resumo das Execuções:"
echo "Sucessos: $SUCCESS_COUNT"
echo "Falhas:   $FAILED_COUNT"
echo "Relatórios gerados em: $OUTPUT_DIR"
echo "==========================================="

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

TEST_FILES=$(find "$TEST_DIR" -name "*.js" -not -path "*/node_modules/*" -not -path "*/common/*")
options=($TEST_FILES)

echo "Selecione o script k6 para execução individual:"
for i in "${!options[@]}"; do
    echo "$((i+1))) $(basename "${options[$i]}")"
done

read -p "Digite o número (1-${#options[@]}): " selection

if [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le "${#options[@]}" ]; then
    FILE="${options[$((selection-1))]}"
    FILENAME=$(basename "$FILE")
    TIME_PREFIX=$(date +"%Y%m%d%H%M%S")

    clear
    echo "--- Executando individualmente: $FILENAME ---"
    k6 run --summary-export="$OUTPUT_DIR/${TIME_PREFIX}_${prefix}_${FILENAME%.js}.json" "$FILE"
    
    if [ $? -eq 0 ]; then
        echo "✅ $FILENAME finalizado com sucesso."
    else
        echo "❌ $FILENAME falhou."
    fi
    echo "==========================================="
    echo "Relatório gerado em: $OUTPUT_DIR"
else
    echo "Erro: Seleção inválida."
    exit 1
fi

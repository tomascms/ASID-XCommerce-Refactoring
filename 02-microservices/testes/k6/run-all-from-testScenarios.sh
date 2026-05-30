#!/bin/bash
# Copy of original run-all.sh from testScenarios
$(cat << 'BASH'
#!/bin/bash

# Diretório base dos testes (onde o script está localizado)
TEST_DIR=$(dirname "$0")
OUTPUT_DIR="$TEST_DIR/output"

# Cria a pasta de output caso não exista
mkdir -p "$OUTPUT_DIR"

echo "==========================================="
echo "Iniciando execução de todos os testes k6"
echo "==========================================="

# Contador de sucessos e falhas
SUCCESS_COUNT=0
FAILED_COUNT=0

# Busca todos os arquivos .js recursivamente, exceto os que estão na pasta node_modules (se houver)
TEST_FILES=$(find "$TEST_DIR" -name "*.js" -not -path "*/node_modules/*")

for FILE in $TEST_FILES; do
    FILENAME=$(basename "$FILE")
    echo "--- Executando: $FILENAME ---"
    
    # Executa o k6 com a flag de exportação de sumário
    k6 run --summary-export="$OUTPUT_DIR/${FILENAME%.js}.json" "$FILE"
    
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
BASH
)

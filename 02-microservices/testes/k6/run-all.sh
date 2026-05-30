#!/bin/bash

# Consolidated runner for k6 scripts
TEST_DIR=$(dirname "$0")
OUTPUT_DIR="$TEST_DIR/output"
mkdir -p "$OUTPUT_DIR"

echo "Running all k6 tests in $TEST_DIR"
TEST_FILES=$(find "$TEST_DIR" -name "*.js" -not -path "*/node_modules/*")
for FILE in $TEST_FILES; do
  FILENAME=$(basename "$FILE")
  echo "--- Executing: $FILENAME ---"
  k6 run --summary-export="$OUTPUT_DIR/${FILENAME%.js}.json" "$FILE"
  if [ $? -eq 0 ]; then
    echo "✅ $FILENAME finished successfully."
  else
    echo "❌ $FILENAME failed."
  fi
  echo ""
done

echo "Reports: $OUTPUT_DIR"

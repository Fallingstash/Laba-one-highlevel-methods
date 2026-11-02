#!/bin/bash
# Сборка и запуск модульных тестов (JUnit 5). Код возврата не нулевой, если есть упавшие тесты.
# Аргументы передаются консольному запуску JUnit, например:
#   bash scripts/run-tests.sh --select-class domain.ResourcePathTest
set -e
cd "$(dirname "$0")/.."
source scripts/config.sh
bash scripts/build-tests.sh

java -jar "$JUNIT" execute \
    --class-path "build/tests.jar$SEP$CLI" \
    --scan-class-path build/tests.jar \
    --details=tree \
    --disable-ansi-colors \
    --reports-dir build/reports \
    "$@"

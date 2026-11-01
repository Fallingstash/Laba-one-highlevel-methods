#!/bin/bash
# Модульные тесты сценария CheckAccessUseCase с поддельными репозиториями (без процесса и без CLI).
set -e
cd "$(dirname "$0")/.."
source scripts/config.sh
bash scripts/fetch-deps.sh

rm -rf build/unit
mkdir -p build/unit
mapfile -t SOURCES < <(find src -name '*.kt')
kotlinc $KOTLINC_OPTS -cp "$CLI" "${SOURCES[@]}" tests/UseCaseTest.kt -include-runtime -d build/unit/tests.jar
java -cp build/unit/tests.jar UseCaseTest

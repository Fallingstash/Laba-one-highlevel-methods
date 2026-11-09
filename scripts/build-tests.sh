#!/bin/bash
# Сборка модульных тестов: исходники приложения + tests/ -> build/tests.jar
# Дополнительные опции компилятора: KOTLINC_OPTS="..." bash scripts/build-tests.sh
set -e
cd "$(dirname "$0")/.."
source scripts/config.sh
bash scripts/fetch-deps.sh all

rm -f build/tests.jar
mkdir -p build

mapfile -t SOURCES < <(find src -name '*.kt')
mapfile -t TESTS < <(find tests -name '*.kt')
kotlinc $KOTLINC_OPTS -cp "$CLI$SEP$JUNIT$SEP$H2" "${SOURCES[@]}" "${TESTS[@]}" -include-runtime -d build/tests.jar

echo "Собрано: build/tests.jar"

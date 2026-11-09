#!/bin/bash
# Скачивает зависимости в lib/, если их там нет. Можно запускать повторно.
#   bash scripts/fetch-deps.sh app    kotlinx-cli и драйвер H2 (для сборки и запуска приложения)
#   bash scripts/fetch-deps.sh h2     только H2 (для init-db.sh)
#   bash scripts/fetch-deps.sh junit  только JUnit (для тестов)
#   bash scripts/fetch-deps.sh all    всё
# Зеркало Maven можно подменить: MAVEN_BASE=<адрес> (структура каталогов как в Maven Central).
set -e
cd "$(dirname "$0")/.."
source scripts/config.sh

# fetch <файл> <адрес>
fetch() {
    local target="$1" url="$2"
    [ -f "$target" ] && return 0

    echo "Скачиваю $(basename "$target") ..."
    mkdir -p lib
    curl -fsSL -o "$target.tmp" "$url"

    # проверка целостности по SHA-1, который публикует Maven Central рядом с jar
    local expected actual
    expected=$(curl -fsSL "$url.sha1" | awk '{print $1}')
    actual=$(sha1sum "$target.tmp" | awk '{print $1}')
    if [ "$expected" != "$actual" ]; then
        rm -f "$target.tmp"
        echo "Контрольная сумма не совпала для $(basename "$target"): ожидали $expected, получили $actual" >&2
        exit 1
    fi

    mv "$target.tmp" "$target"
}

case "${1:-app}" in
    app)   fetch "$CLI" "$CLI_URL"; fetch "$H2" "$H2_URL" ;;
    h2)    fetch "$H2" "$H2_URL" ;;
    junit) fetch "$JUNIT" "$JUNIT_URL" ;;
    all)   fetch "$CLI" "$CLI_URL"; fetch "$H2" "$H2_URL"; fetch "$JUNIT" "$JUNIT_URL" ;;
    *)     echo "Использование: fetch-deps.sh [app|h2|junit|all]" >&2; exit 2 ;;
esac

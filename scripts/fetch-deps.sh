#!/bin/bash
# Скачивает kotlinx-cli в lib/, если его там нет. Можно запускать повторно.
# Адрес можно переопределить: KOTLINX_CLI_BASE_URL=<каталог с jar и jar.sha1>
set -e
cd "$(dirname "$0")/.."
source scripts/config.sh

[ -f "$CLI" ] && exit 0

BASE_URL="${KOTLINX_CLI_BASE_URL:-https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-cli-jvm/$CLI_VERSION}"
URL="$BASE_URL/kotlinx-cli-jvm-$CLI_VERSION.jar"

echo "Скачиваю kotlinx-cli $CLI_VERSION ..."
mkdir -p lib
curl -fsSL -o "$CLI.tmp" "$URL"

# проверка целостности по SHA-1, который публикует Maven Central рядом с jar
expected=$(curl -fsSL "$URL.sha1" | awk '{print $1}')
actual=$(sha1sum "$CLI.tmp" | awk '{print $1}')
if [ "$expected" != "$actual" ]; then
    rm -f "$CLI.tmp"
    echo "Контрольная сумма kotlinx-cli не совпала: ожидали $expected, получили $actual" >&2
    exit 1
fi

mv "$CLI.tmp" "$CLI"
echo "Скачано: $CLI"

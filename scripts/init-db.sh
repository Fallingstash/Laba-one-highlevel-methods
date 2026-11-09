#!/bin/bash
# Создаёт файл базы H2 и заполняет его: scripts/init.sql (схема), затем scripts/fill.sql (данные).
# Существующая база с тем же именем удаляется. Путь по умолчанию ./data/access,
# другой задаётся переменной ACCESS_DB_PATH (без расширения).
set -e
cd "$(dirname "$0")/.."
source scripts/config.sh
bash scripts/fetch-deps.sh h2

mkdir -p "$(dirname "$DB_PATH")"
rm -f "$DB_PATH.mv.db" "$DB_PATH.trace.db"

for script in scripts/init.sql scripts/fill.sql; do
    java -cp "$H2" org.h2.tools.RunScript -url "jdbc:h2:file:$DB_PATH" -user sa -password "" -script "$script"
done

echo "База создана: $DB_PATH.mv.db"

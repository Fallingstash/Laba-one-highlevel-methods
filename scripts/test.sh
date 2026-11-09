#!/bin/bash
# Сквозные тесты: создают тестовую базу, запускают app.jar и проверяют код ответа.
cd "$(dirname "$0")/.."
source scripts/config.sh

TEST_DB_DIR=./build/test-db
export ACCESS_DB_PATH="$TEST_DB_DIR/access"

# обычная база (схема + данные) и «пустая» база без таблиц (для ошибки SQL)
ACCESS_DB_PATH="$TEST_DB_DIR/access" bash scripts/init-db.sh >/dev/null
rm -f "$TEST_DB_DIR/empty.mv.db"
printf 'SELECT 1;\n' > "$TEST_DB_DIR/empty.sql"
java -cp "$H2" org.h2.tools.RunScript -url "jdbc:h2:file:$TEST_DB_DIR/empty" -user sa -password "" -script "$TEST_DB_DIR/empty.sql"

pass=0
total=0

# check <название> <ожидаемый код> <аргументы программы...>
# Перед вызовом можно задать окружение: ACCESS_DB_PATH=... check ...
check() {
    local name="$1" expected="$2"
    shift 2
    java -jar app.jar "$@" >/dev/null 2>&1
    local code=$?
    total=$((total + 1))
    if [ "$code" -eq "$expected" ]; then
        echo "OK   $name (код $code)"
        pass=$((pass + 1))
    else
        echo "FAIL $name (ожидали $expected, получили $code)"
    fi
}

# по одному кейсу на каждый код ответа 0-10
check "успех"                 0 --login alice --password qwerty  --action read   --resource A.B.C --volume 10
check "справка"               1 --help
check "неверный пароль"       2 --login alice --password wrong   --action read   --resource A.B.C --volume 10
check "неверный логин"        3 --login nobody --password qwerty --action read   --resource A.B.C --volume 10
check "неизвестное действие"  4 --login alice --password qwerty  --action delete --resource A.B.C --volume 10
check "нет доступа"           5 --login bob   --password hunter2 --action write  --resource A.B   --volume 10
check "несуществующий ресурс" 6 --login alice --password qwerty  --action read   --resource A.X   --volume 10
check "неверный формат пути"  7 --login alice --password qwerty  --action read   --resource A..B  --volume 10
check "превышение объёма"     8 --login alice --password qwerty  --action read   --resource A.B.C --volume 21
ACCESS_DB_PATH="$TEST_DB_DIR/missing" \
check "нет файла базы"        9 --login alice --password qwerty  --action read   --resource A.B.C --volume 10
ACCESS_DB_PATH="$TEST_DB_DIR/empty" \
check "ошибка SQL-запроса"   10 --login alice --password qwerty  --action read   --resource A.B.C --volume 10

# неверный формат запуска: справка приложения и код 1, а не 127 из kotlinx-cli
check "неизвестный флаг"      1 --bogus
check "не все флаги"          1 --login alice
# объём проверяется приложением, а не парсером
check "объём не число"        7 --login alice --password qwerty  --action read   --resource A.B.C --volume abc
check "объём отрицательный"   7 --login alice --password qwerty  --action read   --resource A.B.C --volume -5
# справке база не нужна
ACCESS_DB_PATH="$TEST_DB_DIR/missing" \
check "справка без базы"      1 --help

echo "$pass/$total"
[ "$pass" -eq "$total" ]

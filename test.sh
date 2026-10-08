#!/bin/bash
cd "$(dirname "$0")"

pass=0
total=0

# check <название> <ожидаемый код> <аргументы программы...>
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

check "успех"                    0 --login alice --password qwerty --action read  --resource A.B.C --volume 10
check "справка"                  1 --help
check "неверный пароль"          2 --login alice --password wrong  --action read  --resource A.B.C --volume 10
check "неверный логин"           3 --login nobody --password qwerty --action read --resource A.B.C --volume 10
check "неизвестное действие"     4 --login alice --password qwerty --action delete --resource A.B.C --volume 10
check "нет доступа"              5 --login bob   --password hunter2 --action write --resource A.B --volume 10
check "несуществующий ресурс"    6 --login alice --password qwerty --action read  --resource A.X   --volume 10
check "неверный формат"          7 --login alice --password qwerty --action read  --resource A..B  --volume 10
check "превышение объёма"        8 --login alice --password qwerty --action read  --resource A.B.C --volume 21

echo "$pass/$total"
[ "$pass" -eq "$total" ]
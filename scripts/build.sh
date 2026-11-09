#!/bin/bash
# Сборка app.jar. Дополнительные опции компилятора можно передать через KOTLINC_OPTS,
# например: KOTLINC_OPTS="-jvm-target 17" bash scripts/build.sh
set -e
cd "$(dirname "$0")/.."
source scripts/config.sh
bash scripts/fetch-deps.sh app

rm -rf build/classes build/app-tmp.jar app.jar
mkdir -p build/classes

# все исходники приложения; tools/ и tests/ в сборку не входят.
# Драйвер H2 для компиляции не нужен: код использует только java.sql, драйвер подхватывается при запуске.
mapfile -t SOURCES < <(find src -name '*.kt')
kotlinc $KOTLINC_OPTS -cp "$CLI" "${SOURCES[@]}" -include-runtime -d build/app-tmp.jar

# «толстый» jar: наш код + stdlib + kotlinx-cli + драйвер H2
(cd build/classes && jar xf ../app-tmp.jar && jar xf "../../$CLI" && jar xf "../../$H2")
rm -f build/classes/META-INF/MANIFEST.MF
jar cfe app.jar MainKt -C build/classes .

echo "Собрано: app.jar"

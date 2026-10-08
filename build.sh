#!/bin/bash
set -e
cd "$(dirname "$0")"

CLI=lib/kotlinx-cli-jvm-0.3.6.jar

rm -rf build app.jar
mkdir -p build/classes

# GenHash.kt в сборку не входит: это вспомогательный инструмент
kotlinc -cp "$CLI" Models.kt Auth.kt Access.kt Data.kt Main.kt \
  -include-runtime -d build/app-tmp.jar

(cd build/classes && jar xf ../app-tmp.jar && jar xf "../../$CLI")
rm -f build/classes/META-INF/MANIFEST.MF
jar cfe app.jar MainKt -C build/classes .

echo "Собрано: app.jar"
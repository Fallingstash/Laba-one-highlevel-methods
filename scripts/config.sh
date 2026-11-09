# Общие настройки скриптов (подключается через source из корня проекта)
CLI_VERSION=0.3.6
H2_VERSION=2.2.220
JUNIT_VERSION=1.12.2

CLI="lib/kotlinx-cli-jvm-$CLI_VERSION.jar"
H2="lib/h2-$H2_VERSION.jar"
JUNIT="lib/junit-platform-console-standalone-$JUNIT_VERSION.jar"

MAVEN_BASE="${MAVEN_BASE:-https://repo1.maven.org/maven2}"
CLI_URL="$MAVEN_BASE/org/jetbrains/kotlinx/kotlinx-cli-jvm/$CLI_VERSION/kotlinx-cli-jvm-$CLI_VERSION.jar"
H2_URL="$MAVEN_BASE/com/h2database/h2/$H2_VERSION/h2-$H2_VERSION.jar"
JUNIT_URL="$MAVEN_BASE/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"

# Путь к файлу базы H2 (без расширения). Тот же переменной пользуется и приложение.
DB_PATH="${ACCESS_DB_PATH:-./data/access}"
# H2 принимает только пути вида ./x, ../x, /x, ~/x, C:/x
case "$DB_PATH" in
    /*|./*|../*|~*|[A-Za-z]:*) ;;
    *) DB_PATH="./$DB_PATH" ;;
esac

# Разделитель путей в classpath: ';' в Windows (Git Bash), ':' в Linux/macOS
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
    *) SEP=":" ;;
esac

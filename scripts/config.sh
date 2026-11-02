# Общие настройки скриптов (подключается через source из корня проекта)
CLI_VERSION=0.3.6
JUNIT_VERSION=1.12.2

CLI="lib/kotlinx-cli-jvm-$CLI_VERSION.jar"
JUNIT="lib/junit-platform-console-standalone-$JUNIT_VERSION.jar"

MAVEN_BASE="${MAVEN_BASE:-https://repo1.maven.org/maven2}"
CLI_URL="$MAVEN_BASE/org/jetbrains/kotlinx/kotlinx-cli-jvm/$CLI_VERSION/kotlinx-cli-jvm-$CLI_VERSION.jar"
JUNIT_URL="$MAVEN_BASE/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"

# Разделитель путей в classpath: ';' в Windows (Git Bash), ':' в Linux/macOS
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
    *) SEP=":" ;;
esac

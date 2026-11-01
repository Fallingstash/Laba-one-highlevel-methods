package adapter.cli

enum class ExitCode(val code: Int) {
    OK(0),
    HELP(1),
    WRONG_PASSWORD(2),
    WRONG_LOGIN(3),
    UNKNOWN_ACTION(4),
    NO_ACCESS(5),
    NO_RESOURCE(6),
    BAD_FORMAT(7),
    VOLUME_EXCEEDED(8)
}

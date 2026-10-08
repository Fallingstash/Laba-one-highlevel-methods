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

enum class Action(val flag: String) {
    READ("read"),
    WRITE("write"),
    EXECUTE("execute");

    companion object {
        fun parse(raw: String): Action? = values().firstOrNull { it.flag == raw }
    }
}

// Право на путь (и всё поддерево под ним)
data class Grant(val path: List<String>, val actions: Set<Action>)

data class User(
    val login: String,
    val saltHex: String,
    val hashHex: String,
    val grants: List<Grant>
)

// Узел дерева ресурсов
data class Resource(
    val name: String,
    val maxVolume: Int,
    val children: Map<String, Resource> = emptyMap()
)
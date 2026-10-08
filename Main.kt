import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlin.system.exitProcess

private const val HELP = """
Использование:
  java -jar app.jar --login <логин> --password <пароль> --action <действие> --resource <путь> --volume <объём>

Аргументы:
  --login      логин пользователя
  --password   пароль пользователя
  --action     действие: read, write или execute
  --resource   путь до ресурса через точки, например A.B.C
  --volume     запрашиваемый объём (целое неотрицательное число)
  -h, --help   показать эту справку

Коды ответа:
  0 успех, 1 справка, 2 неверный пароль, 3 неверный логин, 4 неизвестное действие,
  5 нет доступа, 6 ресурс не существует, 7 неверный формат ресурса или объёма,
  8 превышен максимальный объём
"""

// Все значения читаем как строки: типы и формат проверяем сами, чтобы получить нужный код ответа
private class Request(
    val login: String,
    val password: String,
    val action: String,
    val resource: String,
    val volume: String
)

private fun parseArgs(args: Array<String>): Request? {
    val parser = ArgParser("app")
    val login by parser.option(ArgType.String, fullName = "login", description = "Логин").required()
    val password by parser.option(ArgType.String, fullName = "password", description = "Пароль").required()
    val action by parser.option(ArgType.String, fullName = "action", description = "Действие").required()
    val resource by parser.option(ArgType.String, fullName = "resource", description = "Путь до ресурса").required()
    val volume by parser.option(ArgType.String, fullName = "volume", description = "Объём").required()

    return try {
        parser.parse(args)
        Request(login, password, action, resource, volume)
    } catch (e: Exception) {
        null   // неверный формат запуска
    }
}

private fun execute(args: Array<String>): ExitCode {
    if (args.any { it == "-h" || it == "--help" }) {
        println(HELP)
        return ExitCode.HELP
    }

    val req = parseArgs(args)
    if (req == null) {
        println(HELP)            // при неверном формате запуска ТЗ требует показать справку
        return ExitCode.HELP
    }

    val user = Data.users[req.login] ?: return ExitCode.WRONG_LOGIN
    if (!Auth.verify(req.password, user)) return ExitCode.WRONG_PASSWORD
    val action = Action.parse(req.action) ?: return ExitCode.UNKNOWN_ACTION

    val path = Access.parsePath(req.resource) ?: return ExitCode.BAD_FORMAT
    val volume = req.volume.toIntOrNull()?.takeIf { it >= 0 } ?: return ExitCode.BAD_FORMAT

    val resource = Access.find(Data.root, path) ?: return ExitCode.NO_RESOURCE
    if (!Access.isAllowed(user, path, action)) return ExitCode.NO_ACCESS
    if (volume > resource.maxVolume) return ExitCode.VOLUME_EXCEEDED

    return ExitCode.OK
}

fun main(args: Array<String>) {
    exitProcess(execute(args).code)
}
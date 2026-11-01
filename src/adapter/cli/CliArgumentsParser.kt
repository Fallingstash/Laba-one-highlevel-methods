package adapter.cli

import application.AccessRequest
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

/**
 * kotlinx-cli при ошибке аргументов сам завершает процесс (код 127), поэтому
 * сначала мы проверяем «форму» аргументов сами и отдаём библиотеке только заведомо верные.
 */
object CliArgumentsParser {
    private val REQUIRED_FLAGS = setOf("--login", "--password", "--action", "--resource", "--volume")

    fun parse(args: Array<String>): CliCommand {
        if (args.any { it == "-h" || it == "--help" }) return CliCommand.ShowHelp
        if (!hasValidShape(args)) return CliCommand.ShowHelp

        // Значения читаем как строки: типы и формат проверяет сценарий (коды 4 и 7).
        val parser = ArgParser("app")
        val login by parser.option(ArgType.String, fullName = "login", description = "Логин").required()
        val password by parser.option(ArgType.String, fullName = "password", description = "Пароль").required()
        val action by parser.option(ArgType.String, fullName = "action", description = "Действие").required()
        val resource by parser.option(ArgType.String, fullName = "resource", description = "Путь до ресурса").required()
        val volume by parser.option(ArgType.String, fullName = "volume", description = "Объём").required()
        parser.parse(args)

        return CliCommand.Check(AccessRequest(login, password, action, resource, volume))
    }

    /** Ровно пять пар «флаг значение», все пять флагов обязательные и разные. */
    private fun hasValidShape(args: Array<String>): Boolean {
        if (args.size != REQUIRED_FLAGS.size * 2) return false
        val flags = args.filterIndexed { index, _ -> index % 2 == 0 }
        return flags.toSet() == REQUIRED_FLAGS
    }
}

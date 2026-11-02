package adapter.cli

import application.CheckAccessUseCase
import java.io.PrintStream

/**
 * Весь сценарий запуска «аргументы -> код ответа» без завершения процесса.
 * Вывод (только справка) идёт в [out], поэтому его можно подменить в тестах.
 */
class CliRunner(
    private val useCase: CheckAccessUseCase,
    private val out: PrintStream = System.out
) {
    fun run(args: Array<String>): ExitCode = when (val command = CliArgumentsParser.parse(args)) {
        is CliCommand.ShowHelp -> {
            out.println(HelpPrinter.text())
            ExitCode.HELP
        }
        is CliCommand.Check -> ExitCodeMapper.map(useCase.check(command.request))
    }
}

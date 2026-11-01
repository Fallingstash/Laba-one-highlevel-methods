import adapter.cli.CliArgumentsParser
import adapter.cli.CliCommand
import adapter.cli.ExitCode
import adapter.cli.ExitCodeMapper
import adapter.cli.HelpPrinter
import application.CheckAccessUseCase
import infrastructure.InMemoryResourceRepository
import infrastructure.InMemoryUserRepository
import infrastructure.Sha256PasswordHasher
import kotlin.system.exitProcess

/** Точка входа и composition root: единственное место, где создаются конкретные объекты. */
fun main(args: Array<String>) {
    val useCase = CheckAccessUseCase(
        users = InMemoryUserRepository(),
        resources = InMemoryResourceRepository(),
        hasher = Sha256PasswordHasher()
    )

    val exitCode = when (val command = CliArgumentsParser.parse(args)) {
        is CliCommand.ShowHelp -> {
            HelpPrinter.print()
            ExitCode.HELP
        }
        is CliCommand.Check -> ExitCodeMapper.map(useCase.check(command.request))
    }

    exitProcess(exitCode.code)
}

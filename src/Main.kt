import adapter.cli.CliRunner
import application.CheckAccessUseCase
import infrastructure.InMemoryResourceRepository
import infrastructure.InMemoryUserRepository
import infrastructure.Sha256PasswordHasher
import kotlin.system.exitProcess

/** Точка входа и composition root: создаёт объекты, запускает сценарий и завершает процесс. */
fun main(args: Array<String>) {
    val useCase = CheckAccessUseCase(
        users = InMemoryUserRepository(),
        resources = InMemoryResourceRepository(),
        hasher = Sha256PasswordHasher()
    )
    exitProcess(CliRunner(useCase).run(args).code)
}

import adapter.cli.CliRunner
import application.CheckAccessUseCase
import infrastructure.Sha256PasswordHasher
import infrastructure.jdbc.Database
import infrastructure.jdbc.DatabaseUrls
import infrastructure.jdbc.JdbcResourceRepository
import infrastructure.jdbc.JdbcUserRepository
import kotlin.system.exitProcess

private const val DEFAULT_DB_PATH = "./data/access"

/** Точка входа и composition root: создаёт объекты, запускает сценарий, закрывает базу и завершает процесс. */
fun main(args: Array<String>) {
    val dbPath = System.getenv("ACCESS_DB_PATH") ?: DEFAULT_DB_PATH

    // exitProcess вызывается ПОСЛЕ use, иначе соединение с базой не закрылось бы
    val exitCode = Database(DatabaseUrls.h2File(dbPath)).use { database ->
        val useCase = CheckAccessUseCase(
            users = JdbcUserRepository(database),
            resources = JdbcResourceRepository(database),
            hasher = Sha256PasswordHasher()
        )
        CliRunner(useCase).run(args)
    }

    exitProcess(exitCode.code)
}

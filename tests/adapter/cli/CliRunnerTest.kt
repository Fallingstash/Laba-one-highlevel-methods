package adapter.cli

import application.CheckAccessUseCase
import infrastructure.Sha256PasswordHasher
import infrastructure.jdbc.Database
import infrastructure.jdbc.DatabaseUrls
import infrastructure.jdbc.JdbcResourceRepository
import infrastructure.jdbc.JdbcUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import support.TestDatabase
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files

/**
 * Сквозные проверки «аргументы -> код ответа» внутри процесса: настоящий JDBC и H2 в памяти
 * со схемой и данными из scripts/init.sql и scripts/fill.sql, без exitProcess.
 */
class CliRunnerTest {

    private val buffer = ByteArrayOutputStream()

    private fun runnerFor(database: Database) = CliRunner(
        CheckAccessUseCase(
            JdbcUserRepository(database), JdbcResourceRepository(database), Sha256PasswordHasher()
        ),
        PrintStream(buffer, true, "UTF-8")
    )

    private val runner = runnerFor(TestDatabase.filled())

    private fun request(
        login: String = "alice", password: String = "qwerty", action: String = "read",
        resource: String = "A.B.C", volume: String = "10"
    ) = arrayOf(
        "--login", login, "--password", password, "--action", action,
        "--resource", resource, "--volume", volume
    )

    private fun output() = buffer.toString("UTF-8")

    private fun missingDatabase() =
        Database(DatabaseUrls.h2File(Files.createTempDirectory("lab-db").resolve("missing").toString()))

    // ---------- по одному сценарию на каждый код ответа ----------

    @Test
    fun shouldReturnOkWhenAccessIsGranted() {
        assertEquals(ExitCode.OK, runner.run(request()))
    }

    @Test
    fun shouldReturnHelpCodeWhenHelpIsRequested() {
        assertEquals(ExitCode.HELP, runner.run(arrayOf("--help")))
    }

    @Test
    fun shouldReturnWrongPasswordWhenPasswordIsIncorrect() {
        assertEquals(ExitCode.WRONG_PASSWORD, runner.run(request(password = "wrong")))
    }

    @Test
    fun shouldReturnWrongLoginWhenLoginIsUnknown() {
        assertEquals(ExitCode.WRONG_LOGIN, runner.run(request(login = "nobody")))
    }

    @Test
    fun shouldReturnUnknownActionWhenActionIsNotSupported() {
        assertEquals(ExitCode.UNKNOWN_ACTION, runner.run(request(action = "delete")))
    }

    @Test
    fun shouldReturnNoAccessWhenUserHasNoSuchPermission() {
        assertEquals(ExitCode.NO_ACCESS, runner.run(request(login = "bob", password = "hunter2", action = "write", resource = "A.B")))
    }

    @Test
    fun shouldReturnNoResourceWhenResourceDoesNotExist() {
        assertEquals(ExitCode.NO_RESOURCE, runner.run(request(resource = "A.X")))
    }

    @Test
    fun shouldReturnBadFormatWhenResourcePathIsMalformed() {
        assertEquals(ExitCode.BAD_FORMAT, runner.run(request(resource = "A..B")))
    }

    @Test
    fun shouldReturnVolumeExceededWhenVolumeIsAboveMaximum() {
        assertEquals(ExitCode.VOLUME_EXCEEDED, runner.run(request(volume = "21")))
    }

    @Test
    fun shouldReturnDbConnectionErrorWhenDatabaseFileDoesNotExist() {
        assertEquals(ExitCode.DB_CONNECTION_ERROR, runnerFor(missingDatabase()).run(request()))
    }

    @Test
    fun shouldReturnDbQueryErrorWhenTablesAreMissing() {
        assertEquals(ExitCode.DB_QUERY_ERROR, runnerFor(TestDatabase.empty()).run(request()))
    }

    // ---------- граничные случаи и «тихая» работа ----------

    @Test
    fun shouldReturnBadFormatWhenVolumeIsNegative() {
        assertEquals(ExitCode.BAD_FORMAT, runner.run(request(volume = "-5")))
    }

    @Test
    fun shouldReturnHelpCodeWhenArgumentsAreMalformed() {
        assertEquals(ExitCode.HELP, runner.run(arrayOf("--bogus")))
    }

    @Test
    fun shouldPrintNothingWhenAccessIsGranted() {
        runner.run(request())
        assertEquals("", output())
    }

    @Test
    fun shouldPrintNothingWhenAccessIsDenied() {
        runner.run(request(password = "wrong"))
        assertEquals("", output())
    }

    @Test
    fun shouldPrintNothingWhenDatabaseIsUnreachable() {
        runnerFor(missingDatabase()).run(request())
        assertEquals("", output())
    }

    @Test
    fun shouldPrintNothingWhenSqlQueryFails() {
        runnerFor(TestDatabase.empty()).run(request())
        assertEquals("", output())
    }

    @Test
    fun shouldNotTouchDatabaseWhenHelpIsRequested() {
        assertEquals(ExitCode.HELP, runnerFor(missingDatabase()).run(arrayOf("-h")))
    }

    @Test
    fun shouldPrintHelpTextWhenHelpIsRequested() {
        runner.run(arrayOf("-h"))
        assertEquals(HelpPrinter.text(), output().trim())
    }

    @Test
    fun shouldPrintApplicationHelpWhenArgumentsAreMalformed() {
        runner.run(arrayOf("--login", "alice"))
        assertEquals(HelpPrinter.text(), output().trim())
    }
}

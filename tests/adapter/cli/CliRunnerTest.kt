package adapter.cli

import application.CheckAccessUseCase
import infrastructure.InMemoryResourceRepository
import infrastructure.InMemoryUserRepository
import infrastructure.Sha256PasswordHasher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/** Сквозные проверки «аргументы -> код ответа» внутри процесса: реальные данные, без exitProcess. */
class CliRunnerTest {

    private val buffer = ByteArrayOutputStream()
    private val runner = CliRunner(
        CheckAccessUseCase(InMemoryUserRepository(), InMemoryResourceRepository(), Sha256PasswordHasher()),
        PrintStream(buffer, true, "UTF-8")
    )

    private fun run(vararg args: String): ExitCode = runner.run(arrayOf(*args))

    private fun request(
        login: String = "alice", password: String = "qwerty", action: String = "read",
        resource: String = "A.B.C", volume: String = "10"
    ) = arrayOf(
        "--login", login, "--password", password, "--action", action,
        "--resource", resource, "--volume", volume
    )

    private fun output() = buffer.toString("UTF-8")

    // ---------- по одному сценарию на каждый код ответа ----------

    @Test
    fun shouldReturnOkWhenAccessIsGranted() {
        assertEquals(ExitCode.OK, runner.run(request()))
    }

    @Test
    fun shouldReturnHelpCodeWhenHelpIsRequested() {
        assertEquals(ExitCode.HELP, run("--help"))
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

    // ---------- граничные случаи и «тихая» работа ----------

    @Test
    fun shouldReturnBadFormatWhenVolumeIsNegative() {
        assertEquals(ExitCode.BAD_FORMAT, runner.run(request(volume = "-5")))
    }

    @Test
    fun shouldReturnHelpCodeWhenArgumentsAreMalformed() {
        assertEquals(ExitCode.HELP, run("--bogus"))
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
    fun shouldPrintHelpTextWhenHelpIsRequested() {
        run("-h")
        assertEquals(HelpPrinter.text(), output().trim())
    }

    @Test
    fun shouldPrintApplicationHelpWhenArgumentsAreMalformed() {
        run("--login", "alice")
        assertEquals(HelpPrinter.text(), output().trim())
    }
}

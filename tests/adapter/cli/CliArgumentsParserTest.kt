package adapter.cli

import application.AccessRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CliArgumentsParserTest {

    private fun validArgs(
        login: String = "alice", password: String = "qwerty", action: String = "read",
        resource: String = "A.B.C", volume: String = "10"
    ) = arrayOf(
        "--login", login, "--password", password, "--action", action,
        "--resource", resource, "--volume", volume
    )

    private fun parse(vararg args: String) = CliArgumentsParser.parse(arrayOf(*args))

    // ---------- корректные аргументы ----------

    @Test
    fun shouldReturnCheckCommandWhenAllFlagsAreProvided() {
        val command = CliArgumentsParser.parse(validArgs())
        assertEquals(CliCommand.Check(AccessRequest("alice", "qwerty", "read", "A.B.C", "10")), command)
    }

    @Test
    fun shouldAcceptFlagsInAnyOrder() {
        val command = parse(
            "--volume", "10", "--resource", "A.B.C", "--action", "read",
            "--password", "qwerty", "--login", "alice"
        )
        assertEquals(CliCommand.Check(AccessRequest("alice", "qwerty", "read", "A.B.C", "10")), command)
    }

    @Test
    fun shouldKeepValuesAsRawStringsWhenVolumeIsNotANumber() {
        val command = CliArgumentsParser.parse(validArgs(volume = "abc"))
        assertEquals(CliCommand.Check(AccessRequest("alice", "qwerty", "read", "A.B.C", "abc")), command)
    }

    @Test
    fun shouldPassUnknownActionToUseCaseInsteadOfRejectingIt() {
        val command = CliArgumentsParser.parse(validArgs(action = "delete"))
        assertEquals(CliCommand.Check(AccessRequest("alice", "qwerty", "delete", "A.B.C", "10")), command)
    }

    @Test
    fun shouldAcceptNegativeVolumeAsValue() {
        val command = CliArgumentsParser.parse(validArgs(volume = "-5"))
        assertEquals(CliCommand.Check(AccessRequest("alice", "qwerty", "read", "A.B.C", "-5")), command)
    }

    @Test
    fun shouldAcceptPasswordWithSpacesAndSpecialCharacters() {
        val command = CliArgumentsParser.parse(validArgs(password = "p@ss w0rd!"))
        assertEquals(CliCommand.Check(AccessRequest("alice", "p@ss w0rd!", "read", "A.B.C", "10")), command)
    }

    @Test
    fun shouldAcceptEmptyValues() {
        val command = CliArgumentsParser.parse(validArgs(login = ""))
        assertEquals(CliCommand.Check(AccessRequest("", "qwerty", "read", "A.B.C", "10")), command)
    }

    // ---------- справка ----------

    @Test
    fun shouldReturnHelpWhenShortHelpFlagIsUsed() {
        assertEquals(CliCommand.ShowHelp, parse("-h"))
    }

    @Test
    fun shouldReturnHelpWhenLongHelpFlagIsUsed() {
        assertEquals(CliCommand.ShowHelp, parse("--help"))
    }

    @Test
    fun shouldReturnHelpWhenHelpFlagIsMixedWithValidArguments() {
        assertEquals(CliCommand.ShowHelp, CliArgumentsParser.parse(validArgs() + "--help"))
    }

    @Test
    fun shouldReturnHelpWhenPasswordValueIsHelpFlag() {
        // зафиксированное ограничение: значение -h воспринимается как запрос справки
        assertEquals(CliCommand.ShowHelp, CliArgumentsParser.parse(validArgs(password = "-h")))
    }

    // ---------- неверный формат запуска (раньше давал код 127 из kotlinx-cli) ----------

    @Test
    fun shouldReturnHelpWhenThereAreNoArguments() {
        assertEquals(CliCommand.ShowHelp, parse())
    }

    @Test
    fun shouldReturnHelpWhenFlagIsUnknown() {
        assertEquals(CliCommand.ShowHelp, parse("--bogus"))
    }

    @Test
    fun shouldReturnHelpWhenOnlyLoginIsProvided() {
        assertEquals(CliCommand.ShowHelp, parse("--login", "alice"))
    }

    @Test
    fun shouldReturnHelpWhenLastValueIsMissing() {
        assertEquals(CliCommand.ShowHelp, CliArgumentsParser.parse(validArgs().copyOf(9).requireNoNulls()))
    }

    @Test
    fun shouldReturnHelpWhenFlagIsRepeatedAndAnotherIsMissing() {
        val args = arrayOf(
            "--login", "alice", "--login", "bob", "--password", "qwerty",
            "--action", "read", "--resource", "A.B.C"
        )
        assertEquals(CliCommand.ShowHelp, CliArgumentsParser.parse(args))
    }

    @Test
    fun shouldReturnHelpWhenThereAreExtraArguments() {
        assertEquals(CliCommand.ShowHelp, CliArgumentsParser.parse(validArgs() + arrayOf("--extra", "x")))
    }

    @Test
    fun shouldReturnHelpWhenEqualsSignFormIsUsed() {
        val args = arrayOf(
            "--login=alice", "--password=qwerty", "--action=read",
            "--resource=A.B.C", "--volume=10"
        )
        assertEquals(CliCommand.ShowHelp, CliArgumentsParser.parse(args))
    }

    @Test
    fun shouldReturnHelpWhenValuesStandInPlaceOfFlags() {
        val args = arrayOf("alice", "--login", "qwerty", "--password", "read", "--action", "A.B.C", "--resource", "10", "--volume")
        assertEquals(CliCommand.ShowHelp, CliArgumentsParser.parse(args))
    }
}

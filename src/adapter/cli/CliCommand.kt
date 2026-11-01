package adapter.cli

import application.AccessRequest

sealed interface CliCommand {
    data object ShowHelp : CliCommand
    data class Check(val request: AccessRequest) : CliCommand
}

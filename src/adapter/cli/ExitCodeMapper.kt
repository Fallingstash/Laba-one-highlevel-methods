package adapter.cli

import application.AccessResult
import application.DenyReason
import application.port.StorageFailureKind

object ExitCodeMapper {
    fun map(result: AccessResult): ExitCode = when (result) {
        is AccessResult.Granted -> ExitCode.OK
        is AccessResult.Denied -> when (result.reason) {
            DenyReason.UNKNOWN_LOGIN -> ExitCode.WRONG_LOGIN
            DenyReason.WRONG_PASSWORD -> ExitCode.WRONG_PASSWORD
            DenyReason.UNKNOWN_ACTION -> ExitCode.UNKNOWN_ACTION
            DenyReason.INVALID_FORMAT -> ExitCode.BAD_FORMAT
            DenyReason.RESOURCE_NOT_FOUND -> ExitCode.NO_RESOURCE
            DenyReason.ACCESS_DENIED -> ExitCode.NO_ACCESS
            DenyReason.VOLUME_EXCEEDED -> ExitCode.VOLUME_EXCEEDED
        }
        is AccessResult.StorageFailure -> when (result.kind) {
            StorageFailureKind.CONNECTION -> ExitCode.DB_CONNECTION_ERROR
            StorageFailureKind.QUERY -> ExitCode.DB_QUERY_ERROR
        }
    }
}

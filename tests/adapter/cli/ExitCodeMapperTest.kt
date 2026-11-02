package adapter.cli

import application.AccessResult
import application.DenyReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ExitCodeMapperTest {

    private fun mapDenied(reason: DenyReason) = ExitCodeMapper.map(AccessResult.Denied(reason))

    @Test
    fun shouldMapGrantedToOk() {
        assertEquals(ExitCode.OK, ExitCodeMapper.map(AccessResult.Granted))
    }

    @Test
    fun shouldMapUnknownLoginToWrongLogin() {
        assertEquals(ExitCode.WRONG_LOGIN, mapDenied(DenyReason.UNKNOWN_LOGIN))
    }

    @Test
    fun shouldMapWrongPasswordToWrongPassword() {
        assertEquals(ExitCode.WRONG_PASSWORD, mapDenied(DenyReason.WRONG_PASSWORD))
    }

    @Test
    fun shouldMapUnknownActionToUnknownAction() {
        assertEquals(ExitCode.UNKNOWN_ACTION, mapDenied(DenyReason.UNKNOWN_ACTION))
    }

    @Test
    fun shouldMapInvalidFormatToBadFormat() {
        assertEquals(ExitCode.BAD_FORMAT, mapDenied(DenyReason.INVALID_FORMAT))
    }

    @Test
    fun shouldMapResourceNotFoundToNoResource() {
        assertEquals(ExitCode.NO_RESOURCE, mapDenied(DenyReason.RESOURCE_NOT_FOUND))
    }

    @Test
    fun shouldMapAccessDeniedToNoAccess() {
        assertEquals(ExitCode.NO_ACCESS, mapDenied(DenyReason.ACCESS_DENIED))
    }

    @Test
    fun shouldMapVolumeExceededToVolumeExceeded() {
        assertEquals(ExitCode.VOLUME_EXCEEDED, mapDenied(DenyReason.VOLUME_EXCEEDED))
    }

    @Test
    fun shouldNeverMapAnyDenyReasonToOk() {
        DenyReason.values().forEach { assertNotEquals(ExitCode.OK, mapDenied(it), "причина $it") }
    }

    @Test
    fun shouldMapDifferentDenyReasonsToDifferentCodes() {
        val codes = DenyReason.values().map { mapDenied(it) }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun shouldKeepNumericCodesFromTaskTable() {
        val expected = mapOf(
            ExitCode.OK to 0, ExitCode.HELP to 1, ExitCode.WRONG_PASSWORD to 2,
            ExitCode.WRONG_LOGIN to 3, ExitCode.UNKNOWN_ACTION to 4, ExitCode.NO_ACCESS to 5,
            ExitCode.NO_RESOURCE to 6, ExitCode.BAD_FORMAT to 7, ExitCode.VOLUME_EXCEEDED to 8
        )
        expected.forEach { (exitCode, number) -> assertEquals(number, exitCode.code, "код $exitCode") }
    }

    @Test
    fun shouldHaveExactlyNineExitCodes() {
        assertEquals((0..8).toList(), ExitCode.values().map { it.code }.sorted())
    }
}

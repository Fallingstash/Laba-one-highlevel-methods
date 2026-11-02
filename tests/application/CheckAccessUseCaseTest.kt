package application

import domain.Action
import domain.Grant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import support.fakeUser
import support.path

class CheckAccessUseCaseTest {

    private val hasher = FakePasswordHasher()

    private val useCase = CheckAccessUseCase(
        users = FakeUserRepository(
            listOf(
                fakeUser("alice", "pw", Grant(path("A"), setOf(Action.READ, Action.WRITE))),
                fakeUser(
                    "bob", "pw",
                    Grant(path("A.B"), setOf(Action.READ)),
                    Grant(path("Docs"), setOf(Action.EXECUTE))
                ),
                fakeUser("nogrants", "pw")
            )
        ),
        resources = FakeResourceRepository(
            mapOf("A" to 100, "A.B" to 50, "A.B.C" to 20, "A.BX" to 10, "Docs" to 200)
        ),
        hasher = hasher
    )

    private fun check(
        login: String = "alice",
        password: String = "pw",
        action: String = "read",
        resource: String = "A.B.C",
        volume: String = "10"
    ) = useCase.check(AccessRequest(login, password, action, resource, volume))

    private fun denied(reason: DenyReason) = AccessResult.Denied(reason)

    // ---------- успешные сценарии ----------

    @Test
    fun shouldGrantAccessWhenGrantIsInheritedFromParent() {
        assertEquals(AccessResult.Granted, check())
    }

    @Test
    fun shouldGrantAccessWhenGrantIsOnTheResourceItself() {
        assertEquals(AccessResult.Granted, check(resource = "A", volume = "100"))
    }

    @Test
    fun shouldGrantExecuteWhenUserHasExecuteGrant() {
        assertEquals(AccessResult.Granted, check(login = "bob", action = "execute", resource = "Docs", volume = "5"))
    }

    @Test
    fun shouldGrantWhenVolumeEqualsMaximum() {
        assertEquals(AccessResult.Granted, check(volume = "20"))
    }

    @Test
    fun shouldGrantWhenVolumeIsZero() {
        assertEquals(AccessResult.Granted, check(volume = "0"))
    }

    // ---------- логин и пароль ----------

    @Test
    fun shouldDenyWithUnknownLoginWhenLoginDoesNotExist() {
        assertEquals(denied(DenyReason.UNKNOWN_LOGIN), check(login = "nobody"))
    }

    @Test
    fun shouldDenyWithUnknownLoginWhenLoginIsEmpty() {
        assertEquals(denied(DenyReason.UNKNOWN_LOGIN), check(login = ""))
    }

    @Test
    fun shouldDenyWithUnknownLoginWhenLoginDiffersByCase() {
        assertEquals(denied(DenyReason.UNKNOWN_LOGIN), check(login = "Alice"))
    }

    @Test
    fun shouldDenyWithWrongPasswordWhenPasswordIsIncorrect() {
        assertEquals(denied(DenyReason.WRONG_PASSWORD), check(password = "bad"))
    }

    @Test
    fun shouldDenyWithWrongPasswordWhenPasswordIsEmpty() {
        assertEquals(denied(DenyReason.WRONG_PASSWORD), check(password = ""))
    }

    @Test
    fun shouldReportUnknownLoginBeforeWrongPassword() {
        assertEquals(denied(DenyReason.UNKNOWN_LOGIN), check(login = "nobody", password = "bad"))
    }

    @Test
    fun shouldNotAskHasherWhenLoginIsUnknown() {
        check(login = "nobody")
        assertEquals(0, hasher.calls)
    }

    @Test
    fun shouldAskHasherExactlyOnceWhenLoginExists() {
        check()
        assertEquals(1, hasher.calls)
    }

    // ---------- действие ----------

    @Test
    fun shouldDenyWithUnknownActionWhenActionIsNotSupported() {
        assertEquals(denied(DenyReason.UNKNOWN_ACTION), check(action = "delete"))
    }

    @Test
    fun shouldDenyWithUnknownActionWhenActionIsWrittenInUpperCase() {
        assertEquals(denied(DenyReason.UNKNOWN_ACTION), check(action = "READ"))
    }

    @Test
    fun shouldDenyWithUnknownActionWhenActionIsEmpty() {
        assertEquals(denied(DenyReason.UNKNOWN_ACTION), check(action = ""))
    }

    @Test
    fun shouldReportUnknownActionBeforeInvalidResourceFormat() {
        assertEquals(denied(DenyReason.UNKNOWN_ACTION), check(action = "delete", resource = "A..B"))
    }

    // ---------- формат ресурса и объёма ----------

    @Test
    fun shouldDenyWithInvalidFormatWhenResourceIsEmpty() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(resource = ""))
    }

    @Test
    fun shouldDenyWithInvalidFormatWhenResourceHasEmptySegment() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(resource = "A..B"))
    }

    @Test
    fun shouldDenyWithInvalidFormatWhenResourceSegmentIsTooLong() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(resource = "A." + "x".repeat(21)))
    }

    @Test
    fun shouldDenyWithInvalidFormatWhenResourceHasForbiddenCharacters() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(resource = "A.B-C"))
    }

    @Test
    fun shouldDenyWithInvalidFormatWhenVolumeIsNotANumber() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(volume = "ten"))
    }

    @Test
    fun shouldDenyWithInvalidFormatWhenVolumeIsEmpty() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(volume = ""))
    }

    @Test
    fun shouldDenyWithInvalidFormatWhenVolumeIsFractional() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(volume = "1.5"))
    }

    @Test
    fun shouldDenyWithInvalidFormatWhenVolumeIsNegative() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(volume = "-1"))
    }

    @Test
    fun shouldDenyWithInvalidFormatWhenVolumeDoesNotFitInInt() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(volume = "99999999999"))
    }

    @Test
    fun shouldReportInvalidVolumeBeforeResourceNotFound() {
        assertEquals(denied(DenyReason.INVALID_FORMAT), check(resource = "A.X", volume = "abc"))
    }

    // ---------- существование ресурса ----------

    @Test
    fun shouldDenyWithResourceNotFoundWhenResourceDoesNotExist() {
        assertEquals(denied(DenyReason.RESOURCE_NOT_FOUND), check(resource = "A.X"))
    }

    @Test
    fun shouldDenyWithResourceNotFoundWhenChildOfLeafIsRequested() {
        assertEquals(denied(DenyReason.RESOURCE_NOT_FOUND), check(resource = "A.B.C.D"))
    }

    @Test
    fun shouldReportResourceNotFoundBeforeAccessDenied() {
        assertEquals(denied(DenyReason.RESOURCE_NOT_FOUND), check(login = "nogrants", resource = "A.X"))
    }

    // ---------- права доступа ----------

    @Test
    fun shouldDenyWithAccessDeniedWhenUserHasNoGrants() {
        assertEquals(denied(DenyReason.ACCESS_DENIED), check(login = "nogrants"))
    }

    @Test
    fun shouldDenyWithAccessDeniedWhenActionIsNotGranted() {
        assertEquals(denied(DenyReason.ACCESS_DENIED), check(login = "bob", action = "write", resource = "A.B"))
    }

    @Test
    fun shouldDenyWithAccessDeniedWhenGrantIsOnlyOnChild() {
        assertEquals(denied(DenyReason.ACCESS_DENIED), check(login = "bob", resource = "A"))
    }

    @Test
    fun shouldDenyWithAccessDeniedWhenOnlyNamePrefixMatches() {
        assertEquals(denied(DenyReason.ACCESS_DENIED), check(login = "bob", resource = "A.BX", volume = "1"))
    }

    // ---------- объём ----------

    @Test
    fun shouldDenyWithVolumeExceededWhenVolumeIsAboveMaximum() {
        assertEquals(denied(DenyReason.VOLUME_EXCEEDED), check(volume = "21"))
    }

    @Test
    fun shouldReportAccessDeniedBeforeVolumeExceeded() {
        assertEquals(denied(DenyReason.ACCESS_DENIED), check(login = "nogrants", volume = "9999"))
    }
}

package application

import application.port.StorageFailureKind
import domain.Action
import domain.Grant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import support.fakeUser
import support.path

/** Сбои хранилища (подключение, SQL) должны превращаться в результат, а не в исключение. */
class CheckAccessUseCaseStorageFailureTest {

    private val workingUsers = FakeUserRepository(
        listOf(fakeUser("alice", "pw", Grant(path("A"), setOf(Action.READ))))
    )
    private val workingResources = FakeResourceRepository(mapOf("A" to 100))

    private fun request(action: String = "read", resource: String = "A", volume: String = "1") =
        AccessRequest("alice", "pw", action, resource, volume)

    private fun useCase(
        users: application.port.UserRepository = workingUsers,
        resources: application.port.ResourceRepository = workingResources
    ) = CheckAccessUseCase(users, resources, FakePasswordHasher())

    @Test
    fun shouldReturnConnectionFailureWhenUserRepositoryCannotConnect() {
        val result = useCase(users = FailingUserRepository(StorageFailureKind.CONNECTION)).check(request())
        assertEquals(AccessResult.StorageFailure(StorageFailureKind.CONNECTION), result)
    }

    @Test
    fun shouldReturnQueryFailureWhenUserRepositoryQueryFails() {
        val result = useCase(users = FailingUserRepository(StorageFailureKind.QUERY)).check(request())
        assertEquals(AccessResult.StorageFailure(StorageFailureKind.QUERY), result)
    }

    @Test
    fun shouldReturnStorageFailureWhenResourceRepositoryFails() {
        val result = useCase(resources = FailingResourceRepository(StorageFailureKind.QUERY)).check(request())
        assertEquals(AccessResult.StorageFailure(StorageFailureKind.QUERY), result)
    }

    @Test
    fun shouldReportStorageFailureBeforeActionAndFormatChecks() {
        // пользователя ищут первым, поэтому недоступная база важнее любых ошибок формата
        val result = useCase(users = FailingUserRepository(StorageFailureKind.CONNECTION))
            .check(request(action = "delete", resource = "A..B", volume = "abc"))
        assertEquals(AccessResult.StorageFailure(StorageFailureKind.CONNECTION), result)
    }

    @Test
    fun shouldNotTouchResourcesWhenUserLookupFails() {
        val result = useCase(
            users = FailingUserRepository(StorageFailureKind.CONNECTION),
            resources = FailingResourceRepository(StorageFailureKind.QUERY)
        ).check(request())
        assertEquals(AccessResult.StorageFailure(StorageFailureKind.CONNECTION), result)
    }

    @Test
    fun shouldStillGrantWhenStorageWorks() {
        assertEquals(AccessResult.Granted, useCase().check(request()))
    }
}

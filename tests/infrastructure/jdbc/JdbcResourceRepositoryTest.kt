package infrastructure.jdbc

import application.port.StorageException
import application.port.StorageFailureKind
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import support.TestDatabase
import support.path

class JdbcResourceRepositoryTest {

    private val database = TestDatabase.filled()
    private val repository = JdbcResourceRepository(database)

    @AfterEach
    fun closeDatabase() = database.close()

    @Test
    fun shouldFindTopLevelResourceWithItsMaximumVolume() {
        assertEquals(100, repository.find(path("A"))?.maxVolume)
    }

    @Test
    fun shouldFindDeeplyNestedResourceWithItsOwnMaximumVolume() {
        assertEquals(20, repository.find(path("A.B.C"))?.maxVolume)
    }

    @Test
    fun shouldFindResourceInAnotherBranch() {
        assertEquals(100, repository.find(path("Docs.Reports"))?.maxVolume)
    }

    @Test
    fun shouldReturnResourceWithRequestedPath() {
        assertEquals("A.D", repository.find(path("A.D"))?.path.toString())
    }

    @Test
    fun shouldReturnNullWhenResourceDoesNotExist() {
        assertNull(repository.find(path("A.X")))
    }

    @Test
    fun shouldReturnNullWhenChildOfLeafIsRequested() {
        assertNull(repository.find(path("A.B.C.D")))
    }

    @Test
    fun shouldReturnNullWhenChildIsRequestedWithoutItsParent() {
        assertNull(repository.find(path("B")))
    }

    @Test
    fun shouldBeCaseSensitiveWhenSearchingResources() {
        assertNull(repository.find(path("a")))
    }

    @Test
    fun shouldReportQueryFailureWhenTablesAreMissing() {
        val broken = JdbcResourceRepository(TestDatabase.empty())
        val error = assertThrows(StorageException::class.java) { broken.find(path("A")) }
        assertEquals(StorageFailureKind.QUERY, error.kind)
    }

    @Test
    fun shouldReportConnectionFailureWhenDatabaseIsUnreachable() {
        val broken = JdbcResourceRepository(Database("jdbc:h2:file:/nonexistent-dir/x;IFEXISTS=TRUE"))
        val error = assertThrows(StorageException::class.java) { broken.find(path("A")) }
        assertEquals(StorageFailureKind.CONNECTION, error.kind)
    }
}

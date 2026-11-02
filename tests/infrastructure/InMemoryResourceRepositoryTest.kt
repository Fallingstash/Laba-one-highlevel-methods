package infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import support.path

class InMemoryResourceRepositoryTest {

    private val repository = InMemoryResourceRepository()

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
        assertNotNull(repository.find(path("A")))
    }
}

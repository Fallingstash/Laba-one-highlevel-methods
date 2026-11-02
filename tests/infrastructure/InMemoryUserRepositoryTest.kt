package infrastructure

import domain.Action
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InMemoryUserRepositoryTest {

    private val repository = InMemoryUserRepository()
    private val hasher = Sha256PasswordHasher()

    @Test
    fun shouldFindUserByLogin() {
        assertEquals("alice", repository.findByLogin("alice")?.login)
    }

    @Test
    fun shouldReturnNullWhenLoginIsUnknown() {
        assertNull(repository.findByLogin("nobody"))
    }

    @Test
    fun shouldBeCaseSensitiveWhenSearchingByLogin() {
        assertNull(repository.findByLogin("Alice"))
    }

    @Test
    fun shouldReturnNullWhenLoginIsEmpty() {
        assertNull(repository.findByLogin(""))
    }

    @Test
    fun shouldStoreCredentialsWithSha256AlgorithmAndSalt() {
        val credentials = repository.findByLogin("alice")!!.credentials
        assertEquals(Sha256PasswordHasher.ALGORITHM, credentials.algorithm)
        assertTrue(credentials.saltHex.isNotEmpty())
    }

    @Test
    fun shouldNotStorePasswordsInPlainText() {
        val credentials = repository.findByLogin("alice")!!.credentials
        assertFalse(credentials.hashHex.contains("qwerty"))
        assertFalse(credentials.saltHex.contains("qwerty"))
    }

    @Test
    fun shouldAcceptDocumentedPasswordForAlice() {
        assertTrue(hasher.verify("qwerty", repository.findByLogin("alice")!!.credentials))
    }

    @Test
    fun shouldAcceptDocumentedPasswordForBob() {
        assertTrue(hasher.verify("hunter2", repository.findByLogin("bob")!!.credentials))
    }

    @Test
    fun shouldNotAcceptPasswordOfAnotherUser() {
        assertFalse(hasher.verify("hunter2", repository.findByLogin("alice")!!.credentials))
    }

    @Test
    fun shouldGiveUsersDifferentSalts() {
        val alice = repository.findByLogin("alice")!!.credentials.saltHex
        val bob = repository.findByLogin("bob")!!.credentials.saltHex
        assertTrue(alice != bob)
    }

    @Test
    fun shouldGiveAliceReadAndWriteOnA() {
        val grant = repository.findByLogin("alice")!!.grants.single()
        assertEquals("A", grant.path.toString())
        assertEquals(setOf(Action.READ, Action.WRITE), grant.actions)
    }

    @Test
    fun shouldGiveBobReadOnABAndExecuteOnDocs() {
        val grants = repository.findByLogin("bob")!!.grants.associate { it.path.toString() to it.actions }
        assertEquals(mapOf("A.B" to setOf(Action.READ), "Docs" to setOf(Action.EXECUTE)), grants)
    }

    @Test
    fun shouldFindBothSeedUsers() {
        assertNotNull(repository.findByLogin("alice"))
        assertNotNull(repository.findByLogin("bob"))
    }
}

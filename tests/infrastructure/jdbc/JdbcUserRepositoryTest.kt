package infrastructure.jdbc

import application.port.StorageException
import application.port.StorageFailureKind
import domain.Action
import infrastructure.Sha256PasswordHasher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import support.TestDatabase

class JdbcUserRepositoryTest {

    private val database = TestDatabase.filled()
    private val repository = JdbcUserRepository(database)
    private val hasher = Sha256PasswordHasher()

    @AfterEach
    fun closeDatabase() = database.close()

    private fun execute(sql: String) =
        database.withConnection { it.createStatement().use { st -> st.execute(sql) } }

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
    fun shouldNotBeFooledByQuotesInLogin() {
        assertNull(repository.findByLogin("alice' OR '1'='1"))
    }

    @Test
    fun shouldNotExecuteSqlHiddenInLogin() {
        repository.findByLogin("x'; DROP TABLE users; --")
        assertEquals(2, database.withConnection { c ->
            c.createStatement().use { st -> st.executeQuery("SELECT COUNT(*) FROM users").use { rs -> rs.next(); rs.getInt(1) } }
        })
    }

    @Test
    fun shouldLoadCredentialsWithSha256AlgorithmAndSalt() {
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
    fun shouldGroupAliceActionsIntoSingleGrantOnA() {
        val grant = repository.findByLogin("alice")!!.grants.single()
        assertEquals("A", grant.path.toString())
        assertEquals(setOf(Action.READ, Action.WRITE), grant.actions)
    }

    @Test
    fun shouldLoadBobGrantsOnTwoResources() {
        val grants = repository.findByLogin("bob")!!.grants.associate { it.path.toString() to it.actions }
        assertEquals(mapOf("A.B" to setOf(Action.READ), "Docs" to setOf(Action.EXECUTE)), grants)
    }

    @Test
    fun shouldReturnEmptyGrantsWhenUserHasNoPermissions() {
        execute("INSERT INTO users (login, algorithm, salt_hex, hash_hex) VALUES ('carol', 'SHA-256', '00', '00')")
        assertEquals(emptyList<Any>(), repository.findByLogin("carol")!!.grants)
    }

    @Test
    fun shouldSeeChangesMadeInDatabaseBetweenCalls() {
        assertNull(repository.findByLogin("carol"))
        execute("INSERT INTO users (login, algorithm, salt_hex, hash_hex) VALUES ('carol', 'SHA-256', '00', '00')")
        assertEquals("carol", repository.findByLogin("carol")?.login)
    }

    @Test
    fun shouldReportQueryFailureWhenTablesAreMissing() {
        val broken = JdbcUserRepository(TestDatabase.empty())
        val error = assertThrows(StorageException::class.java) { broken.findByLogin("alice") }
        assertEquals(StorageFailureKind.QUERY, error.kind)
    }

    @Test
    fun shouldReportQueryFailureWhenDatabaseHoldsUnknownAction() {
        execute("ALTER TABLE permissions DROP CONSTRAINT chk_permissions_action")
        execute("INSERT INTO permissions (user_id, resource_id, action) VALUES ((SELECT id FROM users WHERE login = 'alice'), (SELECT id FROM resources WHERE path = 'A.B.C'), 'DELETE')")
        val error = assertThrows(StorageException::class.java) { repository.findByLogin("alice") }
        assertEquals(StorageFailureKind.QUERY, error.kind)
    }

    @Test
    fun shouldReportQueryFailureWhenDatabaseHoldsMalformedResourcePath() {
        execute("INSERT INTO resources (parent_id, name, path, max_volume) VALUES (NULL, 'X', 'A..B', 1)")
        execute("INSERT INTO permissions (user_id, resource_id, action) VALUES ((SELECT id FROM users WHERE login = 'bob'), (SELECT id FROM resources WHERE name = 'X'), 'READ')")
        val error = assertThrows(StorageException::class.java) { repository.findByLogin("bob") }
        assertEquals(StorageFailureKind.QUERY, error.kind)
    }
}

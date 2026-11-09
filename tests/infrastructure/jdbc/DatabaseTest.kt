package infrastructure.jdbc

import application.port.StorageException
import application.port.StorageFailureKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import support.TestDatabase
import java.nio.file.Files

class DatabaseTest {

    private fun failureKind(block: () -> Unit): StorageFailureKind =
        assertThrows(StorageException::class.java) { block() }.kind

    @Test
    fun shouldConnectWhenUrlIsValid() {
        TestDatabase.filled().use { database ->
            assertTrue(database.withConnection { it.isValid(1) })
        }
    }

    @Test
    fun shouldReuseSameConnectionBetweenCalls() {
        TestDatabase.filled().use { database ->
            val first = database.withConnection { it }
            val second = database.withConnection { it }
            assertSame(first, second)
        }
    }

    @Test
    fun shouldCloseConnectionWhenDatabaseIsClosed() {
        val database = TestDatabase.filled()
        val connection = database.withConnection { it }
        database.close()
        assertTrue(connection.isClosed)
    }

    @Test
    fun shouldOpenNewConnectionWhenUsedAfterClose() {
        val database = TestDatabase.filled()
        val first = database.withConnection { it }
        database.close()
        val second = database.withConnection { it }
        assertNotSame(first, second)
        assertFalse(second.isClosed)
        database.close()
    }

    @Test
    fun shouldNotFailWhenClosedWithoutEverConnecting() {
        Database("jdbc:h2:mem:never-used").close()
    }

    @Test
    fun shouldNotConnectUntilFirstUse() {
        // адрес заведомо неверный, но пока запросов нет, ошибки быть не должно
        Database("jdbc:h2:file:/nonexistent-dir/x;IFEXISTS=TRUE").use { }
    }

    @Test
    fun shouldReportConnectionFailureWhenDatabaseFileDoesNotExist() {
        val missing = Files.createTempDirectory("lab-db").resolve("missing").toString()
        val database = Database(DatabaseUrls.h2File(missing))
        assertEquals(StorageFailureKind.CONNECTION, failureKind { database.withConnection { } })
    }

    @Test
    fun shouldNotCreateDatabaseFileWhenItDoesNotExist() {
        val dir = Files.createTempDirectory("lab-db")
        val database = Database(DatabaseUrls.h2File(dir.resolve("missing").toString()))
        failureKind { database.withConnection { } }
        assertEquals(0, dir.toFile().listFiles()!!.size)
    }

    @Test
    fun shouldReportConnectionFailureWhenPasswordIsWrong() {
        val database = Database(TestDatabase.filledUrl(), "sa", "wrong-password")
        assertEquals(StorageFailureKind.CONNECTION, failureKind { database.withConnection { } })
    }

    @Test
    fun shouldReportConnectionFailureWhenNoDriverMatchesUrl() {
        val database = Database("jdbc:no-such-database:whatever")
        assertEquals(StorageFailureKind.CONNECTION, failureKind { database.withConnection { } })
    }

    @Test
    fun shouldReportQueryFailureWhenSqlIsInvalid() {
        TestDatabase.filled().use { database ->
            val kind = failureKind {
                database.withConnection { it.createStatement().use { st -> st.execute("SELECT * FROM missing_table") } }
            }
            assertEquals(StorageFailureKind.QUERY, kind)
        }
    }

    @Test
    fun shouldKeepOriginalSqlExceptionAsCause() {
        TestDatabase.filled().use { database ->
            val error = assertThrows(StorageException::class.java) {
                database.withConnection { it.createStatement().use { st -> st.execute("SELECT * FROM missing_table") } }
            }
            assertTrue(error.cause is java.sql.SQLException)
        }
    }

    @Test
    fun shouldStayUsableAfterQueryFailure() {
        TestDatabase.filled().use { database ->
            failureKind { database.withConnection { it.createStatement().use { st -> st.execute("BAD SQL") } } }
            assertTrue(database.withConnection { it.isValid(1) })
        }
    }

    @Test
    fun shouldReturnValueProducedByBlock() {
        TestDatabase.filled().use { database ->
            val count = database.withConnection { c ->
                c.createStatement().use { st -> st.executeQuery("SELECT COUNT(*) FROM users").use { rs -> rs.next(); rs.getInt(1) } }
            }
            assertEquals(2, count)
        }
    }
}

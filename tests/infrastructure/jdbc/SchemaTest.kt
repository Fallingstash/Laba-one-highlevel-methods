package infrastructure.jdbc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import support.TestDatabase
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/** Проверка самой схемы из scripts/init.sql и данных из scripts/fill.sql: ограничения целостности. */
class SchemaTest {

    private fun <T> withRawConnection(block: (Connection) -> T): T =
        DriverManager.getConnection(TestDatabase.filledUrl(), TestDatabase.USER, TestDatabase.PASSWORD).use(block)

    private fun Connection.execute(sql: String) = createStatement().use { it.execute(sql) }

    private fun Connection.count(sql: String): Int =
        createStatement().use { st -> st.executeQuery(sql).use { rs -> rs.next(); rs.getInt(1) } }

    private fun assertRejected(sql: String) {
        withRawConnection { connection ->
            assertThrows(SQLException::class.java) { connection.execute(sql) }
        }
    }

    // ---------- данные ----------

    @Test
    fun shouldFillTwoUsersSixResourcesAndFourPermissions() {
        withRawConnection { c ->
            assertEquals(2, c.count("SELECT COUNT(*) FROM users"))
            assertEquals(6, c.count("SELECT COUNT(*) FROM resources"))
            assertEquals(4, c.count("SELECT COUNT(*) FROM permissions"))
        }
    }

    @Test
    fun shouldKeepPathEqualToParentPathPlusName() {
        withRawConnection { c ->
            val broken = c.count(
                "SELECT COUNT(*) FROM resources child JOIN resources parent ON child.parent_id = parent.id " +
                    "WHERE child.path <> parent.path || '.' || child.name"
            )
            assertEquals(0, broken)
        }
    }

    @Test
    fun shouldUseNameAsPathForTopLevelResources() {
        withRawConnection { c ->
            assertEquals(0, c.count("SELECT COUNT(*) FROM resources WHERE parent_id IS NULL AND path <> name"))
        }
    }

    // ---------- ограничения ----------

    @Test
    fun shouldRejectDuplicateLogin() {
        assertRejected("INSERT INTO users (login, algorithm, salt_hex, hash_hex) VALUES ('alice', 'SHA-256', '00', '00')")
    }

    @Test
    fun shouldRejectDuplicateResourcePath() {
        assertRejected("INSERT INTO resources (parent_id, name, path, max_volume) VALUES (NULL, 'A', 'A', 1)")
    }

    @Test
    fun shouldRejectResourceNameLongerThanTwentyCharacters() {
        assertRejected("INSERT INTO resources (parent_id, name, path, max_volume) VALUES (NULL, '${"x".repeat(21)}', 'long', 1)")
    }

    @Test
    fun shouldRejectResourceNameWithForbiddenCharacters() {
        assertRejected("INSERT INTO resources (parent_id, name, path, max_volume) VALUES (NULL, 'a-b', 'a-b', 1)")
    }

    @Test
    fun shouldRejectNegativeMaximumVolume() {
        assertRejected("INSERT INTO resources (parent_id, name, path, max_volume) VALUES (NULL, 'Z', 'Z', -1)")
    }

    @Test
    fun shouldRejectUnknownParentResource() {
        assertRejected("INSERT INTO resources (parent_id, name, path, max_volume) VALUES (999, 'Z', 'Z', 1)")
    }

    @Test
    fun shouldRejectUnknownActionInPermissions() {
        assertRejected("INSERT INTO permissions (user_id, resource_id, action) VALUES ((SELECT id FROM users WHERE login = 'alice'), (SELECT id FROM resources WHERE path = 'A'), 'DELETE')")
    }

    @Test
    fun shouldRejectPermissionForUnknownUser() {
        assertRejected("INSERT INTO permissions (user_id, resource_id, action) VALUES (999, (SELECT id FROM resources WHERE path = 'A'), 'READ')")
    }

    @Test
    fun shouldRejectPermissionForUnknownResource() {
        assertRejected("INSERT INTO permissions (user_id, resource_id, action) VALUES ((SELECT id FROM users WHERE login = 'alice'), 999, 'READ')")
    }

    @Test
    fun shouldRejectSamePermissionTwice() {
        assertRejected("INSERT INTO permissions (user_id, resource_id, action) VALUES ((SELECT id FROM users WHERE login = 'alice'), (SELECT id FROM resources WHERE path = 'A'), 'READ')")
    }

    @Test
    fun shouldRejectDeletingResourceThatHasChildren() {
        assertRejected("DELETE FROM resources WHERE path = 'A'")
    }

    @Test
    fun shouldDeletePermissionsWhenUserIsDeleted() {
        withRawConnection { c ->
            c.execute("DELETE FROM users WHERE login = 'bob'")
            assertEquals(0, c.count("SELECT COUNT(*) FROM permissions WHERE user_id NOT IN (SELECT id FROM users)"))
            assertEquals(2, c.count("SELECT COUNT(*) FROM permissions"))
        }
    }
}

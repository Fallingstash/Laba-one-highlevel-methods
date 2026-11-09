package infrastructure.jdbc

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DatabaseUrlsTest {

    @Test
    fun shouldUseH2FileProtocol() {
        assertTrue(DatabaseUrls.h2File("./data/access").startsWith("jdbc:h2:file:"))
    }

    @Test
    fun shouldForbidCreatingMissingDatabase() {
        assertTrue(DatabaseUrls.h2File("./data/access").endsWith(";IFEXISTS=TRUE"))
    }

    @Test
    fun shouldKeepDatabaseNameInUrl() {
        assertTrue(DatabaseUrls.h2File("./data/access").contains("data/access"))
    }

    @Test
    fun shouldNotContainBackslashes() {
        assertFalse(DatabaseUrls.h2File("data\\access").contains("\\"))
    }

    @Test
    fun shouldMakeRelativePathAbsolute() {
        val path = DatabaseUrls.h2File("data/access").removePrefix("jdbc:h2:file:")
        assertTrue(path.startsWith("/") || path[1] == ':', "ожидали абсолютный путь, получили $path")
    }
}

package support

import infrastructure.jdbc.Database
import org.h2.tools.RunScript
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Базы H2 в памяти для тестов. Тесты запускаются из корня проекта, поэтому скрипты берутся по относительному пути. */
object TestDatabase {

    /** URL новой базы в памяти со схемой и данными из scripts/init.sql и scripts/fill.sql. */
    fun filledUrl(): String {
        val url = memoryUrl()
        RunScript.execute(url, USER, PASSWORD, "scripts/init.sql", StandardCharsets.UTF_8, false)
        RunScript.execute(url, USER, PASSWORD, "scripts/fill.sql", StandardCharsets.UTF_8, false)
        return url
    }

    /** URL новой базы в памяти без таблиц: подключение работает, но любой запрос к таблицам падает. */
    fun emptyUrl(): String = memoryUrl()

    fun filled(): Database = Database(filledUrl())

    fun empty(): Database = Database(emptyUrl())

    const val USER = "sa"
    const val PASSWORD = ""

    // DB_CLOSE_DELAY=-1: база живёт, пока не завершится JVM, даже когда все соединения закрыты
    private fun memoryUrl() = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
}

package infrastructure.jdbc

import application.port.StorageException
import application.port.StorageFailureKind
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Подключение к базе через стандартный JDBC. Соединение открывается лениво (при первом запросе),
 * поэтому, например, вывод справки не требует базы. Закрывается через [close].
 */
class Database(
    private val url: String,
    private val user: String = "sa",
    private val password: String = ""
) : AutoCloseable {

    private var current: Connection? = null

    /**
     * Выполняет [block] с открытым соединением. Ошибка подключения превращается в
     * [StorageFailureKind.CONNECTION], ошибка запроса (SQLException) в [StorageFailureKind.QUERY].
     */
    fun <T> withConnection(block: (Connection) -> T): T {
        val connection = open()
        try {
            return block(connection)
        } catch (e: SQLException) {
            throw StorageException(StorageFailureKind.QUERY, "Ошибка SQL-запроса: ${e.message}", e)
        }
    }

    private fun open(): Connection {
        try {
            val existing = current
            if (existing != null && !existing.isClosed) return existing
            return DriverManager.getConnection(url, user, password).also { current = it }
        } catch (e: SQLException) {
            throw StorageException(
                StorageFailureKind.CONNECTION, "Не удалось подключиться к базе данных: ${e.message}", e
            )
        }
    }

    override fun close() {
        val connection = current
        current = null
        try {
            connection?.close()
        } catch (ignored: SQLException) {
            // при закрытии сообщить нечего: результат работы уже получен
        }
    }
}

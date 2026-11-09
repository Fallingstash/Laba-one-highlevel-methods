package infrastructure.jdbc

import application.port.StorageException
import application.port.StorageFailureKind
import application.port.UserRepository
import domain.Action
import domain.Credentials
import domain.Grant
import domain.ResourcePath
import domain.UserAccount
import java.sql.Connection

class JdbcUserRepository(private val database: Database) : UserRepository {

    private class UserRow(val id: Int, val login: String, val credentials: Credentials)

    override fun findByLogin(login: String): UserAccount? = database.withConnection { connection ->
        val row = findUser(connection, login) ?: return@withConnection null
        UserAccount(row.login, row.credentials, findGrants(connection, row.id))
    }

    private fun findUser(connection: Connection, login: String): UserRow? =
        connection.prepareStatement(FIND_USER).use { statement ->
            statement.setString(1, login)
            statement.executeQuery().use { rows ->
                if (rows.next()) {
                    UserRow(
                        id = rows.getInt("id"),
                        login = rows.getString("login"),
                        credentials = Credentials(
                            algorithm = rows.getString("algorithm"),
                            saltHex = rows.getString("salt_hex"),
                            hashHex = rows.getString("hash_hex")
                        )
                    )
                } else {
                    null
                }
            }
        }

    private fun findGrants(connection: Connection, userId: Int): List<Grant> =
        connection.prepareStatement(FIND_GRANTS).use { statement ->
            statement.setInt(1, userId)
            statement.executeQuery().use { rows ->
                val actionsByPath = LinkedHashMap<String, MutableSet<Action>>()
                while (rows.next()) {
                    actionsByPath
                        .getOrPut(rows.getString("path")) { mutableSetOf() }
                        .add(toAction(rows.getString("action")))
                }
                actionsByPath.map { (path, actions) -> Grant(toPath(path), actions) }
            }
        }

    // Некорректные данные в БД считаем ошибкой запроса, а не падением программы
    private fun toAction(raw: String): Action =
        Action.values().firstOrNull { it.name == raw }
            ?: throw StorageException(StorageFailureKind.QUERY, "Неизвестное действие в базе: $raw")

    private fun toPath(raw: String): ResourcePath =
        ResourcePath.parse(raw)
            ?: throw StorageException(StorageFailureKind.QUERY, "Некорректный путь ресурса в базе: $raw")

    private companion object {
        const val FIND_USER =
            "SELECT id, login, algorithm, salt_hex, hash_hex FROM users WHERE login = ?"

        const val FIND_GRANTS =
            "SELECT r.path AS path, p.action AS action " +
                "FROM permissions p JOIN resources r ON r.id = p.resource_id " +
                "WHERE p.user_id = ? ORDER BY r.path, p.action"
    }
}

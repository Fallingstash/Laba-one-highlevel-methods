package infrastructure.jdbc

import application.port.ResourceRepository
import domain.Resource
import domain.ResourcePath

class JdbcResourceRepository(private val database: Database) : ResourceRepository {

    override fun find(path: ResourcePath): Resource? = database.withConnection { connection ->
        connection.prepareStatement(FIND_RESOURCE).use { statement ->
            statement.setString(1, path.toString())
            statement.executeQuery().use { rows ->
                if (rows.next()) Resource(path, rows.getInt("max_volume")) else null
            }
        }
    }

    private companion object {
        const val FIND_RESOURCE = "SELECT max_volume FROM resources WHERE path = ?"
    }
}

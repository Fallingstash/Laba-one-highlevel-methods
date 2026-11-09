package application.port

/** Что именно сломалось в хранилище: подключение или выполнение запроса. */
enum class StorageFailureKind { CONNECTION, QUERY }

/**
 * Сбой хранилища. Порты (репозитории) бросают это исключение, сценарий превращает его в результат,
 * поэтому детали конкретной СУБД (SQLException) наружу из infrastructure не попадают.
 */
class StorageException(
    val kind: StorageFailureKind,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

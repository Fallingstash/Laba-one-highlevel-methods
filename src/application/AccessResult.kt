package application

import application.port.StorageFailureKind

enum class DenyReason {
    UNKNOWN_LOGIN,
    WRONG_PASSWORD,
    UNKNOWN_ACTION,
    INVALID_FORMAT,
    RESOURCE_NOT_FOUND,
    ACCESS_DENIED,
    VOLUME_EXCEEDED
}

sealed interface AccessResult {
    /** Доступ разрешён. */
    data object Granted : AccessResult

    /** Доступ запрещён по бизнес-причине. */
    data class Denied(val reason: DenyReason) : AccessResult

    /** Проверку выполнить не удалось: хранилище недоступно или запрос к нему завершился ошибкой. */
    data class StorageFailure(val kind: StorageFailureKind) : AccessResult
}

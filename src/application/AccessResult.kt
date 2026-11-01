package application

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
    data object Granted : AccessResult
    data class Denied(val reason: DenyReason) : AccessResult
}

package domain

/** Данные для проверки пароля: алгоритм, соль и хэш (оба в hex). */
data class Credentials(val algorithm: String, val saltHex: String, val hashHex: String)

class UserAccount(
    val login: String,
    val credentials: Credentials,
    val grants: List<Grant>
)

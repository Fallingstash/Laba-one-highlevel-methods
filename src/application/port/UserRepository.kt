package application.port

import domain.UserAccount

interface UserRepository {
    /** null, если пользователя с таким логином нет. */
    fun findByLogin(login: String): UserAccount?
}

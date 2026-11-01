package application.port

import domain.Credentials

interface PasswordHasher {
    /** true, если пароль подходит к учётным данным; false (без исключения), если нет. */
    fun verify(password: String, credentials: Credentials): Boolean
}

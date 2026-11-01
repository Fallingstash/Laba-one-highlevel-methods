package infrastructure

import application.port.UserRepository
import domain.Action
import domain.Credentials
import domain.Grant
import domain.ResourcePath
import domain.UserAccount

/** Пользователи, хранящиеся в коде. Пароли только в виде соли и хэша. */
class InMemoryUserRepository : UserRepository {

    private val users: Map<String, UserAccount> = listOf(
        UserAccount(
            login = "alice",
            credentials = Credentials(Sha256PasswordHasher.ALGORITHM, "5cc8c530ea93bb53907ded1882305b8b", "7b50c883e6000b5f31f982ad632d23b5310ec432559a9382506fe0c33046564f"), // пароль: qwerty
            grants = listOf(Grant(path("A"), setOf(Action.READ, Action.WRITE)))
        ),
        UserAccount(
            login = "bob",
            credentials = Credentials(Sha256PasswordHasher.ALGORITHM, "e0e1049586ab91d68def66d2536fcd60", "844d9a7f0c184c4bea5bad23c39e109902a43050a12998f67ec360b4ca3f6faa"), // пароль: hunter2
            grants = listOf(
                Grant(path("A.B"), setOf(Action.READ)),
                Grant(path("Docs"), setOf(Action.EXECUTE))
            )
        )
    ).associateBy { it.login }

    override fun findByLogin(login: String): UserAccount? = users[login]

    private fun path(raw: String): ResourcePath =
        ResourcePath.parse(raw) ?: error("Некорректный путь в данных: $raw")
}

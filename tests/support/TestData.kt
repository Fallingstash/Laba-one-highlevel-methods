package support

import domain.Credentials
import domain.Grant
import domain.ResourcePath
import domain.UserAccount

/** Путь для тестов: падает сразу, если в тесте опечатка в формате пути. */
fun path(raw: String): ResourcePath = ResourcePath.parse(raw) ?: error("Неверный путь в тесте: $raw")

/** Пользователь с «хэшем» вида hash:<пароль> (его понимает FakePasswordHasher). */
fun fakeUser(login: String, password: String, vararg grants: Grant) =
    UserAccount(login, Credentials("fake", "", "hash:$password"), grants.toList())

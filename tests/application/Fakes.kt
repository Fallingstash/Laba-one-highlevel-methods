package application

import application.port.PasswordHasher
import application.port.ResourceRepository
import application.port.StorageException
import application.port.StorageFailureKind
import application.port.UserRepository
import domain.Credentials
import domain.Resource
import domain.ResourcePath
import domain.UserAccount

class FakeUserRepository(users: List<UserAccount>) : UserRepository {
    private val byLogin = users.associateBy { it.login }
    override fun findByLogin(login: String): UserAccount? = byLogin[login]
}

class FakeResourceRepository(private val maxVolumes: Map<String, Int>) : ResourceRepository {
    override fun find(path: ResourcePath): Resource? =
        maxVolumes[path.toString()]?.let { Resource(path, it) }
}

/** «Хэш» в тестах это "hash:" + пароль. Считает, сколько раз его спросили. */
class FakePasswordHasher : PasswordHasher {
    var calls = 0
        private set

    override fun verify(password: String, credentials: Credentials): Boolean {
        calls++
        return credentials.hashHex == "hash:$password"
    }
}

/** Репозиторий пользователей, который всегда падает с заданным сбоем хранилища. */
class FailingUserRepository(private val kind: StorageFailureKind) : UserRepository {
    override fun findByLogin(login: String): UserAccount? =
        throw StorageException(kind, "тестовый сбой хранилища")
}

/** Репозиторий ресурсов, который всегда падает с заданным сбоем хранилища. */
class FailingResourceRepository(private val kind: StorageFailureKind) : ResourceRepository {
    override fun find(path: ResourcePath): Resource? =
        throw StorageException(kind, "тестовый сбой хранилища")
}

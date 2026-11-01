import application.AccessRequest
import application.AccessResult
import application.CheckAccessUseCase
import application.DenyReason
import application.port.PasswordHasher
import application.port.ResourceRepository
import application.port.UserRepository
import domain.Action
import domain.Credentials
import domain.Grant
import domain.Resource
import domain.ResourcePath
import domain.UserAccount
import kotlin.system.exitProcess

/** Модульные тесты сценария. Поддельные реализации портов: никакого SHA-256 и никакого CLI. */
object UseCaseTest {

    private class FakeUsers(private val users: Map<String, UserAccount>) : UserRepository {
        override fun findByLogin(login: String): UserAccount? = users[login]
    }

    private class FakeResources(private val maxVolumes: Map<String, Int>) : ResourceRepository {
        override fun find(path: ResourcePath): Resource? =
            maxVolumes[path.toString()]?.let { Resource(path, it) }
    }

    /** «Хэш» в тестах это просто "hash:" + пароль. */
    private class FakeHasher : PasswordHasher {
        override fun verify(password: String, credentials: Credentials) =
            credentials.hashHex == "hash:$password"
    }

    private fun path(raw: String) = ResourcePath.parse(raw)!!

    private fun user(login: String, password: String, vararg grants: Grant) =
        UserAccount(login, Credentials("fake", "", "hash:$password"), grants.toList())

    private val useCase = CheckAccessUseCase(
        users = FakeUsers(
            listOf(
                user("alice", "pw", Grant(path("A"), setOf(Action.READ, Action.WRITE))),
                user("bob", "pw", Grant(path("A.B"), setOf(Action.READ)))
            ).associateBy { it.login }
        ),
        resources = FakeResources(
            mapOf("A" to 100, "A.B" to 50, "A.B.C" to 20, "A.BX" to 10)
        ),
        hasher = FakeHasher()
    )

    private var passed = 0
    private var total = 0

    private fun request(
        login: String = "alice", password: String = "pw", action: String = "read",
        resource: String = "A.B.C", volume: String = "10"
    ) = AccessRequest(login, password, action, resource, volume)

    private fun denied(reason: DenyReason) = AccessResult.Denied(reason)

    private fun case(name: String, expected: AccessResult, request: AccessRequest) {
        val actual = useCase.check(request)
        total++
        if (actual == expected) {
            passed++
            println("OK   $name")
        } else {
            println("FAIL $name (ожидали $expected, получили $actual)")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        case("права наследуются потомкам", AccessResult.Granted, request())
        case("неизвестный логин", denied(DenyReason.UNKNOWN_LOGIN), request(login = "nobody"))
        case("неверный пароль", denied(DenyReason.WRONG_PASSWORD), request(password = "bad"))
        case("неизвестное действие", denied(DenyReason.UNKNOWN_ACTION), request(action = "delete"))
        case("пустой сегмент пути", denied(DenyReason.INVALID_FORMAT), request(resource = "A..B"))
        case("слишком длинное имя", denied(DenyReason.INVALID_FORMAT), request(resource = "A." + "x".repeat(21)))
        case("объём не число", denied(DenyReason.INVALID_FORMAT), request(volume = "ten"))
        case("объём отрицательный", denied(DenyReason.INVALID_FORMAT), request(volume = "-1"))
        case("ресурса нет", denied(DenyReason.RESOURCE_NOT_FOUND), request(resource = "A.X"))
        case("нет права на действие", denied(DenyReason.ACCESS_DENIED), request(login = "bob", action = "write", resource = "A.B"))
        case("право на потомка не даёт доступ к родителю", denied(DenyReason.ACCESS_DENIED), request(login = "bob", resource = "A"))
        case("право на A.B не распространяется на A.BX", denied(DenyReason.ACCESS_DENIED), request(login = "bob", resource = "A.BX"))
        case("объём равен максимуму", AccessResult.Granted, request(volume = "20"))
        case("объём ноль допустим", AccessResult.Granted, request(volume = "0"))
        case("объём больше максимума", denied(DenyReason.VOLUME_EXCEEDED), request(volume = "21"))

        println("$passed/$total")
        exitProcess(if (passed == total) 0 else 1)
    }
}

import infrastructure.SaltGenerator
import infrastructure.Sha256PasswordHasher

/** Вспомогательный инструмент: печатает соль и хэш для пары логин/пароль. В приложение не входит. */
object GenHash {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "usage: GenHash <login> <password>" }
        val salt = SaltGenerator().generateHex()
        println("login=${args[0]}")
        println("salt=$salt")
        println("hash=${Sha256PasswordHasher().hashHex(args[1], salt)}")
    }
}

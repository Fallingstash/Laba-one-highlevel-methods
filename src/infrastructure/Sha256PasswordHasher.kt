package infrastructure

import application.port.PasswordHasher
import domain.Credentials
import java.security.MessageDigest

class Sha256PasswordHasher : PasswordHasher {

    fun hashHex(password: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(HexCodec.decode(saltHex))
        digest.update(password.toByteArray(Charsets.UTF_8))
        return HexCodec.encode(digest.digest())
    }

    override fun verify(password: String, credentials: Credentials): Boolean {
        if (credentials.algorithm != ALGORITHM) return false
        val actual = hashHex(password, credentials.saltHex)
        // сравнение за постоянное время
        return MessageDigest.isEqual(actual.toByteArray(), credentials.hashHex.toByteArray())
    }

    companion object {
        const val ALGORITHM = "SHA-256"
    }
}

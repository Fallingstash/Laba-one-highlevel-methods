import java.security.MessageDigest
import java.security.SecureRandom

object Auth {
    fun generateSaltHex(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt.toHex()
    }

    fun hashHex(password: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltHex.hexToBytes())
        digest.update(password.toByteArray(Charsets.UTF_8))
        return digest.digest().toHex()
    }

    fun verify(password: String, user: User): Boolean =
        MessageDigest.isEqual(
            hashHex(password, user.saltHex).toByteArray(),
            user.hashHex.toByteArray()
        )
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun String.hexToBytes(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()  
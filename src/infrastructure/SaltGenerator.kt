package infrastructure

import java.security.SecureRandom

class SaltGenerator(private val random: SecureRandom = SecureRandom()) {
    fun generateHex(): String {
        val salt = ByteArray(SALT_SIZE_BYTES)
        random.nextBytes(salt)
        return HexCodec.encode(salt)
    }

    private companion object {
        const val SALT_SIZE_BYTES = 16
    }
}

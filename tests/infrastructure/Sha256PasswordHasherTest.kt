package infrastructure

import domain.Credentials
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Sha256PasswordHasherTest {

    private val hasher = Sha256PasswordHasher()
    private val salt = "00112233445566778899aabbccddeeff"

    private fun credentialsFor(password: String, saltHex: String = salt) =
        Credentials(Sha256PasswordHasher.ALGORITHM, saltHex, hasher.hashHex(password, saltHex))

    // ---------- эталонные значения SHA-256 ----------

    @Test
    fun shouldMatchKnownSha256VectorForAbcWhenSaltIsEmpty() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hasher.hashHex("abc", ""))
    }

    @Test
    fun shouldMatchKnownSha256VectorForEmptyPasswordWhenSaltIsEmpty() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hasher.hashHex("", ""))
    }

    @Test
    fun shouldPutSaltBytesBeforePasswordWhenHashing() {
        assertEquals("c038ae3bc0bc351d1a67b5446f000f21df05e24bdfd434e905afd2c3eaf801e4", hasher.hashHex("abc", "0001"))
    }

    @Test
    fun shouldHashNonLatinPasswordAsUtf8() {
        assertEquals("2dbc574daca52689a24fb60e835f8c19a36400830df7350859dd32d1abaaec5d", hasher.hashHex("пароль", ""))
    }

    // ---------- свойства хэша ----------

    @Test
    fun shouldProduceSixtyFourLowerCaseHexCharacters() {
        val hash = hasher.hashHex("qwerty", salt)
        assertTrue(Regex("^[0-9a-f]{64}$").matches(hash))
    }

    @Test
    fun shouldProduceSameHashForSamePasswordAndSalt() {
        assertEquals(hasher.hashHex("qwerty", salt), hasher.hashHex("qwerty", salt))
    }

    @Test
    fun shouldProduceDifferentHashesForDifferentSalts() {
        assertNotEquals(hasher.hashHex("qwerty", "00"), hasher.hashHex("qwerty", "01"))
    }

    @Test
    fun shouldProduceDifferentHashesForDifferentPasswords() {
        assertNotEquals(hasher.hashHex("qwerty", salt), hasher.hashHex("qwerty1", salt))
    }

    // ---------- verify ----------

    @Test
    fun shouldVerifyWhenPasswordMatchesCredentials() {
        assertTrue(hasher.verify("qwerty", credentialsFor("qwerty")))
    }

    @Test
    fun shouldNotVerifyWhenPasswordIsWrong() {
        assertFalse(hasher.verify("qwerty!", credentialsFor("qwerty")))
    }

    @Test
    fun shouldNotVerifyWhenPasswordDiffersOnlyByCase() {
        assertFalse(hasher.verify("Qwerty", credentialsFor("qwerty")))
    }

    @Test
    fun shouldNotVerifyWhenPasswordIsEmpty() {
        assertFalse(hasher.verify("", credentialsFor("qwerty")))
    }

    @Test
    fun shouldNotVerifyWhenSaltDiffersFromTheOneUsedForHash() {
        val credentials = credentialsFor("qwerty")
        assertFalse(hasher.verify("qwerty", credentials.copy(saltHex = "00")))
    }

    @Test
    fun shouldNotVerifyWhenAlgorithmIsNotSha256() {
        val credentials = credentialsFor("qwerty").copy(algorithm = "MD5")
        assertFalse(hasher.verify("qwerty", credentials))
    }
}

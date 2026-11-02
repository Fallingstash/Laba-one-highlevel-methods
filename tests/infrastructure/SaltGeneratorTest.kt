package infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SaltGeneratorTest {

    private val generator = SaltGenerator()

    @Test
    fun shouldGenerateSixteenBytesEncodedAsThirtyTwoHexCharacters() {
        assertEquals(32, generator.generateHex().length)
    }

    @Test
    fun shouldGenerateOnlyLowerCaseHexCharacters() {
        assertTrue(Regex("^[0-9a-f]{32}$").matches(generator.generateHex()))
    }

    @Test
    fun shouldGenerateDifferentSaltsOnEachCall() {
        val salts = List(50) { generator.generateHex() }
        assertEquals(50, salts.toSet().size)
    }
}

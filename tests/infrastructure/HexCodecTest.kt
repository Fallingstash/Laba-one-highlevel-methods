package infrastructure

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HexCodecTest {

    @Test
    fun shouldEncodeBytesAsLowerCaseHexWithLeadingZeros() {
        assertEquals("000fffa0", HexCodec.encode(byteArrayOf(0x00, 0x0f, 0xff.toByte(), 0xa0.toByte())))
    }

    @Test
    fun shouldEncodeEmptyArrayAsEmptyString() {
        assertEquals("", HexCodec.encode(ByteArray(0)))
    }

    @Test
    fun shouldDecodeHexIntoBytes() {
        assertArrayEquals(byteArrayOf(0x00, 0x0f, 0xff.toByte(), 0xa0.toByte()), HexCodec.decode("000fffa0"))
    }

    @Test
    fun shouldDecodeUpperCaseHex() {
        assertArrayEquals(byteArrayOf(0xab.toByte(), 0xcd.toByte()), HexCodec.decode("ABCD"))
    }

    @Test
    fun shouldDecodeEmptyStringAsEmptyArray() {
        assertArrayEquals(ByteArray(0), HexCodec.decode(""))
    }

    @Test
    fun shouldRestoreOriginalBytesAfterEncodeAndDecode() {
        val original = ByteArray(256) { it.toByte() }
        assertArrayEquals(original, HexCodec.decode(HexCodec.encode(original)))
    }

    @Test
    fun shouldThrowWhenHexContainsInvalidCharacters() {
        assertThrows(NumberFormatException::class.java) { HexCodec.decode("zz") }
    }
}

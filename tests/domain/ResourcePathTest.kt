package domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import support.path

class ResourcePathTest {

    // ---------- parse ----------

    @Test
    fun shouldSplitPathIntoSegmentsWhenFormatIsValid() {
        assertEquals(listOf("A", "A8B", "C", "f_d"), ResourcePath.parse("A.A8B.C.f_d")?.segments)
    }

    @Test
    fun shouldParseSingleSegmentPath() {
        assertEquals(listOf("Docs"), ResourcePath.parse("Docs")?.segments)
    }

    @Test
    fun shouldAcceptSegmentOfExactlyTwentyCharacters() {
        assertNotNull(ResourcePath.parse("a".repeat(20)))
    }

    @Test
    fun shouldRejectSegmentLongerThanTwentyCharacters() {
        assertNull(ResourcePath.parse("a".repeat(21)))
    }

    @Test
    fun shouldAcceptDigitsAndUnderscoreInSegment() {
        assertNotNull(ResourcePath.parse("_1.2_3.__"))
    }

    @Test
    fun shouldRejectEmptyPath() {
        assertNull(ResourcePath.parse(""))
    }

    @Test
    fun shouldRejectPathWithEmptySegmentInTheMiddle() {
        assertNull(ResourcePath.parse("A..B"))
    }

    @Test
    fun shouldRejectPathStartingWithDot() {
        assertNull(ResourcePath.parse(".A"))
    }

    @Test
    fun shouldRejectPathEndingWithDot() {
        assertNull(ResourcePath.parse("A."))
    }

    @Test
    fun shouldRejectNonLatinLetters() {
        assertNull(ResourcePath.parse("Ресурс"))
    }

    @Test
    fun shouldRejectSpecialCharactersAndSpaces() {
        assertAllNull("A-B", "A B", "A/B", "A\\B", "A,B", "A:B")
    }

    @Test
    fun shouldRejectWhenOnlyOneSegmentIsInvalid() {
        assertNull(ResourcePath.parse("A.B.C d"))
    }

    @Test
    fun shouldPrintPathWithDotsInToString() {
        assertEquals("A.B.C", path("A.B.C").toString())
    }

    // ---------- startsWith ----------

    @Test
    fun shouldStartWithItsParentPath() {
        assertTrue(path("A.B.C").startsWith(path("A.B")))
    }

    @Test
    fun shouldStartWithItself() {
        assertTrue(path("A.B").startsWith(path("A.B")))
    }

    @Test
    fun shouldNotStartWithItsOwnChild() {
        assertFalse(path("A").startsWith(path("A.B")))
    }

    @Test
    fun shouldNotStartWithSiblingWhoseNameBeginsTheSame() {
        assertFalse(path("A.BX").startsWith(path("A.B")))
    }

    @Test
    fun shouldNotStartWithPathFromAnotherRoot() {
        assertFalse(path("Docs.Reports").startsWith(path("A")))
    }

    @Test
    fun shouldBeCaseSensitiveWhenComparingSegments() {
        assertFalse(path("a.b").startsWith(path("A")))
    }

    private fun assertAllNull(vararg raw: String) {
        raw.forEach { assertNull(ResourcePath.parse(it), "ожидали null для '$it'") }
    }
}

package web.qr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrCodeTest {

    private fun rows(code: QrCode) = (0 until code.size).map { y -> (0 until code.size).joinToString("") { x -> if (code.isDark(x, y)) "#" else "." } }

    @Test
    fun `the version is the smallest that holds the text, up to version 10`() {
        assertEquals(1, QrCode.versionFor(14))
        assertEquals(2, QrCode.versionFor(15))
        assertEquals(2, QrCode.versionFor(26))
        assertEquals(3, QrCode.versionFor(27))
        assertEquals(7, QrCode.versionFor(122))
        assertEquals(8, QrCode.versionFor(123))
        assertEquals(10, QrCode.versionFor(213))
        assertEquals(213, QrCode.MAX_BYTES)
    }

    @Test
    fun `text past version 10 is refused`() {
        assertFailsWith<IllegalArgumentException> { QrCode.versionFor(214) }
        assertFailsWith<IllegalArgumentException> { QrCode.encode("x".repeat(214)) }
    }

    @Test
    fun `the size grows by 4 modules a version`() {
        assertEquals(21, QrCode.encode("A".repeat(14)).size)
        assertEquals(25, QrCode.encode("A".repeat(15)).size)
        assertEquals(57, QrCode.encode("C".repeat(213)).size)
    }

    @Test
    fun `outside the code is light`() {
        val code = QrCode.encode("hi")

        assertFalse(code.isDark(-1, 0))
        assertFalse(code.isDark(0, code.size))
    }

    @Test
    fun `the finder squares, timing lines and dark module sit where the standard puts them`() {
        val code = QrCode.encode("A".repeat(15)) // version 2: 25 x 25, one alignment square
        val n = code.size
        for ((ox, oy) in listOf(0 to 0, n - 7 to 0, 0 to n - 7)) {
            for (d in 0..6) {
                assertTrue(code.isDark(ox + d, oy) && code.isDark(ox + d, oy + 6) && code.isDark(ox, oy + d) && code.isDark(ox + 6, oy + d))
            }
            assertTrue(code.isDark(ox + 3, oy + 3))
            assertFalse(code.isDark(ox + 1, oy + 1))
        }
        for (i in 8 until n - 8) {
            assertEquals(i % 2 == 0, code.isDark(i, 6), "timing row at $i")
            assertEquals(i % 2 == 0, code.isDark(6, i), "timing column at $i")
        }
        assertTrue(code.isDark(8, n - 8))
        assertTrue(code.isDark(18, 18)) // the alignment centre
        assertFalse(code.isDark(17, 17))
        assertTrue(code.isDark(16, 16))
    }

    @Test
    fun `the format word is written twice`() {
        val code = QrCode.encode("A".repeat(15))
        val n = code.size
        val first = (0..14).fold(0) { acc, i ->
            val (x, y) = when {
                i <= 5 -> 8 to i
                i == 6 -> 8 to 7
                i == 7 -> 8 to 8
                i == 8 -> 7 to 8
                else -> 14 - i to 8
            }
            acc or ((if (code.isDark(x, y)) 1 else 0) shl i)
        }
        val second = (0..14).fold(0) { acc, i ->
            val (x, y) = if (i < 8) n - 1 - i to 8 else 8 to n - 15 + i
            acc or ((if (code.isDark(x, y)) 1 else 0) shl i)
        }
        assertEquals(QrMasks.formatBits(code.mask), first)
        assertEquals(QrMasks.formatBits(code.mask), second)
    }

    @Test
    fun `version bits are written at version 7 and up`() {
        val code = QrCode.encode("B".repeat(123)) // version 8
        val n = code.size
        val bits = (0 until 18).fold(0) { acc, i -> acc or ((if (code.isDark(n - 11 + i % 3, i / 3)) 1 else 0) shl i) }
        assertEquals(QrMasks.versionBits(8), bits)
    }

    @Test
    fun `matches the reference encoder module for module`() {
        val cases = listOf(
            QrFixtures.LINK_4X4_TEXT to QrFixtures.LINK_4X4,
            QrFixtures.LINK_9X9_TEXT to QrFixtures.LINK_9X9,
            QrFixtures.LINK_LOCALHOST_TEXT to QrFixtures.LINK_LOCALHOST,
            QrFixtures.V1_FULL_TEXT to QrFixtures.V1_FULL,
            QrFixtures.V2_FIRST_TEXT to QrFixtures.V2_FIRST,
            QrFixtures.V8_FIRST_TEXT to QrFixtures.V8_FIRST,
            QrFixtures.V10_FULL_TEXT to QrFixtures.V10_FULL,
            QrFixtures.UNICODE_TEXT to QrFixtures.UNICODE,
        )
        for ((text, expected) in cases) assertEquals(expected, rows(QrCode.encode(text)), text)
    }
}

package web.qr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrMasksTest {

    @Test
    fun `the format words at level M for the eight masks`() {
        val expected = listOf(
            "101010000010010", "101000100100101", "101111001111100", "101101101001011",
            "100010111111001", "100000011001110", "100111110010111", "100101010100000",
        )
        assertEquals(expected, (0 until 8).map { QrMasks.formatBits(it).toString(2).padStart(15, '0') })
    }

    @Test
    fun `the version words of versions 7 to 10`() {
        assertEquals(0x07C94, QrMasks.versionBits(7))
        assertEquals(0x085BC, QrMasks.versionBits(8))
        assertEquals(0x09A99, QrMasks.versionBits(9))
        assertEquals(0x0A4D3, QrMasks.versionBits(10))
    }

    @Test
    fun `each mask flips the modules its formula names`() {
        assertTrue(QrMasks.flips(0, 1, 1))
        assertFalse(QrMasks.flips(0, 1, 2))
        assertTrue(QrMasks.flips(1, 5, 4))
        assertFalse(QrMasks.flips(1, 5, 3))
        assertTrue(QrMasks.flips(2, 3, 9))
        assertFalse(QrMasks.flips(2, 4, 9))
        assertTrue(QrMasks.flips(3, 2, 1))
        assertFalse(QrMasks.flips(3, 2, 2))
        assertTrue(QrMasks.flips(4, 2, 1))
        assertFalse(QrMasks.flips(4, 3, 0))
        assertTrue(QrMasks.flips(5, 2, 3))
        assertFalse(QrMasks.flips(5, 1, 1))
        assertTrue(QrMasks.flips(6, 1, 2))
        assertFalse(QrMasks.flips(6, 1, 3))
        assertTrue(QrMasks.flips(7, 0, 0))
        assertFalse(QrMasks.flips(7, 1, 0))
    }

    @Test
    fun `an all light grid scores runs, blocks and balance`() {
        val light = Array(11) { BooleanArray(11) }
        // Rule 1: 22 lines of 11 = 9 each, 198. Rule 2: 100 blocks of 3, 300. Rule 4: 0% dark is 10 steps, 100.
        assertEquals(198 + 300 + 100, QrMasks.penalty(light))
    }

    @Test
    fun `a checkerboard scores nothing`() {
        val board = Array(11) { y -> BooleanArray(11) { x -> (x + y) % 2 == 0 } }
        assertEquals(0, QrMasks.penalty(board))
    }

    @Test
    fun `a finder like row adds 40 per match`() {
        val board = Array(11) { y -> BooleanArray(11) { x -> (x + y) % 2 == 0 } }
        val pattern = "10111010000".map { it == '1' }
        val plain = QrMasks.penalty(board)
        pattern.forEachIndexed { x, dark -> board[0][x] = dark }

        assertTrue(QrMasks.penalty(board) >= plain + 40)
    }
}

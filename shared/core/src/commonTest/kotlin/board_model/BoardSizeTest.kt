package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardSizeTest {

    @Test
    fun `range constants are 3 to 6, default 4 (GH-160)`() {
        assertEquals(3, BoardSize.MIN)
        assertEquals(6, BoardSize.MAX)
        assertEquals(4, BoardSize.DEFAULT)
    }

    @Test
    fun `isValid accepts 3 to 6`() {
        assertTrue(BoardSize.isValid(3))
        assertTrue(BoardSize.isValid(4))
        assertTrue(BoardSize.isValid(5))
        assertTrue(BoardSize.isValid(6))
    }

    @Test
    fun `isValid rejects anything outside 3 to 6`() {
        assertFalse(BoardSize.isValid(2))
        assertFalse(BoardSize.isValid(7))
        assertFalse(BoardSize.isValid(0))
        assertFalse(BoardSize.isValid(-1))
    }

    @Test
    fun `require passes a valid size through unchanged`() {
        assertEquals(5, BoardSize.require(5))
    }

    @Test
    fun `require throws IllegalArgumentException on an invalid size`() {
        assertFailsWith<IllegalArgumentException> { BoardSize.require(7) }
    }

    @Test
    fun `VERY_LARGE is 7 to 9 and sits above MAX (GH-207)`() {
        assertEquals(7..9, BoardSize.VERY_LARGE)
        assertEquals(BoardSize.MAX + 1, BoardSize.VERY_LARGE.first)
    }

    @Test
    fun `isValid and require take a wider max`() {
        assertTrue(BoardSize.isValid(9, max = 9))
        assertFalse(BoardSize.isValid(10, max = 9))
        assertEquals(7, BoardSize.require(7, max = 9))
        assertFailsWith<IllegalArgumentException> { BoardSize.require(10, max = 9) }
    }
}

package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardSizeTest {

    @Test
    fun `range constants are 3 to 5, default 4`() {
        assertEquals(3, BoardSize.MIN)
        assertEquals(5, BoardSize.MAX)
        assertEquals(4, BoardSize.DEFAULT)
    }

    @Test
    fun `isValid accepts 3, 4, and 5`() {
        assertTrue(BoardSize.isValid(3))
        assertTrue(BoardSize.isValid(4))
        assertTrue(BoardSize.isValid(5))
    }

    @Test
    fun `isValid rejects anything outside 3 to 5`() {
        assertFalse(BoardSize.isValid(2))
        assertFalse(BoardSize.isValid(6))
        assertFalse(BoardSize.isValid(0))
        assertFalse(BoardSize.isValid(-1))
    }

    @Test
    fun `require passes a valid size through unchanged`() {
        assertEquals(5, BoardSize.require(5))
    }

    @Test
    fun `require throws IllegalArgumentException on an invalid size`() {
        assertFailsWith<IllegalArgumentException> { BoardSize.require(6) }
    }
}

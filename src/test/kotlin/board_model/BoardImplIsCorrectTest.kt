package board_model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardImplIsCorrectTest {

    private fun boardWith(vararg values: Int): BoardImpl {
        val board = BoardImpl(4)
        board.boardArray = values.toList().toIntArray()
        return board
    }

    @Test
    fun `ascending board is correct`() {
        val board = boardWith(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

        assertTrue(board.isCorrect())
    }

    @Test
    fun `swapped adjacent pair is not correct`() {
        val board = boardWith(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 14)

        assertFalse(board.isCorrect())
    }

    @Test
    fun `reversed board is not correct`() {
        val board = boardWith(15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)

        assertFalse(board.isCorrect())
    }

    @Test
    fun `single trailing element out of order is not correct`() {
        val board = boardWith(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 13, 15)

        assertFalse(board.isCorrect())
    }
}

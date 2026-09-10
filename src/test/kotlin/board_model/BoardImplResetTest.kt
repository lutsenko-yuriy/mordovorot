package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardImplResetTest {

    @Test
    fun `resetGame produces a permutation of 0 until size`() {
        val board = BoardImpl(4)

        assertEquals(16, board.boardArray.size)
        assertEquals((0..15).toList(), board.boardArray.sorted())
    }

    @Test
    fun `resetGame resets counter to zero`() {
        val board = BoardImpl(4)
        board.counter = 7

        board.resetGame()

        assertEquals(0, board.counter)
    }

    @Test
    fun `constructor honours a non-default square side`() {
        val board = BoardImpl(3)

        assertEquals(9, board.boardArray.size)
        assertEquals((0..8).toList(), board.boardArray.sorted())
    }

    @Test
    fun `a 1x1 board is always correct`() {
        val board = BoardImpl(1)

        assertTrue(board.isCorrect())
        assertEquals(listOf(0), board.boardArray.toList())
    }
}

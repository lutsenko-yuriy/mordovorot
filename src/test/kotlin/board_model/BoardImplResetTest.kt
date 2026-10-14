package board_model

import kotlin.test.Test
import kotlin.test.assertEquals

class BoardImplResetTest {

    @Test
    fun `resetGame produces a permutation of 0 until size`() {
        val board = BoardImpl(4)

        assertEquals(16, board.boardArray.size)
        assertEquals((0..15).toList(), board.boardArray.sorted())
    }

    @Test
    fun `resetGame resets the move count to zero`() {
        val board = BoardImpl(4)
        board.shiftLeft(0)
        board.shiftUp(1)

        board.resetGame()

        assertEquals(0, board.moveCount)
    }

    @Test
    fun `constructor honours a non-default square side`() {
        val board = BoardImpl(3)

        assertEquals(9, board.boardArray.size)
        assertEquals((0..8).toList(), board.boardArray.sorted())
    }

}

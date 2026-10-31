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

    /** GH-74: without the parity fix, 200 odd-side shuffles all landing solvable is ~2^-200. */
    @Test
    fun `resetGame always produces a solvable board on odd sides`() {
        for (side in listOf(3, 5, 7, 9)) {
            val board = BoardImpl(side, maxSide = 9)
            repeat(200) {
                board.resetGame()
                assertTrue(Solvability.isSolvable(board.boardArray), "side $side: ${board.boardArray.toList()}")
            }
        }
    }
}

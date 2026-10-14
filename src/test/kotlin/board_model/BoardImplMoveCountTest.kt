package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** GH-62: a move is a run of consecutive shifts on the same row or column. */
class BoardImplMoveCountTest {

    @Test
    fun `a fresh board has no moves`() {
        assertEquals(0, BoardImpl(4).moveCount)
    }

    @Test
    fun `consecutive shifts on one row count as one move, in either direction`() {
        val board = BoardImpl(4)

        board.shiftLeft(0)
        board.shiftLeft(0)
        board.shiftRight(0)

        assertEquals(1, board.moveCount)
    }

    @Test
    fun `switching to another line starts a new move, including returning to an earlier one`() {
        val board = BoardImpl(4)

        board.shiftLeft(0)
        board.shiftRight(1)
        board.shiftRight(0)

        assertEquals(3, board.moveCount)
    }

    @Test
    fun `row and column with the same index are different lines`() {
        val board = BoardImpl(4)

        board.shiftLeft(0)
        board.shiftUp(0)
        board.shiftDown(0)

        assertEquals(2, board.moveCount)
    }

    @Test
    fun `a rejected shift does not count and does not break the run`() {
        val board = BoardImpl(4)
        board.shiftLeft(0)

        assertFailsWith<IllegalArgumentException> { board.shiftLeft(9) }
        assertFailsWith<IllegalArgumentException> { board.shiftUp(-1) }
        board.shiftLeft(0)

        assertEquals(1, board.moveCount)
    }

    @Test
    fun `resetGame zeroes the count and breaks the run`() {
        val board = BoardImpl(4)
        board.shiftLeft(0)
        board.shiftUp(1)

        board.resetGame()
        board.shiftUp(1)

        assertEquals(1, board.moveCount)
    }

    @Test
    fun `restoreState sets the count and the next shift starts a new move`() {
        val board = BoardImpl(4)
        board.shiftUp(1)

        board.restoreState(IntArray(16) { it }, moveCount = 5)
        assertEquals(5, board.moveCount)

        board.shiftUp(1)
        assertEquals(6, board.moveCount)
    }

    @Test
    fun `restoreState rejects a negative count and leaves the board untouched`() {
        val board = BoardImpl(4)
        board.shiftLeft(0)
        val before = board.boardArray.toList()

        assertFailsWith<IllegalArgumentException> {
            board.restoreState(IntArray(9) { it }, moveCount = -1)
        }

        assertEquals(before, board.boardArray.toList())
        assertEquals(4, board.squareSide)
        assertEquals(1, board.moveCount)
    }
}

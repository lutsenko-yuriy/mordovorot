package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BoardImplRestoreStateTest {

    @Test
    fun `restoreState replaces the board with a valid arrangement`() {
        val board = BoardImpl(4)
        val arrangement = intArrayOf(15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)

        board.restoreState(arrangement)

        assertEquals(arrangement.toList(), board.boardArray.toList())
    }

    @Test
    fun `restoreState resets counter to 0`() {
        val board = BoardImpl(4)
        board.counter = 7

        board.restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))

        assertEquals(0, board.counter)
    }

    @Test
    fun `restoreState rejects an array of the wrong size`() {
        val board = BoardImpl(4)

        assertFailsWith<IllegalArgumentException> {
            board.restoreState(intArrayOf(0, 1, 2, 3))
        }
    }

    @Test
    fun `restoreState rejects an out-of-range value`() {
        val board = BoardImpl(4)

        assertFailsWith<IllegalArgumentException> {
            board.restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16))
        }
    }

    @Test
    fun `restoreState rejects a duplicate value`() {
        val board = BoardImpl(4)

        assertFailsWith<IllegalArgumentException> {
            board.restoreState(intArrayOf(0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
        }
    }

    @Test
    fun `restoreState defensively copies - mutating the caller's array afterwards does not affect the board`() {
        val board = BoardImpl(4)
        val arrangement = intArrayOf(15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0)

        board.restoreState(arrangement)
        arrangement[0] = 99

        assertEquals(15, board.boardArray[0])
    }
}

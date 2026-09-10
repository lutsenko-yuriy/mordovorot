package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BoardImplShiftTest {

    private fun boardWith(vararg values: Int): BoardImpl {
        val board = BoardImpl(4)
        board.boardArray = values.toList().toIntArray()
        return board
    }

    private val identity = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    @Test
    fun `shiftLeft rotates only the target row, other rows untouched`() {
        val board = boardWith(*identity)

        board.shiftLeft(1)

        assertEquals(
            listOf(0, 1, 2, 3, 5, 6, 7, 4, 8, 9, 10, 11, 12, 13, 14, 15),
            board.boardArray.toList()
        )
    }

    @Test
    fun `shiftLeft rotates row 0`() {
        val board = boardWith(*identity)

        board.shiftLeft(0)

        assertEquals(
            listOf(1, 2, 3, 0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            board.boardArray.toList()
        )
    }

    @Test
    fun `shiftLeft rotates row 3`() {
        val board = boardWith(*identity)

        board.shiftLeft(3)

        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 12),
            board.boardArray.toList()
        )
    }

    @Test
    fun `shiftRight rotates only the target row, other rows untouched`() {
        val board = boardWith(*identity)

        board.shiftRight(2)

        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 11, 8, 9, 10, 12, 13, 14, 15),
            board.boardArray.toList()
        )
    }

    @Test
    fun `shiftLeft then shiftRight on the same row is the identity`() {
        val board = boardWith(*identity)

        board.shiftLeft(2)
        board.shiftRight(2)

        assertEquals(identity.toList(), board.boardArray.toList())
    }

    @Test
    fun `shiftLeft applied 4 times on a 4-wide row is the identity`() {
        val board = boardWith(*identity)

        repeat(4) { board.shiftLeft(0) }

        assertEquals(identity.toList(), board.boardArray.toList())
    }

    @Test
    fun `shiftUp rotates only the target column, other columns untouched`() {
        val board = boardWith(*identity)

        board.shiftUp(1)

        assertEquals(
            listOf(0, 5, 2, 3, 4, 9, 6, 7, 8, 13, 10, 11, 12, 1, 14, 15),
            board.boardArray.toList()
        )
    }

    @Test
    fun `shiftDown rotates only the target column, other columns untouched`() {
        val board = boardWith(*identity)

        board.shiftDown(3)

        assertEquals(
            listOf(0, 1, 2, 15, 4, 5, 6, 3, 8, 9, 10, 7, 12, 13, 14, 11),
            board.boardArray.toList()
        )
    }

    @Test
    fun `shiftUp rotates column 0`() {
        val board = boardWith(*identity)

        board.shiftUp(0)

        assertEquals(
            listOf(4, 1, 2, 3, 8, 5, 6, 7, 12, 9, 10, 11, 0, 13, 14, 15),
            board.boardArray.toList()
        )
    }

    @Test
    fun `shiftDown rotates column 2`() {
        val board = boardWith(*identity)

        board.shiftDown(2)

        assertEquals(
            listOf(0, 1, 14, 3, 4, 5, 2, 7, 8, 9, 6, 11, 12, 13, 10, 15),
            board.boardArray.toList()
        )
    }

    @Test
    fun `shiftUp then shiftDown on the same column is the identity`() {
        val board = boardWith(*identity)

        board.shiftUp(0)
        board.shiftDown(0)

        assertEquals(identity.toList(), board.boardArray.toList())
    }

    @Test
    fun `shiftLeft rejects a negative row`() {
        val board = boardWith(*identity)

        assertFailsWith<IllegalArgumentException> { board.shiftLeft(-1) }
    }

    @Test
    fun `shiftLeft rejects a row equal to SQUARE_SIDE`() {
        val board = boardWith(*identity)

        // SQUARE_SIDE (4) is out of range for a 0-indexed 4-wide board -
        // valid rows are 0..3. Regression test for an off-by-one in the
        // original bounds check (`row !in 0..SQUARE_SIDE`), which let this
        // through and threw ArrayIndexOutOfBoundsException instead.
        assertFailsWith<IllegalArgumentException> { board.shiftLeft(4) }
    }

    @Test
    fun `shiftRight rejects a row equal to SQUARE_SIDE`() {
        val board = boardWith(*identity)

        assertFailsWith<IllegalArgumentException> { board.shiftRight(4) }
    }

    @Test
    fun `shiftRight rejects a negative row`() {
        val board = boardWith(*identity)

        assertFailsWith<IllegalArgumentException> { board.shiftRight(-1) }
    }

    @Test
    fun `shiftUp rejects a column equal to SQUARE_SIDE`() {
        val board = boardWith(*identity)

        assertFailsWith<IllegalArgumentException> { board.shiftUp(4) }
    }

    @Test
    fun `shiftUp rejects a negative column`() {
        val board = boardWith(*identity)

        assertFailsWith<IllegalArgumentException> { board.shiftUp(-1) }
    }

    @Test
    fun `shiftDown rejects a column equal to SQUARE_SIDE`() {
        val board = boardWith(*identity)

        assertFailsWith<IllegalArgumentException> { board.shiftDown(4) }
    }

    @Test
    fun `shiftDown rejects a negative column`() {
        val board = boardWith(*identity)

        assertFailsWith<IllegalArgumentException> { board.shiftDown(-1) }
    }
}

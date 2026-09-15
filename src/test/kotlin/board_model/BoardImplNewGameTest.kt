package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class BoardImplNewGameTest {

    @Test
    fun `newGame(3) shrinks the board to a 3x3 shuffled permutation`() {
        val board = BoardImpl(4)

        board.newGame(3)

        assertEquals(3, board.squareSide)
        assertEquals(9, board.boardArray.size)
        assertEquals((0..8).toList(), board.boardArray.sorted())
    }

    @Test
    fun `newGame(5) grows the board to a 5x5 shuffled permutation`() {
        val board = BoardImpl(4)

        board.newGame(5)

        assertEquals(5, board.squareSide)
        assertEquals(25, board.boardArray.size)
        assertEquals((0..24).toList(), board.boardArray.sorted())
    }

    @Test
    fun `newGame resets the move counter`() {
        val board = BoardImpl(4)
        board.counter = 7

        board.newGame(3)

        assertEquals(0, board.counter)
    }

    @Test
    fun `newGame(2) throws and leaves the board untouched`() {
        val board = BoardImpl(4)
        val before = board.boardArray.toList()

        assertFailsWith<IllegalArgumentException> { board.newGame(2) }

        assertEquals(4, board.squareSide)
        assertEquals(before, board.boardArray.toList())
    }

    @Test
    fun `newGame(6) throws and leaves the board untouched`() {
        val board = BoardImpl(4)
        val before = board.boardArray.toList()

        assertFailsWith<IllegalArgumentException> { board.newGame(6) }

        assertEquals(4, board.squareSide)
        assertEquals(before, board.boardArray.toList())
    }

    @Test
    fun `resetGame after newGame(3) stays 3x3`() {
        val board = BoardImpl(4)
        board.newGame(3)

        board.resetGame()

        assertEquals(3, board.squareSide)
        assertEquals(9, board.boardArray.size)
    }

    @Test
    fun `restoreState after newGame resizes to the restored state's own size, not the size newGame left it at, GH-44 WU5`() {
        val board = BoardImpl(4)
        board.newGame(3)

        board.restoreState(IntArray(16) { it })

        assertEquals(4, board.squareSide)
        assertEquals((0..15).toList(), board.boardArray.toList())
    }

    @Test
    fun `the constructor still validates its own size argument`() {
        assertFailsWith<IllegalArgumentException> { BoardImpl(2) }
        assertFailsWith<IllegalArgumentException> { BoardImpl(6) }
    }

    @Test
    fun `newGame actually reshuffles - repeated calls are not the identity permutation every time`() {
        val board = BoardImpl(5)
        val results = (1..20).map {
            board.newGame(5)
            board.boardArray.toList()
        }

        assertNotEquals(setOf((0..24).toList()), results.toSet())
    }
}

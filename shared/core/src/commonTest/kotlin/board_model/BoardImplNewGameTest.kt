package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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
    fun `newGame(6) grows the board to a 6x6 shuffled permutation (GH-160)`() {
        val board = BoardImpl(4)

        board.newGame(6)

        assertEquals(6, board.squareSide)
        assertEquals((0..35).toList(), board.boardArray.sorted())
    }

    @Test
    fun `newGame resets the move count`() {
        val board = BoardImpl(4)
        board.shiftLeft(0)
        board.shiftUp(1)

        board.newGame(3)

        assertEquals(0, board.moveCount)
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
    fun `newGame(7) throws and leaves the board untouched`() {
        val board = BoardImpl(4)
        val before = board.boardArray.toList()

        assertFailsWith<IllegalArgumentException> { board.newGame(7) }

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
        assertFailsWith<IllegalArgumentException> { BoardImpl(7) }
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

    /** GH-74: covers every new-game surface that goes through newGame (size command, play-again, size dialogs). */
    @Test
    fun `newGame always produces a solvable board on odd sides`() {
        val board = BoardImpl(4, maxSide = 9)
        for (side in listOf(3, 5, 7, 9)) {
            repeat(200) {
                board.newGame(side)
                assertTrue(Solvability.isSolvable(board.boardArray), "side $side: ${board.boardArray.toList()}")
            }
        }
    }

    @Test
    fun `a board built with maxSide 9 grows to 7, 8 and 9, and still rejects 10 (GH-207)`() {
        val board = BoardImpl(4, maxSide = 9)

        for (side in 7..9) {
            board.newGame(side)
            assertEquals(side, board.squareSide)
            assertEquals((0 until side * side).toList(), board.boardArray.sorted())
        }
        assertFailsWith<IllegalArgumentException> { board.newGame(10) }
    }

    @Test
    fun `the default board still rejects 7 (GH-207)`() {
        assertFailsWith<IllegalArgumentException> { BoardImpl(4).newGame(7) }
    }
}

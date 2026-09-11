package view

import board_model.BoardImpl
import presenter.ConsolePresenterImpl
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * End-to-end guard through the real stack (ViewImpl + ConsolePresenterImpl + BoardImpl) - an
 * off-by-one only shows up where the view's 1-based translation meets board_model's
 * 0-based validation, which ViewImplCommandTest's FakePresenter can't exercise (GH-10).
 */
class OneBasedConsoleTest {

    private val identity = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    private fun stackWith(command: String): Triple<ViewImpl, BoardImpl, ByteArrayOutputStream> {
        val board = BoardImpl(4).apply { restoreState(identity) }
        val outputBuffer = ByteArrayOutputStream()
        val view = ViewImpl.create(
            input = BufferedReader(StringReader(command)),
            output = PrintStream(outputBuffer),
        ) { v -> ConsolePresenterImpl(v, board) }
        return Triple(view, board, outputBuffer)
    }

    @Test
    fun `left 1 rotates the first row`() {
        val (view, board, _) = stackWith("left 1\n")

        view.processCommand()

        assertEquals(
            listOf(1, 2, 3, 0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            board.boardArray.toList()
        )
    }

    @Test
    fun `left 4 rotates the last row on a 4x4 board, does not throw`() {
        val (view, board, _) = stackWith("left 4\n")

        view.processCommand()

        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 12),
            board.boardArray.toList()
        )
    }

    @Test
    fun `left 0 leaves the board untouched and surfaces Incorrect row`() {
        val (view, board, _) = stackWith("left 0\n")

        val error = assertFailsWith<IllegalArgumentException> { view.processCommand() }

        assertEquals("Incorrect row", error.message)
        assertEquals(identity.toList(), board.boardArray.toList())
    }

    @Test
    fun `left SQUARE_SIDE+1 leaves the board untouched and surfaces Incorrect row`() {
        val (view, board, _) = stackWith("left 5\n")

        val error = assertFailsWith<IllegalArgumentException> { view.processCommand() }

        assertEquals("Incorrect row", error.message)
        assertEquals(identity.toList(), board.boardArray.toList())
    }

    @Test
    fun `up 0 surfaces Incorrect column`() {
        val (view, board, _) = stackWith("up 0\n")

        val error = assertFailsWith<IllegalArgumentException> { view.processCommand() }

        assertEquals("Incorrect column", error.message)
        assertEquals(identity.toList(), board.boardArray.toList())
    }

    @Test
    fun `up SQUARE_SIDE+1 surfaces Incorrect column`() {
        val (view, board, _) = stackWith("up 5\n")

        val error = assertFailsWith<IllegalArgumentException> { view.processCommand() }

        assertEquals("Incorrect column", error.message)
        assertEquals(identity.toList(), board.boardArray.toList())
    }

    @Test
    fun `the rendered board and left 1's target row agree on the same first row`() {
        val (view, board, output) = stackWith("left 1\n")

        view.displayBoard(board.boardArray, board.SQUARE_SIDE)
        view.processCommand()

        val nl = System.lineSeparator()
        assertTrue(output.toString().startsWith("1\t2\t3\t4\t$nl"))
        assertEquals(
            listOf(1, 2, 3, 0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            board.boardArray.toList()
        )
    }
}

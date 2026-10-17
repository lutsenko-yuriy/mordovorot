package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-110: `shiftRow`/`shiftColumn` - a drag of several tiles is one move, Mordie counts every
 *  step, and the board is checked for solved only once it lands (D6). */
class ViewModelShiftStepsTest {

    /** 3x3, solved by shifting row 0 right once (or left twice). */
    private val nearlySolved = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)

    private val analytics = RecordingAnalyticsService()
    private val mordie = Mordie(FakeMordieSearchFactory())
    private val board = BoardImpl(4)
    private val viewModel = testViewModel(board, analytics = analytics, mordie = mordie)

    private fun boardAt(state: IntArray) = board.restoreState(state)

    private fun solvedCount() = analytics.events.count { it.name == "board_solved" }

    @Test
    fun `a multi-step row shift is that many shifts and one move`() {
        viewModel.newGame(4, trigger = "toolbar")
        val expected = BoardImpl(4).apply { restoreState(board.boardArray); shiftRight(1); shiftRight(1) }

        viewModel.shiftRow(1, 2, PointerShift.DRAG)

        assertContentEquals(expected.boardArray, board.boardArray)
        assertEquals(1, viewModel.moveCount())
        assertEquals(2, mordie.shifts)
    }

    @Test
    fun `a negative step count shifts left or up`() {
        viewModel.newGame(4, trigger = "toolbar")
        val expected = BoardImpl(4).apply { restoreState(board.boardArray); shiftLeft(0); shiftUp(3); shiftUp(3); shiftUp(3) }

        viewModel.shiftRow(0, -1)
        viewModel.shiftColumn(3, -3)

        assertContentEquals(expected.boardArray, board.boardArray)
        assertEquals(2, viewModel.moveCount())
        assertEquals(4, mordie.shifts)
    }

    @Test
    fun `a positive column step count shifts down`() {
        viewModel.newGame(4, trigger = "toolbar")
        val expected = BoardImpl(4).apply { restoreState(board.boardArray); shiftDown(2) }

        viewModel.shiftColumn(2, 1)

        assertContentEquals(expected.boardArray, board.boardArray)
    }

    @Test
    fun `passing through the solved board on the way fires no board_solved`() {
        boardAt(nearlySolved)

        viewModel.shiftRow(0, 3, PointerShift.DRAG) // solved after step 1, back where it started at 3

        assertEquals(0, solvedCount())
        assertContentEquals(nearlySolved, board.boardArray)
    }

    @Test
    fun `landing on the solved board fires board_solved once, with the drag's shift_input`() {
        boardAt(nearlySolved)

        viewModel.shiftRow(0, -2, PointerShift.DRAG)

        assertEquals(1, solvedCount())
        assertEquals("drag", analytics.events.single { it.name == "board_solved" }.properties["shift_input"])
    }

    @Test
    fun `zero steps changes nothing`() {
        viewModel.newGame(4, trigger = "toolbar")
        val before = board.boardArray.copyOf()

        viewModel.shiftRow(1, 0, PointerShift.DRAG)
        viewModel.shiftColumn(1, 0, PointerShift.DRAG)

        assertContentEquals(before, board.boardArray)
        assertEquals(0, viewModel.moveCount())
        assertEquals(0, mordie.shifts)
    }

    @Test
    fun `zero steps doesn't count toward shift_input`() {
        boardAt(nearlySolved)

        viewModel.shiftColumn(0, 0, PointerShift.DRAG)
        viewModel.shiftRight(0, PointerShift.ARROW)

        assertEquals("arrows", analytics.events.single { it.name == "board_solved" }.properties["shift_input"])
    }
}

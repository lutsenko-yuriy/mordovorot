package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
import testing.FakeMordieSearchFactory
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-222: Mordie's best solution so far, through the viewModel - Give Up, the screen and the analytics. */
class ViewModelBestSolutionTest {

    /** A 5x5 far from solved: the exact search would never finish it. */
    private val original = IntArray(25) { 24 - it }
    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()
    private val board = BoardImpl(5).apply { restoreState(original) }
    private val mordie = Mordie(factory)
    private val viewModel = testViewModel(board, analytics = analytics, mordie = mordie)

    private fun event(name: String) = analytics.events.single { it.name == name }.properties

    private fun fast(moves: Int) = SearchProgress(
        moves, found = true, elapsedMs = 4, solution = List(moves) { SolutionMove(isRow = true, index = 0, amount = 1) },
        ruledOut = 10, provenShortest = false, source = SolutionSource.FAST_SOLVER,
    )

    private fun start() {
        mordie.startFresh(original)
        factory.latest.progress = fast(40)
    }

    /** Row 0 rotated right once: `shiftLeft(0)` solves it. */
    private fun oneShiftFromSolved() = IntArray(25) { it }.also { it[0] = 4; it[1] = 0; it[2] = 1; it[3] = 2; it[4] = 3 }

    private fun fiveShifts() = repeat(5) { viewModel.shiftUp(2) }

    @Test
    fun `Give Up is dimmed before the first check and enabled after it on a 5x5`() {
        start()
        repeat(4) { viewModel.shiftUp(2) }
        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())

        viewModel.shiftUp(2)

        assertEquals(GiveUpButton.GIVE_UP, viewModel.giveUpButton())
    }

    @Test
    fun `finding the fast solution tracks its source and that it is not proven`() {
        start()

        fiveShifts()

        val found = event("mordie_solution_found")
        assertEquals("fast_solver", found["source"])
        assertEquals(false, found["proven_shortest"])
        assertEquals(40, found["solution_length"])
    }

    @Test
    fun `giving up takes the best at that moment and tracks whether it is proven`() {
        start()
        fiveShifts()
        factory.latest.progress = fast(31)

        viewModel.giveUp()

        val given = event("game_given_up")
        assertEquals(31, given["solution_length"])
        assertEquals(false, given["solution_proven_shortest"])
        assertEquals(31, viewModel.solution()!!.moves.size)
        assertTrue(factory.latest.cancelled)
    }

    @Test
    fun `giving up after the exact search finished tracks proven`() {
        start()
        fiveShifts()
        factory.latest.progress = SearchProgress(30, found = true, elapsedMs = 9, solution = List(30) { SolutionMove(true, 0, 1) })

        viewModel.giveUp()

        assertEquals(true, event("game_given_up")["solution_proven_shortest"])
    }

    @Test
    fun `solving reports whether Mordie's answer was proven shortest`() {
        start()
        fiveShifts()
        board.restoreState(oneShiftFromSolved())

        viewModel.shiftLeft(0)

        assertEquals(false, event("board_solved")["mordie_proven_shortest"])
    }

    @Test
    fun `board_solved has no proven flag when Mordie sat the game out`() {
        mordie.clear()
        board.restoreState(oneShiftFromSolved())

        viewModel.shiftLeft(0)

        assertNull(event("board_solved")["mordie_proven_shortest"])
        assertFalse(event("board_solved").containsKey("mordie_moves"))
    }
}

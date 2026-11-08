package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
import testing.FakeMordieSearchFactory
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-223: Mordie's lines for an answer that may not be the shortest, through the viewModel. */
class ViewModelShorterLineTest {

    /** A 5x5 far from solved: no exact search runs on it (GH-260). */
    private val original = IntArray(25) { 24 - it }
    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()
    private val board = BoardImpl(5).apply { restoreState(original) }
    private val mordie = Mordie(factory)
    private val viewModel = testViewModel(board, analytics = analytics, mordie = mordie)

    private fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }

    private fun answer(moves: Int, proven: Boolean = false) = SearchProgress(
        moves, found = true, elapsedMs = 4, solution = List(moves) { SolutionMove(isRow = true, index = 0, amount = 1) },
        ruledOut = 3, provenShortest = proven, source = SolutionSource.FAST_SOLVER,
    )

    private fun start(moves: Int = 40) {
        mordie.startFresh(original)
        factory.latest.progress = answer(moves)
    }

    private fun fiveShifts() = repeat(5) { viewModel.shiftUp(2) }

    /** Row 0 rotated right once: `shiftLeft(0)` solves it. */
    private fun oneShiftFromSolved() = IntArray(25) { it }.also { it[0] = 4; it[1] = 0; it[2] = 1; it[3] = 2; it[4] = 3 }

    @Test
    fun `a drop at a check tracks mordie_shorter_found with the old and new N`() {
        start()
        fiveShifts()
        factory.latest.progress = answer(31)

        fiveShifts()

        assertEquals(
            mapOf("board_size" to 5, "solution_length" to 31, "previous_length" to 40, "proven_shortest" to false),
            events("mordie_shorter_found").single(),
        )
        assertEquals("Wait, I found a shorter one: 31 moves now! Can you beat it?", viewModel.mordieLine())
    }

    @Test
    fun `the reveal itself sends no mordie_shorter_found`() {
        start()

        fiveShifts()

        assertEquals(0, events("mordie_shorter_found").size)
        assertEquals(1, events("mordie_solution_found").size)
    }

    @Test
    fun `giving up on a shorter best, and solving, send no mordie_shorter_found`() {
        start()
        fiveShifts()
        factory.latest.progress = answer(31)

        viewModel.giveUp()

        assertEquals(0, events("mordie_shorter_found").size)
    }

    @Test
    fun `Give Up stays enabled under the unproven and the shorter line`() {
        start()
        fiveShifts()
        assertEquals(GiveUpButton.GIVE_UP, viewModel.giveUpButton())

        factory.latest.progress = answer(31)
        fiveShifts()

        assertEquals(GiveUpButton.GIVE_UP, viewModel.giveUpButton())
    }

    @Test
    fun `beating an unproven answer reports beat_mordie and the new ending`() {
        start()
        fiveShifts()
        board.restoreState(oneShiftFromSolved())

        viewModel.shiftLeft(0)

        assertEquals("beat_mordie", events("board_solved").single()["mordie_result"])
        assertEquals("You beat me! 40 moves vs your ${board.moveCount}. Mine may not have been the shortest.", viewModel.mordieEnding())
    }

    @Test
    fun `beating a proven answer still reports machine`() {
        mordie.startFresh(original)
        factory.latest.progress = answer(12, proven = true)
        fiveShifts()
        board.restoreState(oneShiftFromSolved())
        board.moveCount.let { assertEquals(0, it) }

        viewModel.shiftLeft(0)

        assertEquals("machine", events("board_solved").single()["mordie_result"])
        assertEquals("I made 12 moves and you made 1 move! Are you a machine?", viewModel.mordieEnding())
    }
}

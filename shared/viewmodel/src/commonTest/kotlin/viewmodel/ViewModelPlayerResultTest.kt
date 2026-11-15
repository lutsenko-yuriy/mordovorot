package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import testing.FakeMordieSearchFactory
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-273: Mordie's line on the solution screen opens with how the player's game ended. */
class ViewModelPlayerResultTest {

    /** A 3x3 two moves from solved: row 0 right once, then column 0 down once. */
    private val original = intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 0, amount = 1))

    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()
    private val board = BoardImpl(3).apply { restoreState(original) }
    private val mordie = Mordie(factory)
    private val viewModel = testViewModel(board, analytics = analytics, mordie = mordie)

    private fun playerResult() = analytics.events.single { it.name == "screen_solution" }.properties["player_result"]

    private fun reveal() {
        mordie.startFresh(original)
        factory.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = path)
        repeat(5) { viewModel.shiftUp(2) }
    }

    @Test
    fun `giving up puts the moves made in front of Mordie's line`() {
        reveal()
        viewModel.shiftLeft(1)
        viewModel.shiftRight(2)
        assertEquals(3, viewModel.moveCount())

        viewModel.giveUp()

        assertEquals("You gave up after 3 moves. Here's how I did it in 2 moves!", viewModel.mordieLine())
        assertEquals("gave_up", playerResult())
    }

    @Test
    fun `giving up before any move says the player did not even start`() {
        reveal()
        viewModel.shiftUp(2)
        viewModel.shiftDown(2)
        viewModel.shiftUp(2)
        board.restoreState(original, 0)
        assertEquals(0, viewModel.moveCount())

        viewModel.giveUp()

        assertEquals("You did not even start this one. Here's how I did it in 2 moves!", viewModel.mordieLine())
    }

    @Test
    fun `Mordie's solution after a win puts the solved result in front`() {
        mordie.startFresh(original)
        factory.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = path)
        viewModel.shiftRight(0)
        viewModel.shiftDown(0)

        viewModel.showSolution()

        assertEquals("You solved it in 2 moves. Here's how I did it in 2 moves!", viewModel.mordieLine())
        assertEquals("solved", playerResult())
    }

    @Test
    fun `stepping the replay leaves the moves and the line alone`() {
        reveal()
        viewModel.shiftLeft(1)
        viewModel.giveUp()
        val line = viewModel.mordieLine()

        viewModel.replayNext()
        viewModel.replayNext()
        viewModel.replayRestart()

        assertEquals(0, viewModel.moveCount())
        assertEquals(line, viewModel.mordieLine())
        assertFalse(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `a new game forgets the result`() {
        reveal()
        viewModel.shiftLeft(1)
        viewModel.giveUp()

        viewModel.newGame(3, "new_command")

        assertNotEquals(true, viewModel.mordieLine()?.contains("gave up"))
    }
}

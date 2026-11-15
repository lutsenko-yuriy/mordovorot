package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedBoard
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-288: the `tutorial` flag `newGameFrom` sets for one game, sent on `board_solved` and `mordie_solution_found`. */
class ViewModelTutorialGameTest {

    /** A 3x3 one `shiftRight(0)` from solved. */
    private val nearSolved = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)
    private val analytics = RecordingAnalyticsService()

    private fun tutorialOf(event: String) = analytics.events.single { it.name == event }.properties["tutorial"]

    private fun solveNearBoard(viewModel: GameViewModel) {
        viewModel.newGameFrom(nearSolved)
        viewModel.shiftRight(0)
    }

    @Test
    fun `board_solved says tutorial true for a tutorial game`() {
        val viewModel = testViewModel(BoardImpl(3), analytics = analytics)

        viewModel.newGameFrom(nearSolved, tutorial = true)
        viewModel.shiftRight(0)

        assertEquals(true, tutorialOf("board_solved"))
    }

    @Test
    fun `board_solved says tutorial false for any other game`() {
        val viewModel = testViewModel(BoardImpl(3), analytics = analytics)

        solveNearBoard(viewModel)

        assertEquals(false, tutorialOf("board_solved"))
    }

    @Test
    fun `a new game clears the flag`() {
        val viewModel = testViewModel(BoardImpl(3), analytics = analytics)
        viewModel.newGameFrom(nearSolved, tutorial = true)

        viewModel.newGame(3, "toolbar")
        viewModel.newGameFrom(nearSolved)
        viewModel.shiftRight(0)

        assertEquals(false, tutorialOf("board_solved"))
    }

    @Test
    fun `a plain newGameFrom clears the flag`() {
        val viewModel = testViewModel(BoardImpl(3), analytics = analytics)
        viewModel.newGameFrom(nearSolved, tutorial = true)

        solveNearBoard(viewModel)

        assertEquals(false, tutorialOf("board_solved"))
    }

    @Test
    fun `a load and a repair clear the flag`() = runTest {
        val saves = FakeSaveRepository(
            mutableMapOf(
                "near" to SavedBoard(3, nearSolved, 0),
                "odd" to SavedBoard(3, intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8), 0),
            ),
        )
        val ui = FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR))
        val viewModel = testViewModel(BoardImpl(3), saves, analytics)

        ui.drive(viewModel) {
            viewModel.newGameFrom(nearSolved, tutorial = true)
            viewModel.loadGame("odd")
            viewModel.shiftRight(0)
            viewModel.newGameFrom(nearSolved, tutorial = true)
            viewModel.loadGame("near")
            viewModel.shiftRight(0)
        }

        assertEquals(listOf(false, false), analytics.events.filter { it.name == "board_solved" }.map { it.properties["tutorial"] })
    }

    @Test
    fun `mordie_solution_found carries the flag`() {
        val factory = FakeMordieSearchFactory()
        val original = IntArray(25) { 24 - it }
        val viewModel = testViewModel(BoardImpl(5), analytics = analytics, mordie = Mordie(factory))
        viewModel.newGameFrom(original, tutorial = true)
        factory.latest.progress = SearchProgress(
            40, found = true, elapsedMs = 4, solution = List(40) { SolutionMove(isRow = true, index = 0, amount = 1) },
        )

        repeat(5) { viewModel.shiftUp(2) }

        assertEquals(true, tutorialOf("mordie_solution_found"))
    }
}

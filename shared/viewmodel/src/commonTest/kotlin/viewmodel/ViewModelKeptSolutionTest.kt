package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
import storage.SavedMordie
import storage.SavedMove
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-224: saves keep whether Mordie's answer is proven shortest, so a reload ends the same way. */
class ViewModelKeptSolutionTest {

    /** A 3x3 two moves from solved: row 0 right once, then column 0 down once. */
    private val original = intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8)
    private val solved = IntArray(9) { it }
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 0, amount = 1))

    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()
    private val mordie = Mordie(factory)
    private val saves = FakeSaveRepository()
    private val solutionScreen = SolutionScreen()

    private fun viewModel(tiles: IntArray) =
        testViewModel(BoardImpl(3).apply { restoreState(tiles) }, saves, analytics, mordie = mordie, solutionScreen = solutionScreen)

    private suspend fun GameViewModelImpl.load(name: String) = FakeViewModelUi().drive(this) { loadGame(name) }
    private suspend fun GameViewModelImpl.save(name: String) = FakeViewModelUi().drive(this) { saveGame(name) }

    @Test
    fun `a solved save over an unproven answer reloads with the beat-Mordie ending`() = runTest {
        saves.save("s", solved, 3, 2, SavedMordie(original, 4, found = true, provenShortest = false, ruledOut = 1))
        val viewModel = viewModel(original)

        viewModel.load("s")

        assertEquals("You beat me! 4 moves vs your 2. Mine may not have been the shortest.", viewModel.mordieEnding())
    }

    @Test
    fun `a solved save over a proven answer still ends with Are you a machine`() = runTest {
        saves.save("s", solved, 3, 2, SavedMordie(original, 4, found = true))
        val viewModel = viewModel(original)

        viewModel.load("s")

        assertEquals("I made 4 moves and you made 2 moves! Are you a machine?", viewModel.mordieEnding())
    }

    @Test
    fun `a save on the solution screen over an unproven answer keeps the flag and R`() = runTest {
        val viewModel = viewModel(original)
        mordie.startFresh(original)
        factory.latest.progress = SearchProgress(
            2, found = true, elapsedMs = 0, solution = path, ruledOut = 0, provenShortest = false, source = SolutionSource.FAST_SOLVER,
        )
        repeat(5) { viewModel.shiftUp(2) }
        viewModel.giveUp()

        viewModel.save("s")

        val kept = saves.load("s")!!.mordie!!
        assertFalse(kept.provenShortest)
        assertEquals(0, kept.ruledOut)
        assertEquals(true, kept.solutionScreen)
    }

    @Test
    fun `loading that save reopens the screen with the answer still unproven and starts no search`() = runTest {
        val savedPath = path.map { SavedMove(it.isRow, it.index, it.amount) }
        saves.save("s", original, 3, 0, SavedMordie(original, 2, found = true, savedPath, solutionScreen = true, provenShortest = false, ruledOut = 0))
        val viewModel = viewModel(solved)

        viewModel.load("s")
        viewModel.solution()!!.let { assertEquals(true, it.atStart) }
        viewModel.save("again")

        assertEquals(0, factory.started.size)
        assertFalse(saves.load("again")!!.mordie!!.provenShortest)
    }
}

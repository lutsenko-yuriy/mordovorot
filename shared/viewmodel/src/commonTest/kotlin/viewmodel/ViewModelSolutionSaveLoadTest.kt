package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedBoard
import storage.SavedMordie
import storage.SavedMove
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-126: the solution screen across Save, Load, startup restore, New and a mode switch. */
class ViewModelSolutionSaveLoadTest {

    /** A 3x3 two moves from solved: row 0 right once, then column 0 down once. */
    private val original = intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 0, amount = 1))
    private val savedPath = path.map { SavedMove(it.isRow, it.index, it.amount) }
    private val screenSave = SavedMordie(original, 2, found = true, solution = savedPath, solutionScreen = true)

    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()
    private val board = BoardImpl(3).apply { restoreState(original) }
    private val mordie = Mordie(factory)
    private val solutionScreen = SolutionScreen()
    private val saves = FakeSaveRepository()

    private fun viewModel(startupRestoreDone: Boolean = true) =
        testViewModel(board, saves, analytics, startupRestoreDone, mordie = mordie, solutionScreen = solutionScreen)

    private fun event(name: String) = analytics.events.single { it.name == name }.properties

    /** Gives up on [original] after a reveal and a few shifts. */
    private fun GameViewModelImpl.givenUp() = apply {
        mordie.startFresh(original)
        factory.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = path)
        repeat(5) { shiftUp(2) }
        giveUp()
    }

    private suspend fun GameViewModelImpl.save(name: String) = FakeViewModelUi().drive(this) { saveGame(name) }
    private suspend fun GameViewModelImpl.load(name: String) = FakeViewModelUi().drive(this) { loadGame(name) }

    @Test
    fun `a save on the solution screen holds the original, no moves and the solution-screen flag`() = runTest {
        val viewModel = viewModel().givenUp()
        viewModel.playPause()
        viewModel.replayMove()

        viewModel.save("s")

        assertEquals(SavedBoard(3, original, 0, screenSave), saves.load("s"))
    }

    @Test
    fun `loading a solution-screen save reopens the screen, ready to play`() = runTest {
        saves.save("s", original, 3, 0, screenSave)
        val viewModel = viewModel()

        viewModel.load("s")

        assertEquals(Playback.READY, viewModel.solution()?.playback)
        assertNull(viewModel.solution()?.current)
        assertContentEquals(original, viewModel.boardState())
        assertEquals("Here's how I did it in 2 moves!", viewModel.mordieLine())
        assertEquals(mapOf("opened_from" to "load", "board_size" to 3, "solution_length" to 2), event("screen_solution"))
        assertFalse(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `restoring a solution-screen save at startup reopens the screen`() = runTest {
        saves.save("s", original, 3, 0, screenSave)
        val viewModel = viewModel(startupRestoreDone = false)

        FakeViewModelUi(confirmRestoreResponses = mutableListOf(true)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(Playback.READY, viewModel.solution()?.playback)
        assertEquals("startup_prompt", event("screen_solution")["opened_from"])
    }

    @Test
    fun `saving the screen, starting a new game and loading it brings the screen back`() = runTest {
        val viewModel = viewModel().givenUp()
        viewModel.save("s")
        viewModel.newGame(3, trigger = "toolbar")

        viewModel.load("s")

        assertEquals(Playback.READY, viewModel.solution()?.playback)
        assertEquals(listOf("1. Row 1 → ×1", "2. Col 1 ↓ ×1"), viewModel.solution()?.steps)
    }

    @Test
    fun `New leaves the solution screen`() {
        val viewModel = viewModel().givenUp()

        viewModel.newGame(3, trigger = "toolbar")

        assertNull(viewModel.solution())
        assertNull(solutionScreen.replay)
        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())
    }

    @Test
    fun `loading an ordinary save leaves the solution screen`() = runTest {
        saves.save("plain", original, 3, 4)
        val viewModel = viewModel().givenUp()

        viewModel.load("plain")

        assertNull(viewModel.solution())
        assertEquals(4, viewModel.moveCount())
    }

    @Test
    fun `a failed load keeps the solution screen`() = runTest {
        val viewModel = viewModel().givenUp()

        viewModel.load("missing")

        assertEquals(Playback.READY, viewModel.solution()?.playback)
    }

    @Test
    fun `a viewModel rebuilt around the session's SolutionScreen keeps the replay where it was`() {
        val before = viewModel().givenUp()
        before.playPause()
        before.replayMove()

        val after = viewModel()

        assertEquals(before.solution(), after.solution())
        assertEquals(Playback.PLAYING, after.solution()?.playback)
        assertContentEquals(before.boardState(), after.boardState())
    }
}

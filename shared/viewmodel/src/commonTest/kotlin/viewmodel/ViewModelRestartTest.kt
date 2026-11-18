package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedBoard
import storage.SavedMordie
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-349: Restart through the viewModel - the button, the board, Mordie and what the game keeps. */
class ViewModelRestartTest {

    /** A 3x3 two moves from solved: row 0 right once, then column 0 down once. */
    private val original = intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 0, amount = 1))

    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()
    private val currentSaveName = CurrentSaveName()
    private val labels = SessionTileLabels(TileLabelsPreference(testing.FakePreferenceRepository()))
    private val savedBoards = mutableMapOf<String, SavedBoard>()
    private val saves = FakeSaveRepository(savedBoards)
    private val viewModel = testViewModel(BoardImpl(3), saves, analytics, mordie = Mordie(factory), currentSaveName = currentSaveName, tileLabels = labels)

    private fun started() = viewModel.newGameFrom(original, sender = null)
    private fun solve() {
        viewModel.shiftRight(0)
        viewModel.shiftDown(0)
    }

    @Test
    fun `the button is dimmed at the start and live once something moved`() {
        started()
        assertEquals(RestartButton.DIMMED, viewModel.restartButton())

        viewModel.shiftLeft(1)

        assertEquals(RestartButton.LIVE, viewModel.restartButton())
    }

    @Test
    fun `a shift that is undone by another still counts as a move, so the button is live`() {
        started()
        viewModel.shiftLeft(1)
        viewModel.shiftRight(1)

        assertEquals(RestartButton.LIVE, viewModel.restartButton())
    }

    @Test
    fun `restarting puts the starting arrangement back at the starting count`() {
        started()
        viewModel.shiftLeft(1)
        viewModel.shiftUp(2)

        viewModel.restartGame("button", "dont_save")

        assertContentEquals(original, viewModel.boardState())
        assertEquals(0, viewModel.moveCount())
        assertEquals(RestartButton.DIMMED, viewModel.restartButton())
    }

    @Test
    fun `a dimmed button does nothing`() {
        started()

        viewModel.restartGame("shortcut", "not_asked")

        assertEquals(emptyList(), analytics.events)
        assertContentEquals(original, viewModel.boardState())
    }

    @Test
    fun `a won board restarts too, and can be solved again`() {
        started()
        solve()
        assertTrue(viewModel.isSolved())
        assertEquals(RestartButton.LIVE, viewModel.restartButton())

        viewModel.restartGame("button", "not_asked")

        assertFalse(viewModel.isSolved())
        assertContentEquals(original, viewModel.boardState())
        solve()
        assertEquals(2, analytics.events.count { it.name == "board_solved" })
    }

    @Test
    fun `the save name, the labels and the size stay`() {
        started()
        currentSaveName.name = "mine"
        labels.choose(TileLabels.LETTERS)
        viewModel.shiftLeft(1)

        viewModel.restartGame("button", "dont_save")

        assertEquals("mine", viewModel.currentSaveName())
        assertEquals(TileLabels.LETTERS, viewModel.tileLabels())
        assertEquals(3, viewModel.squareSide())
    }

    @Test
    fun `unsaved progress is cleared, and a later shift sets it again`() {
        started()
        viewModel.shiftLeft(1)
        assertTrue(viewModel.hasUnsavedProgress())

        viewModel.restartGame("button", "dont_save")
        assertFalse(viewModel.hasUnsavedProgress())

        viewModel.shiftLeft(1)
        assertTrue(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `shift sources count from the restart`() {
        started()
        viewModel.shiftLeft(1, ShiftKey.ARROW)
        viewModel.restartGame("button", "dont_save")

        viewModel.shiftRight(0, ShiftKey.HJKL)
        viewModel.shiftDown(0, ShiftKey.HJKL)

        assertEquals("hjkl", analytics.events.single { it.name == "board_solved" }.properties["shift_keys"])
    }

    @Test
    fun `board_solved counts the restarts of that game, and a new game starts the count again`() {
        started()
        viewModel.shiftLeft(1)
        viewModel.restartGame("button", "dont_save")
        viewModel.shiftLeft(1)
        viewModel.restartGame("button", "dont_save")
        solve()
        assertEquals(2, analytics.events.single { it.name == "board_solved" }.properties["restart_count"])

        started()
        solve()

        assertEquals(0, analytics.events.last { it.name == "board_solved" }.properties["restart_count"])
    }

    @Test
    fun `Mordie keeps his search, and Give Up is live again after the next check`() {
        started()
        factory.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = path)
        repeat(5) { viewModel.shiftUp(2) }
        assertEquals(GiveUpButton.GIVE_UP, viewModel.giveUpButton())

        viewModel.restartGame("button", "dont_save")

        assertEquals(1, factory.started.size)
        assertEquals(GiveUpButton.GIVE_UP, viewModel.giveUpButton())
    }

    @Test
    fun `from a won board his race resumes with a live Give Up`() {
        started()
        factory.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = path)
        solve()
        assertEquals(GiveUpButton.MORDIES_SOLUTION, viewModel.giveUpButton())

        viewModel.restartGame("button", "not_asked")

        assertEquals(GiveUpButton.GIVE_UP, viewModel.giveUpButton())
        assertNull(viewModel.mordieEnding())
    }

    @Test
    fun `the solution screen hides the button and a restart there does nothing`() {
        started()
        factory.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = path)
        repeat(5) { viewModel.shiftUp(2) }
        viewModel.giveUp()

        assertEquals(RestartButton.HIDDEN, viewModel.restartButton())
        viewModel.restartGame("shortcut", "not_asked")

        assertEquals(0, analytics.events.count { it.name == "game_restarted" })
    }

    @Test
    fun `a loaded save with Mordie's data restarts to his original at zero moves`() = runTest {
        savedBoards["m"] = SavedBoard(3, intArrayOf(3, 1, 2, 6, 4, 5, 0, 7, 8), 4, SavedMordie(original, 2, found = false))
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("m") }
        assertEquals(RestartButton.LIVE, viewModel.restartButton())

        viewModel.restartGame("button", "dont_save")

        assertContentEquals(original, viewModel.boardState())
        assertEquals(0, viewModel.moveCount())
        assertEquals("m", viewModel.currentSaveName())
    }

    @Test
    fun `a loaded save with no start known restarts to the board as loaded, dimmed until the first shift`() = runTest {
        val loaded = intArrayOf(3, 1, 2, 6, 4, 5, 0, 7, 8)
        savedBoards["old"] = SavedBoard(3, loaded, 7)
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("old") }
        assertEquals(RestartButton.DIMMED, viewModel.restartButton())

        viewModel.shiftLeft(1)
        assertEquals(RestartButton.LIVE, viewModel.restartButton())
        viewModel.restartGame("button", "dont_save")

        assertContentEquals(loaded, viewModel.boardState())
        assertEquals(7, viewModel.moveCount())
    }
}

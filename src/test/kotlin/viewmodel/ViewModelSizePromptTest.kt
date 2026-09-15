package viewmodel

import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers GH-44's startup size prompt - the interactive size choice [ViewModelImpl.restoreOnStartup]
 * raises when nothing was actually restored (no saves, declined, blank/EOF) and
 * `sizeChosenAtLaunch` wasn't set. See [ViewModelStartupRestoreTest] for the restore-matrix
 * itself, which this file assumes and doesn't re-cover.
 */
class ViewModelSizePromptTest {

    @Test
    fun `no saves, no --size given - prompts for a size and starts a new game at the chosen size`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(3))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf(4), ui.chooseBoardSizeCalls)
        assertEquals(listOf("newGame(3)"), board.calls)
        assertEquals(
            listOf(Event("new_game_size_selected", mapOf("size" to 3, "trigger" to "startup_prompt"))),
            analytics.events,
        )
    }

    @Test
    fun `saves exist but restore is declined - the size prompt runs after and starts a new game at the chosen size`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(mutableMapOf("foo" to storage.SavedBoard(4, IntArray(16) { it })))
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(confirmRestoreResponses = mutableListOf(false), chooseBoardSizeResponses = mutableListOf(5))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf(4), ui.chooseBoardSizeCalls)
        assertEquals(listOf("newGame(5)"), board.calls)
        assertEquals(
            Event("new_game_size_selected", mapOf("size" to 5, "trigger" to "startup_prompt")),
            analytics.events.last(),
        )
    }

    @Test
    fun `restore is accepted - the size prompt never runs, the restored save's size applies`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(mutableMapOf("foo" to storage.SavedBoard(4, IntArray(16) { it })))
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(confirmRestoreResponses = mutableListOf(true))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(emptyList(), ui.chooseBoardSizeCalls)
        assertEquals(false, analytics.events.any { it.name == "new_game_size_selected" })
    }

    @Test
    fun `--size given at launch - restore and the size prompt never run`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(mutableMapOf("foo" to storage.SavedBoard(4, IntArray(16) { it })))
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics, sizeChosenAtLaunch = true)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(emptyList(), ui.confirmRestoreCalls)
        assertEquals(emptyList(), ui.chooseBoardSizeCalls)
        assertEquals(emptyList(), board.calls)
        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `size prompt - blank input-EOF keeps the current size without starting a new game`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(emptyList(), board.calls)
        assertEquals(emptyList(), analytics.events)
    }
}

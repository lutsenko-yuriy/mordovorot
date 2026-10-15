package viewmodel

import storage.SaveFileFormatException
import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeViewModelUi
import testing.FakeSaveRepository
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [ViewModelImpl.restoreOnStartup]'s branch matrix (0/1/2+ save files), driven directly -
 * `restoreOnStartup` is public since GH-3 so each View can run it ahead of its own event loop
 * ([view.tui.TuiView]). See the plan comment on GH-6
 * for the full branch matrix.
 *
 * Driven via [FakeViewModelUi.drive] (GH-42 WU2) - `restoreOnStartup` suspends on
 * [ViewModelImpl.ask], so something must drain [ViewModel.uiRequests] concurrently, or the `ask`
 * call hangs forever.
 */
class ViewModelStartupRestoreTest {

    private val fourByFour = IntArray(16) { it }

    @Test
    fun `no saves - skips the restore prompt (the size prompt still runs - see ViewModelSizePromptTest)`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(emptyList(), ui.confirmRestoreCalls)
        assertEquals(emptyList(), ui.chooseSaveToRestoreCalls)
        assertEquals(emptyList(), analytics.events)
        assertEquals(emptyList(), board.calls)
    }

    @Test
    fun `exactly one save, user confirms - restores it`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val savedState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, savedState)))
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(confirmRestoreResponses = mutableListOf(true))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf("foo"), ui.confirmRestoreCalls)
        assertEquals(listOf("restoreState(${savedState.toList()})"), board.calls)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 1)),
                Event("load_command_used", mapOf("trigger" to "startup_prompt", "result" to "success")),
                Event("startup_restore_decision", mapOf("decision" to "restored", "save_file_count" to 1)),
            ),
            analytics.events,
        )
    }

    @Test
    fun `exactly one save, user declines - starts a new game`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, fourByFour)))
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(confirmRestoreResponses = mutableListOf(false), chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(emptyList(), board.calls)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 1)),
                Event("startup_restore_decision", mapOf("decision" to "new_game", "save_file_count" to 1)),
            ),
            analytics.events,
        )
    }

    @Test
    fun `two or more saves, exact name typed - restores it`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val barState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, barState),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(chooseSaveToRestoreResponses = mutableListOf("bar"))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf(listOf("bar", "foo")), ui.chooseSaveToRestoreCalls)
        assertEquals(listOf("restoreState(${barState.toList()})"), board.calls)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 2)),
                Event("load_command_used", mapOf("trigger" to "startup_prompt", "result" to "success")),
                Event("startup_restore_decision", mapOf("decision" to "restored", "save_file_count" to 2)),
            ),
            analytics.events,
        )
    }

    @Test
    fun `two or more saves, blank input - starts a new game`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, fourByFour),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(chooseSaveToRestoreResponses = mutableListOf(null), chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(emptyList(), board.calls)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 2)),
                Event("startup_restore_decision", mapOf("decision" to "new_game", "save_file_count" to 2)),
            ),
            analytics.events,
        )
    }

    @Test
    fun `two or more saves, an answer matching no save counts as declined`(): Unit = runBlocking {
        // The TUI only ever answers with a listed name (GH-79 removed the typed console prompt),
        // so an unmatched answer is treated like a blank one rather than re-prompted.
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, fourByFour),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(chooseSaveToRestoreResponses = mutableListOf("foo (4x4)"), chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(1, ui.chooseSaveToRestoreCalls.size)
        assertEquals(emptyList(), board.calls)
        assertEquals(emptyList(), ui.shownMessages)
        assertEquals(
            Event("startup_restore_decision", mapOf("decision" to "new_game", "save_file_count" to 2)),
            analytics.events.last(),
        )
    }

    @Test
    fun `EOF at the startup prompt starts a new game, same as a decline`(): Unit = runBlocking {
        // ViewModelImpl only ever sees the sentinel confirmRestore/chooseSaveToRestore response
        // for EOF (false/null) - identical to a clean decline/blank answer. This test only
        // confirms the viewModel doesn't hang or throw when that sentinel comes back.
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, fourByFour)))
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(confirmRestoreResponses = mutableListOf(false), chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(emptyList(), board.calls)
        assertEquals(
            Event("startup_restore_decision", mapOf("decision" to "new_game", "save_file_count" to 1)),
            analytics.events.last(),
        )
    }

    @Test
    fun `a save picked at startup that fails to load is reported as new_game, not restored`(): Unit = runBlocking {
        // Simulates a save deleted or corrupted between listSaves() and load() - e.g. another
        // process touched the saves/ directory between the two calls (audit finding on PR #14).
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, fourByFour)))
        saves.loadException = SaveFileFormatException("foo", "corrupted save file")
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(confirmRestoreResponses = mutableListOf(true), chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(emptyList(), board.calls)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 1)),
                Event("load_command_used", mapOf("trigger" to "startup_prompt", "result" to "error")),
                Event("startup_restore_decision", mapOf("decision" to "new_game", "save_file_count" to 1)),
            ),
            analytics.events,
        )
    }

    @Test
    fun `restoreOnStartup is a no-op on a second call - does not re-prompt or double-track`(): Unit = runBlocking {
        // A caller invoking it twice in one launch (e.g. by mistake, or two code paths both
        // calling it defensively) must not re-prompt the user or double-emit
        // startup_restore_prompt_shown/startup_restore_decision for what is still one launch
        // (audit finding on PR #20).
        val board = FakeBoardModel()
        val savedState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, savedState)))
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(confirmRestoreResponses = mutableListOf(true))

        ui.drive(viewModel) {
            viewModel.restoreOnStartup()
            viewModel.restoreOnStartup()
        }

        assertEquals(listOf("foo"), ui.confirmRestoreCalls)
        assertEquals(listOf("restoreState(${savedState.toList()})"), board.calls)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 1)),
                Event("load_command_used", mapOf("trigger" to "startup_prompt", "result" to "success")),
                Event("startup_restore_decision", mapOf("decision" to "restored", "save_file_count" to 1)),
            ),
            analytics.events,
        )
    }
}

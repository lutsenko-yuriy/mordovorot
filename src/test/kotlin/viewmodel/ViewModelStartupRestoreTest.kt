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
 * ([view.tui.TuiView], and [view.ViewImpl.play] since GH-42 WU3). See the plan comment on GH-6
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
    fun `two or more saves, unknown name re-prompts instead of falling back`(): Unit = runBlocking {
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
        val ui = FakeViewModelUi(chooseSaveToRestoreResponses = mutableListOf("nope", "bar"))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(2, ui.chooseSaveToRestoreCalls.size)
        assertEquals(listOf("restoreState(${barState.toList()})"), board.calls)
        assertEquals(1, analytics.events.count { it.name == "startup_restore_prompt_shown" })
        assertEquals(
            Event("startup_restore_decision", mapOf("decision" to "restored", "save_file_count" to 2)),
            analytics.events.last(),
        )
    }

    @Test
    fun `EOF at the startup prompt starts a new game, same as a decline - real EOF translation is covered in ViewImplCommandTest`(): Unit = runBlocking {
        // ViewModelImpl only ever sees the sentinel confirmRestore/chooseSaveToRestore response
        // for EOF (false/null) - identical to a clean decline/blank answer, since the actual
        // EOF-vs-decline distinction is made inside ViewImpl (see `confirmRestore returns false
        // on EOF instead of throwing` and its chooseSaveToRestore counterpart in
        // ViewImplCommandTest). This test only confirms the viewModel doesn't hang or throw when
        // that sentinel comes back - it can't, by construction, tell EOF apart from a decline at
        // this layer.
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
    fun `an unknown name typed with two or more saves shows a message before re-prompting`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, fourByFour),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi(chooseSaveToRestoreResponses = mutableListOf("nope", null), chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(
            listOf("No save named 'nope'. Available saves: bar (4x4), foo (4x4)"),
            ui.shownMessages,
        )
    }

    /** Audit finding on PR #54 (GH-44 WU4): the console prompt now prints "name (NxN)" for each
     *  save, so a user typing back exactly what they see must restore that save, not get stuck
     *  re-prompted forever against a bare-name-only match. */
    @Test
    fun `typing back the displayed name-and-size form restores that save, same as typing the bare name`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, fourByFour),
            ),
        )
        val viewModel = ViewModelImpl(board, saves)
        val ui = FakeViewModelUi(chooseSaveToRestoreResponses = mutableListOf("foo (4x4)"))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf("Loaded 'foo'."), ui.shownMessages)
        assertEquals(listOf("restoreState(${fourByFour.toList()})"), board.calls)
    }

    /** Round 2 audit finding on PR #54 (GH-44 WU4): an exact name match must win over another
     *  save's display-form match - only reachable with a hand-placed save file whose name
     *  contains a space (no in-app save path allows one), but a real save named literally
     *  "foo (4x4)" must still be selectable by typing its own name. */
    @Test
    fun `an exact name match wins over another save's colliding display form`(): Unit = runBlocking {
        // Same squareSide (4) on both - a mismatch would hit loadGame's separate size-mismatch
        // rejection (pre-WU5), which isn't what this test is about; barState distinguishes
        // which save actually got restored.
        val board = FakeBoardModel()
        val barState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "foo (4x4)" to SavedBoard(4, barState),
            ),
        )
        val viewModel = ViewModelImpl(board, saves)
        val ui = FakeViewModelUi(chooseSaveToRestoreResponses = mutableListOf("foo (4x4)"))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf("Loaded 'foo (4x4)'."), ui.shownMessages)
        assertEquals(listOf("restoreState(${barState.toList()})"), board.calls)
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

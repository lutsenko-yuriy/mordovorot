package presenter

import storage.SaveFileFormatException
import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeView
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [BasePresenter.offerStartupRestore]'s branch matrix (0/1/2+ save files), driven through
 * [ConsolePresenterImpl.play] - its first step, offering the restore before the play loop even
 * starts. Every board here starts already-solved ([FakeBoardModel.correct] = true), so `play()`
 * returns immediately afterward without entering its own loop, leaving each assertion scoped to
 * the restore flow alone. The idempotence guard and a same-sequence cross-check against the TUI
 * entry point ([TuiPresenterImpl.restoreOnStartup]) live in [TuiPresenterStartupRestoreTest]
 * (split GH-23 WU2). See the plan comment on GH-6 for the full branch matrix.
 */
class ConsolePresenterStartupRestoreTest {

    private val fourByFour = IntArray(16) { it }

    @Test
    fun `no saves - starts a new game immediately, no prompt`() {
        val board = FakeBoardModel().apply { correct = true }
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val view = FakeView()
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(emptyList(), view.confirmRestoreCalls)
        assertEquals(emptyList(), view.chooseSaveToRestoreCalls)
        assertEquals(emptyList(), analytics.events)
        assertEquals(emptyList(), board.calls)
    }

    @Test
    fun `exactly one save, user confirms - restores it`() {
        val board = FakeBoardModel().apply { correct = true }
        val savedState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, savedState)))
        val analytics = RecordingAnalyticsService()
        val view = FakeView(confirmRestoreResponses = mutableListOf(true))
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(listOf("foo"), view.confirmRestoreCalls)
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
    fun `exactly one save, user declines - starts a new game`() {
        val board = FakeBoardModel().apply { correct = true }
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, fourByFour)))
        val analytics = RecordingAnalyticsService()
        val view = FakeView(confirmRestoreResponses = mutableListOf(false))
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

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
    fun `two or more saves, exact name typed - restores it`() {
        val board = FakeBoardModel().apply { correct = true }
        val barState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, barState),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val view = FakeView(chooseSaveToRestoreResponses = mutableListOf("bar"))
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(listOf(listOf("bar", "foo")), view.chooseSaveToRestoreCalls)
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
    fun `two or more saves, blank input - starts a new game`() {
        val board = FakeBoardModel().apply { correct = true }
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, fourByFour),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val view = FakeView(chooseSaveToRestoreResponses = mutableListOf(null))
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

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
    fun `two or more saves, unknown name re-prompts instead of falling back`() {
        val board = FakeBoardModel().apply { correct = true }
        val barState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, barState),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val view = FakeView(chooseSaveToRestoreResponses = mutableListOf("nope", "bar"))
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(2, view.chooseSaveToRestoreCalls.size)
        assertEquals(listOf("restoreState(${barState.toList()})"), board.calls)
        assertEquals(1, analytics.events.count { it.name == "startup_restore_prompt_shown" })
        assertEquals(
            Event("startup_restore_decision", mapOf("decision" to "restored", "save_file_count" to 2)),
            analytics.events.last(),
        )
    }

    @Test
    fun `EOF at the startup prompt starts a new game, same as a decline - real EOF translation is covered in ViewImplCommandTest`() {
        // BasePresenter only ever sees the sentinel View.confirmRestore/chooseSaveToRestore
        // return for EOF (false/null) - identical to a clean decline/blank answer, since the
        // actual EOF-vs-decline distinction is made inside ViewImpl (see
        // `confirmRestore returns false on EOF instead of throwing` and its chooseSaveToRestore
        // counterpart in ViewImplCommandTest). This test only confirms the presenter doesn't
        // hang or throw when that sentinel comes back - it can't, by construction, tell EOF
        // apart from a decline at this layer.
        val board = FakeBoardModel().apply { correct = true }
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, fourByFour)))
        val analytics = RecordingAnalyticsService()
        val view = FakeView(confirmRestoreResponses = mutableListOf(false))
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(emptyList(), board.calls)
        assertEquals(
            Event("startup_restore_decision", mapOf("decision" to "new_game", "save_file_count" to 1)),
            analytics.events.last(),
        )
    }

    @Test
    fun `a save picked at startup that fails to load is reported as new_game, not restored`() {
        // Simulates a save deleted or corrupted between listSaves() and load() - e.g. another
        // process touched the saves/ directory between the two calls (audit finding on PR #14).
        val board = FakeBoardModel().apply { correct = true }
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, fourByFour)))
        saves.loadException = SaveFileFormatException("foo", "corrupted save file")
        val analytics = RecordingAnalyticsService()
        val view = FakeView(confirmRestoreResponses = mutableListOf(true))
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

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
    fun `an unknown name typed with two or more saves shows a message before re-prompting`() {
        val board = FakeBoardModel().apply { correct = true }
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fourByFour),
                "bar" to SavedBoard(4, fourByFour),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val view = FakeView(chooseSaveToRestoreResponses = mutableListOf("nope", null))
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(
            listOf("No save named 'nope'. Available saves: bar, foo"),
            view.shownMessages,
        )
    }
}

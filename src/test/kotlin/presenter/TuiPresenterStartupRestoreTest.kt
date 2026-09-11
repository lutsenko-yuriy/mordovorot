package presenter

import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeView
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [TuiPresenterImpl.restoreOnStartup] as its own public entry point - `restoreOnStartup`
 * is public since GH-3 so [view.tui.TuiView] can call it ahead of its own event loop. The full
 * branch matrix (0/1/2+ saves, decline, unknown name, etc.) is exercised once, against
 * [ConsolePresenterImpl.play], in [ConsolePresenterStartupRestoreTest] - both entry points share
 * [BasePresenter.offerStartupRestore], so this file only needs the idempotence guard, which
 * doubles as the cross-check that the TUI entry point runs the same prompt/track/restore
 * sequence the console entry point does, guarding against the two drifting (split GH-23 WU2).
 */
class TuiPresenterStartupRestoreTest {

    @Test
    fun `restoreOnStartup is a no-op on a second call - does not re-prompt or double-track`() {
        // A caller invoking it twice in one launch (e.g. by mistake, or two code paths both
        // calling it defensively) must not re-prompt the user or double-emit
        // startup_restore_prompt_shown/startup_restore_decision for what is still one launch
        // (audit finding on PR #20).
        val board = FakeBoardModel().apply { correct = true }
        val savedState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, savedState)))
        val analytics = RecordingAnalyticsService()
        val view = FakeView(confirmRestoreResponses = mutableListOf(true))
        val presenter = TuiPresenterImpl(view, board, saves, analytics)

        presenter.restoreOnStartup()
        presenter.restoreOnStartup()

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
}

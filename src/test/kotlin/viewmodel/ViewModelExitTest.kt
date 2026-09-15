package viewmodel

import testing.FakeBoardModel
import testing.FakeViewModelUi
import testing.FakeSaveRepository
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Covers [ViewModelImpl.exitGame] (the `exit`/`quit` command) directly against [ViewModelImpl],
 * driven with no `play()` loop involved - [exitGame] itself never calls back into it. The one
 * play-loop-specific assertion this used to carry ("a failed save doesn't end the session") is
 * [view.ViewImplPlayTest]'s `a failed save during exit does not quit - play keeps looping`
 * instead (split GH-23 WU2, since that's a property of the console play loop, not
 * of [exitGame] itself). The `exit`/`quit` -> `viewModel.exitGame()` dispatch itself is covered
 * separately in ViewImplCommandTest. See the analytics plan on GH-12 for `exit_command_used`.
 *
 * Driven via [FakeViewModelUi.drive] (GH-42 WU2) - [exitGame] suspends on
 * [ViewModelImpl.ask], so something must be draining [ViewModel.uiRequests] concurrently or the
 * `ask` call hangs forever.
 */
class ViewModelExitTest {

    @Test
    fun `exit, save declined - ends play without saving`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(confirmSaveBeforeExitResponses = mutableListOf(false))
        val viewModel = ViewModelImpl(board, saves, analytics)

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.exitGame() } }

        assertEquals(emptyList(), saves.saveCalls)
        assertEquals(
            listOf(Event("exit_command_used", mapOf("save_choice" to "declined"))),
            analytics.events,
        )
    }

    @Test
    fun `exit, save confirmed with a name - saves then ends play`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("foo"),
        )
        val viewModel = ViewModelImpl(board, saves, analytics)

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.exitGame() } }

        assertEquals(1, saves.saveCalls.size)
        val (name, state, squareSide) = saves.saveCalls[0]
        assertEquals("foo", name)
        assertEquals(board.boardArray.toList(), state.toList())
        assertEquals(board.SQUARE_SIDE, squareSide)
        assertEquals(
            listOf(
                Event("save_command_used", mapOf("result" to "success", "overwrote_existing" to false)),
                Event("exit_command_used", mapOf("save_choice" to "saved")),
            ),
            analytics.events,
        )
    }

    @Test
    fun `exit, save confirmed but the save itself fails - returns normally and says so, without quitting`(): Unit = runBlocking {
        // A failed save must not compound into a lost session on top of it (audit round 2 on
        // PR #15: quitting anyway after a failed save was strictly worse than not asking at
        // all) - exitGame returns normally instead of throwing ExitRequestedException.
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        saves.saveException = RuntimeException("disk full")
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("foo"),
        )
        val viewModel = ViewModelImpl(board, saves, analytics)

        ui.drive(viewModel) { viewModel.exitGame() }

        assertEquals(
            listOf(Event("save_command_used", mapOf("result" to "error"))),
            analytics.events,
        )
        // saveGame's own error message, then exitGame's explicit "your quit was cancelled" -
        // without the second line the board redisplay is the only cue anything happened, easily
        // misread as the save having worked (audit round 3 on PR #15).
        assertEquals(
            listOf(
                "Could not save as 'foo': disk full",
                "Not quitting - your game is still running. Fix the problem and try exit again, " +
                    "or answer n to quit without saving.",
            ),
            ui.shownMessages,
        )
    }

    @Test
    fun `exit, a name with spaces re-prompts instead of discarding the save request, and a valid retry saves`(): Unit = runBlocking {
        // A name with spaces would be unloadable via `load <name>` (its command-line parsing
        // requires exactly one token) - audit finding on PR #15. Round 1 declined the save on
        // an invalid name; round 2 found that lost the session's only save attempt, so it
        // re-prompts instead.
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("my game", "foo"),
        )
        val viewModel = ViewModelImpl(board, saves, analytics)

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.exitGame() } }

        assertEquals(1, saves.saveCalls.size)
        assertEquals("foo", saves.saveCalls[0].first)
        assertEquals(
            listOf(
                "'my game' isn't a usable save name (no spaces, path separators, or '..') - " +
                    "try again, or press Enter to skip saving.",
                "Saved as 'foo'.",
            ),
            ui.shownMessages,
        )
        assertEquals(
            listOf(
                Event("save_command_used", mapOf("result" to "success", "overwrote_existing" to false)),
                Event("exit_command_used", mapOf("save_choice" to "saved")),
            ),
            analytics.events,
        )
    }

    @Test
    fun `exit, a name with spaces re-prompted then left blank - declines without saving`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("my game", null),
        )
        val viewModel = ViewModelImpl(board, saves, analytics)

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.exitGame() } }

        assertEquals(emptyList(), saves.saveCalls)
        assertEquals(
            listOf(Event("exit_command_used", mapOf("save_choice" to "declined"))),
            analytics.events,
        )
    }

    @Test
    fun `exit, a name with a path separator re-prompts rather than aborting the quit as a save failure`(): Unit = runBlocking {
        // requireSafeName-style names ('/', '\', '..') are just as unusable as a name with
        // spaces, and must get the same re-prompt treatment rather than falling through to
        // saveGame() and being misclassified as a genuine save failure (audit round 3 on
        // PR #15 - a slash previously took the "abort the quit" path meant for I/O failures).
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("a/b", "foo"),
        )
        val viewModel = ViewModelImpl(board, saves, analytics)

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.exitGame() } }

        assertEquals(1, saves.saveCalls.size)
        assertEquals("foo", saves.saveCalls[0].first)
        assertEquals(
            listOf(
                Event("save_command_used", mapOf("result" to "success", "overwrote_existing" to false)),
                Event("exit_command_used", mapOf("save_choice" to "saved")),
            ),
            analytics.events,
        )
    }

    @Test
    fun `exit, save confirmed but a blank or EOF name - ends play without saving`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf(null),
        )
        val viewModel = ViewModelImpl(board, saves, analytics)

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.exitGame() } }

        assertEquals(emptyList(), saves.saveCalls)
        assertEquals(
            listOf(Event("exit_command_used", mapOf("save_choice" to "declined"))),
            analytics.events,
        )
    }
}

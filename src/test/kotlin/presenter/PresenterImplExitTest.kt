package presenter

import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeView
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [PresenterImpl.exitGame] (the `exit`/`quit` command), exercised through
 * [PresenterImpl.play] so that its play-loop-terminating behaviour is verified alongside its
 * save-before-quitting dialogue. The `exit`/`quit` -> `presenter.exitGame()` dispatch itself is
 * covered separately in ViewImplCommandTest - here the scripted [FakeView] command directly
 * calls `exitGame()`, standing in for that dispatch. See the analytics plan on GH-12 for
 * `exit_command_used`.
 */
class PresenterImplExitTest {

    @Test
    fun `exit, save declined - ends play without saving`() {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(false),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(emptyList(), saves.saveCalls)
        assertEquals(
            listOf(Event("exit_command_used", mapOf("save_choice" to "declined"))),
            analytics.events,
        )
    }

    @Test
    fun `exit, save confirmed with a name - saves then ends play`() {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("foo"),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

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
    fun `exit, save confirmed but the save itself fails - does not quit, and says so`() {
        // A failed save must not compound into a lost session on top of it (audit round 2 on
        // PR #15: quitting anyway after a failed save was strictly worse than not asking at
        // all). The second scripted command ends play() normally, standing in for whatever the
        // user does next (retry `exit`, fix the underlying issue, or just keep playing).
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        saves.saveException = RuntimeException("disk full")
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }, { board.correct = true }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("foo"),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(2, view.processCommandCallCount)
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
            view.shownMessages,
        )
    }

    @Test
    fun `exit, a name with spaces re-prompts instead of discarding the save request, and a valid retry saves`() {
        // A name with spaces would be unloadable via `load <name>` (its command-line parsing
        // requires exactly one token) - audit finding on PR #15. Round 1 declined the save on
        // an invalid name; round 2 found that lost the session's only save attempt, so it
        // re-prompts instead.
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("my game", "foo"),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(1, saves.saveCalls.size)
        assertEquals("foo", saves.saveCalls[0].first)
        assertEquals(
            listOf(
                "'my game' isn't a usable save name (no spaces, path separators, or '..') - " +
                    "try again, or press Enter to skip saving.",
                "Saved as 'foo'.",
            ),
            view.shownMessages,
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
    fun `exit, a name with spaces re-prompted then left blank - declines without saving`() {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("my game", null),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(emptyList(), saves.saveCalls)
        assertEquals(
            listOf(Event("exit_command_used", mapOf("save_choice" to "declined"))),
            analytics.events,
        )
    }

    @Test
    fun `exit, a name with a path separator re-prompts rather than aborting the quit as a save failure`() {
        // requireSafeName-style names ('/', '\', '..') are just as unusable as a name with
        // spaces, and must get the same re-prompt treatment rather than falling through to
        // saveGame() and being misclassified as a genuine save failure (audit round 3 on
        // PR #15 - a slash previously took the "abort the quit" path meant for I/O failures).
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("a/b", "foo"),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

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
    fun `exit, save confirmed but a blank or EOF name - ends play without saving`() {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf(null),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(emptyList(), saves.saveCalls)
        assertEquals(
            listOf(Event("exit_command_used", mapOf("save_choice" to "declined"))),
            analytics.events,
        )
    }
}

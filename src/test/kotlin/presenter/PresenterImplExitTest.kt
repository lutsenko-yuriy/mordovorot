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
    fun `exit, save confirmed but the save itself fails - reports declined, not saved`() {
        // A corrupted/unsafe name or filesystem failure - saveGame's own try/catch means this
        // wouldn't otherwise be visible to exitGame() without saveGame returning a real result
        // (audit finding on PR #15: save_choice was always "saved" once a name was typed).
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        saves.saveException = RuntimeException("disk full")
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("foo"),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(
            listOf(
                Event("save_command_used", mapOf("result" to "error")),
                Event("exit_command_used", mapOf("save_choice" to "declined")),
            ),
            analytics.events,
        )
    }

    @Test
    fun `exit, save confirmed with a name containing spaces - declines without saving, shows a message`() {
        // A name with spaces would be unloadable via `load <name>` (its command-line parsing
        // requires exactly one token) - audit finding on PR #15.
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        lateinit var presenter: PresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("my game"),
        )
        presenter = PresenterImpl(view, board, saves, analytics)

        presenter.play()

        assertEquals(emptyList(), saves.saveCalls)
        assertEquals(
            listOf("Save name can't contain spaces - quitting without saving."),
            view.shownMessages,
        )
        assertEquals(
            listOf(Event("exit_command_used", mapOf("save_choice" to "declined"))),
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

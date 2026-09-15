package presenter

import board_model.BoardImpl
import storage.SaveFileFormatException
import testing.FakePresenterUi
import testing.FakeSaveRepository
import testing.RecordingAnalyticsService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Covers [PresenterImpl.saveGame]/[PresenterImpl.loadGame] (originally GH-6's `save`/`load`
 *  commands, WU3), shared identically by both UIs (split GH-23). Driven via [FakePresenterUi.drive]
 *  (GH-42 WU2) - both methods always call `showMessage`, which suspends on [PresenterImpl.ask],
 *  so every test here needs something draining [Presenter.uiRequests] concurrently, not just the
 *  ones that previously asserted on a message. */
class PresenterSaveLoadTest {

    @Test
    fun `saveGame creates a new file when none exists`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(1, 0, 3, 2)) }
        val presenter = PresenterImpl(board, saves, analytics)
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.saveGame("foo") }

        assertEquals(1, saves.saveCalls.size)
        val (name, state, squareSide) = saves.saveCalls[0]
        assertEquals("foo", name)
        assertEquals(listOf(1, 0, 3, 2), state.toList())
        assertEquals(2, squareSide)
        assertEquals(
            listOf(
                RecordingAnalyticsService.Event(
                    "save_command_used",
                    mapOf("result" to "success", "overwrote_existing" to false),
                ),
            ),
            analytics.events,
        )
    }

    @Test
    fun `saveGame overwrites an existing file`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.save("foo", intArrayOf(0, 1, 2, 3), 2)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(3, 2, 1, 0)) }
        val presenter = PresenterImpl(board, saves, analytics)
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.saveGame("foo") }

        assertEquals(listOf(3, 2, 1, 0), saves.load("foo")!!.state.toList())
        assertEquals(
            listOf(
                RecordingAnalyticsService.Event(
                    "save_command_used",
                    mapOf("result" to "success", "overwrote_existing" to true),
                ),
            ),
            analytics.events,
        )
    }

    @Test
    fun `saveGame surfaces a message and tracks result=error instead of crashing when the repository throws`(): Unit = runBlocking {
        val saves = FakeSaveRepository().apply { saveException = IllegalArgumentException("Save name must not be blank") }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = PresenterImpl(board, saves, analytics)
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.saveGame("..") }

        assertTrue(ui.shownMessages.any { it.contains("Save name must not be blank") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("save_command_used", mapOf("result" to "error"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame restores the board on a known save name`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.save("foo", intArrayOf(3, 2, 1, 0), 2)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = PresenterImpl(board, saves, analytics)
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.loadGame("foo") }

        assertEquals(listOf(3, 2, 1, 0), board.boardArray.toList())
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "success"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame leaves the board unchanged on an unknown save name`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.save("bar", intArrayOf(3, 2, 1, 0), 2)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = PresenterImpl(board, saves, analytics)
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.loadGame("missing") }

        assertEquals(listOf(0, 1, 2, 3), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains("missing") && it.contains("bar") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame with no saves at all still messages cleanly`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = PresenterImpl(board, saves, RecordingAnalyticsService())
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.loadGame("missing") }

        assertTrue(ui.shownMessages.any { it.contains("No saves available") })
    }

    @Test
    fun `loadGame surfaces a message and tracks result=size_mismatch when the save's board size differs`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.save("small", intArrayOf(0, 1, 2, 3), 2)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState((0..15).toList().toIntArray()) }
        val presenter = PresenterImpl(board, saves, analytics)
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.loadGame("small") }

        assertEquals((0..15).toList(), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains("small") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "size_mismatch"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame still reports not_found, with a degraded message, when listing available saves fails`(): Unit = runBlocking {
        // Regression guard: the not_found branch's availableSavesMessage() call can throw
        // (e.g. an unreadable saves/ directory) independently of saves.load() itself - that
        // shouldn't turn an actual not_found result into a spurious second "error" event
        // (audit on PR #13).
        val saves = FakeSaveRepository().apply {
            listSavesException = java.io.IOException("permission denied")
        }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = PresenterImpl(board, saves, analytics)
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.loadGame("missing") }

        assertEquals(listOf(0, 1, 2, 3), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains("Could not list available saves") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame surfaces a message and tracks result=error instead of crashing on a corrupted save file`(): Unit = runBlocking {
        val saves = FakeSaveRepository().apply {
            loadException = SaveFileFormatException("corrupt", "missing board values")
        }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = PresenterImpl(board, saves, analytics)
        val ui = FakePresenterUi()

        ui.drive(presenter) { presenter.loadGame("corrupt") }

        assertEquals(listOf(0, 1, 2, 3), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains("corrupt") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "error"))),
            analytics.events,
        )
    }
}

package presenter

import board_model.BoardImpl
import storage.SaveFileFormatException
import testing.FakeSaveRepository
import testing.FakeView
import testing.RecordingAnalyticsService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Covers GH-6's `save`/`load` console commands (WU3). */
class PresenterImplSaveLoadTest {

    @Test
    fun `saveGame creates a new file when none exists`() {
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(1, 0, 3, 2)) }
        val presenter = ConsolePresenterImpl(FakeView(), board, saves, analytics)

        presenter.saveGame("foo")

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
    fun `saveGame overwrites an existing file`() {
        val saves = FakeSaveRepository()
        saves.save("foo", intArrayOf(0, 1, 2, 3), 2)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(3, 2, 1, 0)) }
        val presenter = ConsolePresenterImpl(FakeView(), board, saves, analytics)

        presenter.saveGame("foo")

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
    fun `saveGame surfaces a message and tracks result=error instead of crashing when the repository throws`() {
        val saves = FakeSaveRepository().apply { saveException = IllegalArgumentException("Save name must not be blank") }
        val analytics = RecordingAnalyticsService()
        val view = FakeView()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.saveGame("..")

        assertTrue(view.shownMessages.any { it.contains("Save name must not be blank") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("save_command_used", mapOf("result" to "error"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame restores the board on a known save name`() {
        val saves = FakeSaveRepository()
        saves.save("foo", intArrayOf(3, 2, 1, 0), 2)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = ConsolePresenterImpl(FakeView(), board, saves, analytics)

        presenter.loadGame("foo")

        assertEquals(listOf(3, 2, 1, 0), board.boardArray.toList())
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "success"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame leaves the board unchanged on an unknown save name`() {
        val saves = FakeSaveRepository()
        saves.save("bar", intArrayOf(3, 2, 1, 0), 2)
        val analytics = RecordingAnalyticsService()
        val view = FakeView()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.loadGame("missing")

        assertEquals(listOf(0, 1, 2, 3), board.boardArray.toList())
        assertTrue(view.shownMessages.any { it.contains("missing") && it.contains("bar") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame with no saves at all still messages cleanly`() {
        val saves = FakeSaveRepository()
        val view = FakeView()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = ConsolePresenterImpl(view, board, saves, RecordingAnalyticsService())

        presenter.loadGame("missing")

        assertTrue(view.shownMessages.any { it.contains("No saves available") })
    }

    @Test
    fun `loadGame surfaces a message and tracks result=size_mismatch when the save's board size differs`() {
        val saves = FakeSaveRepository()
        saves.save("small", intArrayOf(0, 1, 2, 3), 2)
        val analytics = RecordingAnalyticsService()
        val view = FakeView()
        val board = BoardImpl(4).apply { restoreState((0..15).toList().toIntArray()) }
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.loadGame("small")

        assertEquals((0..15).toList(), board.boardArray.toList())
        assertTrue(view.shownMessages.any { it.contains("small") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "size_mismatch"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame still reports not_found, with a degraded message, when listing available saves fails`() {
        // Regression guard: the not_found branch's availableSavesMessage() call can throw
        // (e.g. an unreadable saves/ directory) independently of saves.load() itself - that
        // shouldn't turn an actual not_found result into a spurious second "error" event
        // (audit on PR #13).
        val saves = FakeSaveRepository().apply {
            listSavesException = java.io.IOException("permission denied")
        }
        val analytics = RecordingAnalyticsService()
        val view = FakeView()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.loadGame("missing")

        assertEquals(listOf(0, 1, 2, 3), board.boardArray.toList())
        assertTrue(view.shownMessages.any { it.contains("Could not list available saves") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame surfaces a message and tracks result=error instead of crashing on a corrupted save file`() {
        val saves = FakeSaveRepository().apply {
            loadException = SaveFileFormatException("corrupt", "missing board values")
        }
        val analytics = RecordingAnalyticsService()
        val view = FakeView()
        val board = BoardImpl(2).apply { restoreState(intArrayOf(0, 1, 2, 3)) }
        val presenter = ConsolePresenterImpl(view, board, saves, analytics)

        presenter.loadGame("corrupt")

        assertEquals(listOf(0, 1, 2, 3), board.boardArray.toList())
        assertTrue(view.shownMessages.any { it.contains("corrupt") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "error"))),
            analytics.events,
        )
    }
}

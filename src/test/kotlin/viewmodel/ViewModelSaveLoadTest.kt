package viewmodel

import board_model.BoardImpl
import storage.SaveFileFormatException
import testing.FakeViewModelUi
import testing.FakeSaveRepository
import testing.RecordingAnalyticsService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Covers [ViewModelImpl.saveGame]/[ViewModelImpl.loadGame] (originally GH-6's `save`/`load`
 *  commands, WU3), shared identically by both UIs (split GH-23). Driven via [FakeViewModelUi.drive]
 *  (GH-42 WU2) - both methods always call `showMessage`, which suspends on [ViewModelImpl.ask],
 *  so every test here needs something draining [ViewModel.uiRequests] concurrently, not just the
 *  ones that previously asserted on a message. */
class ViewModelSaveLoadTest {

    @Test
    fun `saveGame creates a new file when none exists`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.saveGame("foo") }

        assertEquals(1, saves.saveCalls.size)
        val (name, state, squareSide) = saves.saveCalls[0]
        assertEquals("foo", name)
        assertEquals(listOf(1, 0, 2, 3, 4, 5, 6, 7, 8), state.toList())
        assertEquals(3, squareSide)
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
        saves.save("foo", intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), 3)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0)) }
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.saveGame("foo") }

        assertEquals(listOf(8, 7, 6, 5, 4, 3, 2, 1, 0), saves.load("foo")!!.state.toList())
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
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.saveGame("..") }

        assertTrue(ui.shownMessages.any { it.contains("Save name must not be blank") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("save_command_used", mapOf("result" to "error"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame restores the board on a known save name`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.save("foo", intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), 3)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("foo") }

        assertEquals(listOf(8, 7, 6, 5, 4, 3, 2, 1, 0), board.boardArray.toList())
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "success"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame leaves the board unchanged on an unknown save name`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.save("bar", intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), 3)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("missing") }

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains("missing") && it.contains("bar") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame with no saves at all still messages cleanly`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = ViewModelImpl(board, saves, RecordingAnalyticsService())
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("missing") }

        assertTrue(ui.shownMessages.any { it.contains("No saves available") })
    }

    @Test
    fun `loadGame resizes the board to match a save whose size differs, instead of rejecting it`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.save("small", intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 8), 3)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState((0..15).toList().toIntArray()) }
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("small") }

        assertEquals(3, board.squareSide)
        assertEquals(listOf(3, 2, 1, 0, 7, 6, 5, 4, 8), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains("Loaded 'small'") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "success"))),
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
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("missing") }

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), board.boardArray.toList())
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
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = ViewModelImpl(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("corrupt") }

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains("corrupt") })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "error"))),
            analytics.events,
        )
    }
}

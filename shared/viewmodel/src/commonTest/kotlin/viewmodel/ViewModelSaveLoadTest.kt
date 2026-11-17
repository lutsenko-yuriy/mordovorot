package viewmodel

import board_model.BoardImpl
import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import storage.SaveFileFormatException
import storage.SaveFromNewerVersionException
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** Covers [GameViewModelImpl.saveGame]/[GameViewModelImpl.loadGame] (originally GH-6's `save`/`load`
 *  commands, WU3), shared identically by both UIs (split GH-23). Driven via [FakeViewModelUi.drive]
 *  (GH-42 WU2) - both methods always call `showMessage`, which suspends on [UiRequestChannel.ask],
 *  so every test here needs something draining [ViewModel.uiRequests] concurrently, not just the
 *  ones that previously asserted on a message. */
class ViewModelSaveLoadTest {
    private val statusTexts = texts().status


    @Test
    fun `saveGame creates a new file when none exists`() = runTest {
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, analytics)
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
                    mapOf("result" to "success", "overwrote_existing" to false, "name_source" to "typed"),
                ),
            ),
            analytics.events,
        )
    }

    @Test
    fun `saveGame overwrites an existing file`() = runTest {
        val saves = FakeSaveRepository()
        saves.save("foo", intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), 3)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0)) }
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.saveGame("foo") }

        assertEquals(listOf(8, 7, 6, 5, 4, 3, 2, 1, 0), saves.load("foo")!!.state.toList())
        assertEquals(
            listOf(
                RecordingAnalyticsService.Event(
                    "save_command_used",
                    mapOf("result" to "success", "overwrote_existing" to true, "name_source" to "typed"),
                ),
            ),
            analytics.events,
        )
    }

    @Test
    fun `saveGame surfaces a message and tracks result=error instead of crashing when the repository throws`() = runTest {
        val saves = FakeSaveRepository().apply { saveException = IllegalArgumentException("Save name must not be blank") }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.saveGame("..") }

        assertEquals(listOf(statusTexts.saveNameRejected("..")), ui.shownMessages)
        assertEquals(
            listOf(RecordingAnalyticsService.Event("save_command_used", mapOf("result" to "error", "name_source" to "typed"))),
            analytics.events,
        )
    }

    @Test
    fun `a storage failure on save shows one friendly line, not the storage's reason`() = runTest {
        val saves = FakeSaveRepository().apply { saveException = IllegalStateException("QuotaExceededError: disk full") }
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, RecordingAnalyticsService())
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.saveGame("foo") }

        assertEquals(listOf(statusTexts.saveFailed("foo")), ui.shownMessages)
    }

    @Test
    fun `an IllegalArgumentException for a valid name is a storage failure, not a name problem`() = runTest {
        val saves = FakeSaveRepository().apply { saveException = IllegalArgumentException("Move count must not be negative") }
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, RecordingAnalyticsService())
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.saveGame("foo") }

        assertEquals(listOf(statusTexts.saveFailed("foo")), ui.shownMessages)
    }

    @Test
    fun `an unexpected failure on load shows one friendly line, not the storage's reason`() = runTest {
        val saves = FakeSaveRepository().apply { loadException = IllegalStateException("disk unplugged") }
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, RecordingAnalyticsService())
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("foo") }

        assertEquals(listOf(statusTexts.loadFailed("foo")), ui.shownMessages)
    }

    @Test
    fun `loadGame restores the board on a known save name`() = runTest {
        val saves = FakeSaveRepository()
        saves.save("foo", intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), 3)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi()
        var loaded = false

        ui.drive(viewModel) { loaded = viewModel.loadGame("foo") }

        assertTrue(loaded)
        assertEquals(listOf(8, 7, 6, 5, 4, 3, 2, 1, 0), board.boardArray.toList())
        assertEquals(listOf(statusTexts.loaded("foo")), ui.shownMessages)
        assertEquals(listOf<String?>("foo"), ui.loadedSaves)
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "success", "labels_differ_from_preference" to false))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame leaves the board unchanged on an unknown save name`() = runTest {
        val saves = FakeSaveRepository()
        saves.save("bar", intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), 3)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi()
        var loaded = true

        ui.drive(viewModel) { loaded = viewModel.loadGame("missing") }

        assertFalse(loaded)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains("missing") && it.contains("bar") })
        assertEquals(listOf<String?>(null), ui.loadedSaves)
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame with no saves at all still messages cleanly`() = runTest {
        val saves = FakeSaveRepository()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, RecordingAnalyticsService())
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("missing") }

        assertTrue(ui.shownMessages.any { it.contains(statusTexts.noSavesAvailable) })
    }

    @Test
    fun `loadGame resizes the board to match a save whose size differs, instead of rejecting it`() = runTest {
        val saves = FakeSaveRepository()
        saves.save("small", intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 8), 3)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState((0..15).toList().toIntArray()) }
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("small") }

        assertEquals(3, board.squareSide)
        assertEquals(listOf(3, 2, 1, 0, 7, 6, 5, 4, 8), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains(statusTexts.loaded("small")) })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "success", "labels_differ_from_preference" to false))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame still reports not_found, with a degraded message, when listing available saves fails`() = runTest {
        // Regression guard: the not_found branch's availableSavesMessage() call can throw
        // (e.g. an unreadable saves/ directory) independently of saves.load() itself - that
        // shouldn't turn an actual not_found result into a spurious second "error" event
        // (audit on PR #13).
        val saves = FakeSaveRepository().apply {
            listSavesException = Exception("permission denied")
        }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("missing") }

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), board.boardArray.toList())
        assertTrue(ui.shownMessages.any { it.contains(statusTexts.couldNotListSaves) })
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame surfaces a message and tracks result=error instead of crashing on a corrupted save file`() = runTest {
        val saves = FakeSaveRepository().apply {
            loadException = SaveFileFormatException("corrupt", "missing board values")
        }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi()
        var loaded = true

        ui.drive(viewModel) { loaded = viewModel.loadGame("corrupt") }

        assertFalse(loaded)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), board.boardArray.toList())
        assertEquals(listOf(statusTexts.saveDamaged("corrupt")), ui.shownMessages)
        assertEquals(listOf<String?>(null), ui.loadedSaves)
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "error"))),
            analytics.events,
        )
    }

    @Test
    fun `a save from a newer version says so and loads nothing, GH-313`() = runTest {
        val saves = FakeSaveRepository().apply {
            loadException = SaveFromNewerVersionException("corrupt")
        }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) }
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi()
        var loaded = true

        ui.drive(viewModel) { loaded = viewModel.loadGame("corrupt") }

        assertFalse(loaded)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), board.boardArray.toList())
        assertEquals(listOf(statusTexts.saveFromNewerVersion), ui.shownMessages)
        assertEquals(listOf<String?>(null), ui.loadedSaves)
        assertEquals(
            listOf(RecordingAnalyticsService.Event("load_command_used", mapOf("trigger" to "command", "result" to "error"))),
            analytics.events,
        )
    }
}

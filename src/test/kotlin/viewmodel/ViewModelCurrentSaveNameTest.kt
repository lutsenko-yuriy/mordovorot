package viewmodel

import board_model.BoardImpl
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import storage.SaveFileFormatException
import storage.SavedBoard
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-99: the current save name behind the Save dialog's prefill - set by a successful save,
 *  load or repair, cleared by a new game, and the source of `save_command_used.name_source`. */
class ViewModelCurrentSaveNameTest {

    private val oddState = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)

    private fun saves() = FakeSaveRepository(
        mutableMapOf(
            "foo" to SavedBoard(3, intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8), 0),
            "odd" to SavedBoard(3, oddState.copyOf(), 5),
        ),
    )

    private fun nameSources(analytics: RecordingAnalyticsService) =
        analytics.events.filter { it.name == "save_command_used" }.map { it.properties["name_source"] }

    @Test
    fun `a fresh session has no current save name`() {
        assertNull(testViewModel(BoardImpl(3), saves()).currentSaveName())
    }

    @Test
    fun `a successful load sets the current save name`(): Unit = runBlocking {
        val viewModel = testViewModel(BoardImpl(3), saves())

        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("foo") }

        assertEquals("foo", viewModel.currentSaveName())
    }

    @Test
    fun `a startup restore sets the current save name`(): Unit = runBlocking {
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(3, intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8), 0)))
        val viewModel = testViewModel(BoardImpl(3), saves)

        FakeViewModelUi(confirmRestoreResponses = mutableListOf(true)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals("foo", viewModel.currentSaveName())
    }

    @Test
    fun `a missing or corrupted load leaves the current save name`(): Unit = runBlocking {
        val saves = saves()
        val viewModel = testViewModel(BoardImpl(3), saves)
        val ui = FakeViewModelUi()

        ui.drive(viewModel) {
            viewModel.loadGame("foo")
            viewModel.loadGame("missing")
            saves.loadException = SaveFileFormatException("foo2", "corrupted")
            viewModel.loadGame("foo2")
        }

        assertEquals("foo", viewModel.currentSaveName())
    }

    @Test
    fun `a successful save sets the current save name`(): Unit = runBlocking {
        val viewModel = testViewModel(BoardImpl(3), saves())

        FakeViewModelUi().drive(viewModel) {
            viewModel.loadGame("foo")
            viewModel.saveGame("bar")
        }

        assertEquals("bar", viewModel.currentSaveName())
    }

    @Test
    fun `a failed save leaves the current save name`(): Unit = runBlocking {
        val saves = saves()
        val viewModel = testViewModel(BoardImpl(3), saves)

        FakeViewModelUi().drive(viewModel) {
            viewModel.loadGame("foo")
            saves.saveException = IOException("read-only")
            viewModel.saveGame("bar")
        }

        assertEquals("foo", viewModel.currentSaveName())
    }

    @Test
    fun `a new game clears the current save name`(): Unit = runBlocking {
        val viewModel = testViewModel(BoardImpl(3), saves())

        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("foo") }
        viewModel.newGame(4, trigger = "toolbar")

        assertNull(viewModel.currentSaveName())
    }

    @Test
    fun `an out-of-range new game leaves the current save name`(): Unit = runBlocking {
        val viewModel = testViewModel(BoardImpl(3), saves())

        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("foo") }
        runCatching { viewModel.newGame(9, trigger = "toolbar") }

        assertEquals("foo", viewModel.currentSaveName())
    }

    @Test
    fun `repair and load sets the current save name, even when the overwrite fails`(): Unit = runBlocking {
        val saves = saves().apply { saveException = IOException("read-only") }
        val viewModel = testViewModel(BoardImpl(4), saves)

        FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR))
            .drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals("odd", viewModel.currentSaveName())
    }

    @Test
    fun `new game from the unsolvable dialog clears the current save name`(): Unit = runBlocking {
        val viewModel = testViewModel(BoardImpl(4), saves())

        FakeViewModelUi(
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.NEW_GAME),
            chooseBoardSizeResponses = mutableListOf(4),
        ).drive(viewModel) {
            viewModel.loadGame("foo")
            viewModel.loadGame("odd")
        }

        assertNull(viewModel.currentSaveName())
    }

    @Test
    fun `name_source is typed with no current save name`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves(), analytics)

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("bar") }

        assertEquals(listOf<Any?>("typed"), nameSources(analytics))
    }

    @Test
    fun `name_source is prefill for the current name and edited for another one`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves(), analytics)

        FakeViewModelUi().drive(viewModel) {
            viewModel.loadGame("foo")
            viewModel.saveGame("foo")
            viewModel.saveGame("foox")
        }

        assertEquals(listOf<Any?>("prefill", "edited"), nameSources(analytics))
    }

    @Test
    fun `name_source is sent on a failed save too`(): Unit = runBlocking {
        val saves = saves()
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves, analytics)

        FakeViewModelUi().drive(viewModel) {
            viewModel.loadGame("foo")
            saves.saveException = IOException("read-only")
            viewModel.saveGame("foo")
        }

        assertEquals(
            RecordingAnalyticsService.Event("save_command_used", mapOf("result" to "error", "name_source" to "prefill")),
            analytics.events.last { it.name == "save_command_used" },
        )
    }
}

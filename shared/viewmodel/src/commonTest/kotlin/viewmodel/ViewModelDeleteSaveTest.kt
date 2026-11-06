package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import storage.SavedBoard
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import testing.testViewModel

/** GH-251: [GameViewModel.deleteSave] - the result, `save_deleted`, the current save name, and
 *  that it asks the View nothing. */
class ViewModelDeleteSaveTest {

    private val solved = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)
    private val unsolvable = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)

    private fun saves() = FakeSaveRepository(
        mutableMapOf(
            "foo" to SavedBoard(3, solved.copyOf(), 0),
            "bar" to SavedBoard(3, solved.copyOf(), 2),
            "odd" to SavedBoard(3, unsolvable.copyOf(), 5),
        ),
    )

    private fun deleted(analytics: RecordingAnalyticsService) = analytics.events.filter { it.name == "save_deleted" }

    @Test
    fun `deleting a save removes it, returns DELETED and tracks save_deleted`() {
        val saves = saves()
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves, analytics)

        val result = viewModel.deleteSave(SaveInfo("foo", 3), trigger = "command")

        assertEquals(DeleteResult.DELETED, result)
        assertEquals(listOf("bar", "odd"), saves.listSaves())
        assertEquals(
            listOf(
                Event(
                    "save_deleted",
                    mapOf("result" to "success", "trigger" to "command", "save_state" to "ok", "was_current_save" to false, "remaining_save_count" to 2),
                ),
            ),
            deleted(analytics),
        )
    }

    @Test
    fun `save_state follows the SaveInfo for an unsolvable and a corrupted save`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves(), analytics)

        viewModel.deleteSave(SaveInfo("odd", 3, solvable = false), trigger = "command")
        viewModel.deleteSave(SaveInfo("bar", null), trigger = "command")

        assertEquals(listOf<Any?>("unsolvable", "corrupted"), deleted(analytics).map { it.properties["save_state"] })
    }

    @Test
    fun `the trigger is passed through`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves(), analytics)

        viewModel.deleteSave(SaveInfo("foo", 3), trigger = "startup_prompt")

        assertEquals("startup_prompt", deleted(analytics).single().properties["trigger"])
    }

    @Test
    fun `deleting the last save reports no saves remaining`() {
        val analytics = RecordingAnalyticsService()
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(3, solved.copyOf(), 0)))
        val viewModel = testViewModel(BoardImpl(3), saves, analytics)

        viewModel.deleteSave(SaveInfo("foo", 3), trigger = "command")

        assertEquals(0, deleted(analytics).single().properties["remaining_save_count"])
    }

    @Test
    fun `a save that is already gone returns NOT_FOUND and tracks it without success-only properties`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves(), analytics)

        val result = viewModel.deleteSave(SaveInfo("missing", 3), trigger = "command")

        assertEquals(DeleteResult.NOT_FOUND, result)
        assertEquals(
            listOf(Event("save_deleted", mapOf("result" to "not_found", "trigger" to "command", "save_state" to "ok"))),
            deleted(analytics),
        )
    }

    @Test
    fun `a storage failure returns ERROR, keeps the save and tracks it`() {
        val saves = saves().apply { deleteException = IllegalStateException("blocked") }
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves, analytics)

        val result = viewModel.deleteSave(SaveInfo("foo", 3), trigger = "command")

        assertEquals(DeleteResult.ERROR, result)
        assertTrue(saves.exists("foo"))
        assertEquals(
            listOf(Event("save_deleted", mapOf("result" to "error", "trigger" to "command", "save_state" to "ok"))),
            deleted(analytics),
        )
    }

    @Test
    fun `a listing failure after the delete leaves remaining_save_count out`() {
        val saves = saves()
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves, analytics)
        saves.listSavesException = IllegalStateException("unreadable")

        val result = viewModel.deleteSave(SaveInfo("foo", 3), trigger = "command")

        assertEquals(DeleteResult.DELETED, result)
        assertFalse(deleted(analytics).single().properties.containsKey("remaining_save_count"))
    }

    @Test
    fun `deleting the current save clears the current save name`() = runTest {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves(), analytics)
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("foo") }

        viewModel.deleteSave(SaveInfo("foo", 3), trigger = "command")

        assertNull(viewModel.currentSaveName())
        assertEquals(true, deleted(analytics).single().properties["was_current_save"])
    }

    @Test
    fun `deleting another save leaves the current save name`() = runTest {
        val viewModel = testViewModel(BoardImpl(3), saves())
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("foo") }

        viewModel.deleteSave(SaveInfo("bar", 3), trigger = "command")

        assertEquals("foo", viewModel.currentSaveName())
    }

    @Test
    fun `deleting the current save that is already gone clears the current save name too`() = runTest {
        val saves = saves()
        val viewModel = testViewModel(BoardImpl(3), saves)
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("foo") }
        saves.deleteCalls.clear()
        saves.delete("foo")

        val result = viewModel.deleteSave(SaveInfo("foo", 3), trigger = "command")

        assertEquals(DeleteResult.NOT_FOUND, result)
        assertNull(viewModel.currentSaveName())
    }

    @Test
    fun `a failed delete of the current save leaves the current save name`() = runTest {
        val saves = saves()
        val viewModel = testViewModel(BoardImpl(3), saves)
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("foo") }
        saves.deleteException = IllegalStateException("blocked")

        viewModel.deleteSave(SaveInfo("foo", 3), trigger = "command")

        assertEquals("foo", viewModel.currentSaveName())
    }

    @Test
    fun `deleting leaves the board, the move count and unsaved progress alone`() = runTest {
        val board = BoardImpl(3)
        val viewModel = testViewModel(board, saves())
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("bar") }
        val before = viewModel.boardState().toList()
        val unsaved = viewModel.hasUnsavedProgress()

        viewModel.deleteSave(SaveInfo("bar", 3), trigger = "command")

        assertEquals(before, viewModel.boardState().toList())
        assertEquals(2, viewModel.moveCount())
        assertEquals(unsaved, viewModel.hasUnsavedProgress())
        assertFalse(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `deleting asks the View nothing`() {
        val viewModel = testViewModel(BoardImpl(3), saves())

        viewModel.deleteSave(SaveInfo("foo", 3), trigger = "command")
        viewModel.deleteSave(SaveInfo("missing", 3), trigger = "command")

        assertTrue(viewModel.uiRequests.tryReceive().isFailure)
    }
}

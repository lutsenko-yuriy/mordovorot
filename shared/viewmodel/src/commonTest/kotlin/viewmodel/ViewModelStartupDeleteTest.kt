package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-251: `startup_restore_decision` is `all_deleted` when every save on offer was deleted from
 *  the restore prompt; the size prompt follows as for any decline. */
class ViewModelStartupDeleteTest {

    private val state = IntArray(16) { it }

    private fun saves(vararg names: String) =
        FakeSaveRepository(names.associateWith { SavedBoard(4, state.copyOf()) }.toMutableMap())

    private fun decisions(analytics: RecordingAnalyticsService) =
        analytics.events.filter { it.name == "startup_restore_decision" }.map { it.properties }

    @Test
    fun `the one save deleted from the prompt - all_deleted, then the size prompt`() = runTest {
        val saves = saves("foo")
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(FakeBoardModel(), saves, analytics)
        val ui = FakeViewModelUi(
            confirmRestoreResponses = mutableListOf(false),
            chooseBoardSizeResponses = mutableListOf(null),
            beforeRestoreAnswer = { viewModel.deleteSave(SaveInfo("foo", 4), trigger = "startup_prompt") },
        )

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf<Map<String, Any?>>(mapOf("decision" to "all_deleted", "save_file_count" to 1)), decisions(analytics))
        assertEquals(listOf(4), ui.chooseBoardSizeCalls)
    }

    @Test
    fun `two saves both deleted - all_deleted counts the saves on offer when the prompt opened`() = runTest {
        val saves = saves("foo", "bar")
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(FakeBoardModel(), saves, analytics)
        val ui = FakeViewModelUi(
            chooseSaveToRestoreResponses = mutableListOf(null),
            chooseBoardSizeResponses = mutableListOf(null),
            beforeRestoreAnswer = {
                viewModel.deleteSave(SaveInfo("foo", 4), trigger = "startup_prompt")
                viewModel.deleteSave(SaveInfo("bar", 4), trigger = "startup_prompt")
            },
        )

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf<Map<String, Any?>>(mapOf("decision" to "all_deleted", "save_file_count" to 2)), decisions(analytics))
        assertEquals(listOf(4), ui.chooseBoardSizeCalls)
    }

    @Test
    fun `one of two saves deleted and the other declined - still new_game`() = runTest {
        val saves = saves("foo", "bar")
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(FakeBoardModel(), saves, analytics)
        val ui = FakeViewModelUi(
            chooseSaveToRestoreResponses = mutableListOf(null),
            chooseBoardSizeResponses = mutableListOf(null),
            beforeRestoreAnswer = { viewModel.deleteSave(SaveInfo("foo", 4), trigger = "startup_prompt") },
        )

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf<Map<String, Any?>>(mapOf("decision" to "new_game", "save_file_count" to 2)), decisions(analytics))
    }

    @Test
    fun `a decline with nothing deleted is still new_game`() = runTest {
        val saves = saves("foo")
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(FakeBoardModel(), saves, analytics)
        val ui = FakeViewModelUi(confirmRestoreResponses = mutableListOf(false), chooseBoardSizeResponses = mutableListOf(null))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf<Map<String, Any?>>(mapOf("decision" to "new_game", "save_file_count" to 1)), decisions(analytics))
    }

    @Test
    fun `a listing failure after a decline is new_game, not all_deleted`() = runTest {
        val saves = saves("foo")
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(FakeBoardModel(), saves, analytics)
        val ui = FakeViewModelUi(
            confirmRestoreResponses = mutableListOf(false),
            chooseBoardSizeResponses = mutableListOf(null),
            beforeRestoreAnswer = { saves.listSavesException = IllegalStateException("unreadable") },
        )

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf<Map<String, Any?>>(mapOf("decision" to "new_game", "save_file_count" to 1)), decisions(analytics))
    }

    @Test
    fun `a save restored after another was deleted is restored, not all_deleted`() = runTest {
        val saves = saves("foo", "bar")
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(FakeBoardModel(), saves, analytics)
        val ui = FakeViewModelUi(
            chooseSaveToRestoreResponses = mutableListOf("bar"),
            beforeRestoreAnswer = { viewModel.deleteSave(SaveInfo("foo", 4), trigger = "startup_prompt") },
        )

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf<Map<String, Any?>>(mapOf("decision" to "restored", "save_file_count" to 2)), decisions(analytics))
        assertEquals(emptyList(), ui.chooseBoardSizeCalls)
    }
}

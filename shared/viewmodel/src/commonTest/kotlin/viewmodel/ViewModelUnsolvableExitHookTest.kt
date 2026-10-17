package viewmodel

import kotlinx.coroutines.test.runTest
import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mordie.Mordie
import storage.SavedBoard
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService

/** GH-41: Exit for an unsolvable save only reports the choice - [UnsolvableSaveExit] runs it,
 *  once the shared flow has returned. */
class ViewModelUnsolvableExitHookTest {

    /** 3x3, tiles 0/2 swapped - odd, unsolvable. */
    private val oddState = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)

    private val analytics = RecordingAnalyticsService()
    private val factory = FakeMordieSearchFactory()

    /** Each hook call as (atStartup, events tracked by then). */
    private val hookCalls = mutableListOf<Pair<Boolean, List<String>>>()
    private val recordingHook = UnsolvableSaveExit { _, atStartup -> hookCalls += atStartup to analytics.events.map { it.name } }

    private fun viewModel(unsolvableExit: UnsolvableSaveExit = recordingHook) = buildGameViewModel(
        board = BoardImpl(4),
        saves = FakeSaveRepository(mutableMapOf("odd" to SavedBoard(3, oddState.copyOf(), 5))),
        analytics = analytics,
        startupRestoreDone = false,
        sizeChosenAtLaunch = false,
        mordie = Mordie(factory),
        currentSaveName = CurrentSaveName(),
        gameShiftSources = GameShiftSources(),
        unsavedProgress = UnsavedProgress(),
        ui = UiRequestChannel(),
        unsolvableExit = unsolvableExit,
    )

    @Test
    fun `exit mid-game calls the hook once the choice is tracked`() = runTest {
        val viewModel = viewModel()
        val ui = FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.EXIT))

        ui.drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals(listOf(false to listOf("load_command_used", "unsolvable_save_resolved")), hookCalls)
        assertEquals(0, ui.confirmSaveBeforeExitCallCount)
    }

    @Test
    fun `exit at startup tracks quit, skips the size prompt and Mordie, then calls the hook`() = runTest {
        val viewModel = viewModel()
        val ui = FakeViewModelUi(
            confirmRestoreResponses = mutableListOf(true),
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.EXIT),
        )

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(1, hookCalls.size)
        assertEquals(true, hookCalls.single().first)
        assertEquals("startup_restore_decision", hookCalls.single().second.last())
        assertEquals(
            mapOf("decision" to "quit", "save_file_count" to 1),
            analytics.events.single { it.name == "startup_restore_decision" }.properties,
        )
        assertTrue(ui.chooseBoardSizeCalls.isEmpty())
        assertTrue(factory.started.isEmpty())
    }

    @Test
    fun `with no hook, exit mid-game just ends the load`() = runTest {
        val viewModel = viewModel(UnsolvableSaveExit.NONE)
        val ui = FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.EXIT))

        ui.drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals(0, ui.confirmSaveBeforeExitCallCount)
        assertEquals(listOf("load_command_used", "unsolvable_save_resolved"), analytics.events.map { it.name })
    }
}

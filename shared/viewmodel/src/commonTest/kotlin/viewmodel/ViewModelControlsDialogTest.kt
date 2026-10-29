package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import storage.SavedMordie
import storage.SavedMove
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-170: `screen_controls_dialog` as the Controls dialog opens - where from, and on which screen. */
class ViewModelControlsDialogTest {

    private val analytics = RecordingAnalyticsService()
    private val saves = FakeSaveRepository()

    /** Row 0 right once solves it. */
    private val board = BoardImpl(3).apply { restoreState(intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)) }

    private val viewModel = testViewModel(board, saves, analytics, startupRestoreDone = true, mordie = Mordie(FakeMordieSearchFactory()))

    private fun opened() = analytics.events.filter { it.name == "screen_controls_dialog" }.map { it.properties }

    @Test
    fun `opening tracks where from and the screen`() = runTest {
        viewModel.controlsDialogOpened("shortcut")
        viewModel.shiftRight(0)
        viewModel.controlsDialogOpened("toolbar")
        val screenSave = SavedMordie(
            intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8), 2, found = true,
            solution = listOf(SavedMove(isRow = true, index = 0, amount = 1), SavedMove(isRow = false, index = 0, amount = 1)),
            solutionScreen = true,
        )
        saves.save("s", screenSave.original, 3, 0, screenSave)
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("s") }
        viewModel.controlsDialogOpened("shortcut")

        assertEquals(
            listOf(
                mapOf("opened_from" to "shortcut", "screen" to "play"),
                mapOf("opened_from" to "toolbar", "screen" to "solved"),
                mapOf("opened_from" to "shortcut", "screen" to "solution"),
            ),
            opened(),
        )
    }
}

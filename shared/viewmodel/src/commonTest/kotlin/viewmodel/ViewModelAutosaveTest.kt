package viewmodel

import board_model.BoardImpl
import kotlinx.coroutines.test.runTest
import storage.SavedBoard
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** GH-338: `autosave` writes the game to its slot, silently, only when there are unsaved shifts. */
class ViewModelAutosaveTest {

    private val saves = FakeSaveRepository(mutableMapOf("solved" to SavedBoard(3, intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), 0)))
    private val analytics = RecordingAnalyticsService()
    private val viewModel = testViewModel(BoardImpl(3), saves, analytics)

    private fun events() = analytics.events.filter { it.name == "autosave_written" }

    @Test
    fun `a game with a shift is written to its slot`() {
        viewModel.shiftLeft(0)

        assertEquals(AutosaveResult.SAVED, viewModel.autosave("page_left"))

        assertEquals(listOf("Autosave_numbers_3x3"), saves.saveCalls.map { it.first })
        assertContentEquals(viewModel.boardState(), saves.saveCalls.single().second)
        assertEquals(1, saves.savedMoveCounts.single())
        assertEquals("numbers", saves.savedLabels.single())
    }

    @Test
    fun `it clears the unsaved progress, so a second call writes nothing`() {
        viewModel.shiftLeft(0)
        viewModel.autosave("page_left")

        assertFalse(viewModel.hasUnsavedProgress())
        assertEquals(AutosaveResult.SKIPPED, viewModel.autosave("page_left"))
        assertEquals(1, saves.saveCalls.size)
    }

    @Test
    fun `a game with no shift is skipped`() {
        assertEquals(AutosaveResult.SKIPPED, viewModel.autosave("page_left"))
        assertTrue(saves.saveCalls.isEmpty())
        assertTrue(events().isEmpty())
    }

    @Test
    fun `a solved board is skipped and stays unsaved`() {
        val nearSolved = testViewModel(BoardImpl(3).apply { restoreState(intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)) }, saves, analytics)
        nearSolved.shiftRight(0)
        assertTrue(nearSolved.isSolved())

        assertEquals(AutosaveResult.SKIPPED, nearSolved.autosave("page_left"))

        assertTrue(saves.saveCalls.isEmpty())
        assertTrue(nearSolved.hasUnsavedProgress())
    }

    @Test
    fun `a game on the tutorial is skipped`() {
        viewModel.newGameFrom(intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8), tutorial = true)
        viewModel.shiftLeft(0)

        assertEquals(AutosaveResult.SKIPPED, viewModel.autosave("page_left"))
        assertTrue(saves.saveCalls.isEmpty())
    }

    @Test
    fun `the solution screen is skipped`() {
        val factory = FakeMordieSearchFactory()
        val mordie = Mordie(factory)
        val original = intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8)
        val solving = testViewModel(BoardImpl(3).apply { restoreState(original) }, saves, analytics, mordie)
        mordie.startFresh(original)
        factory.latest.progress = SearchProgress(
            2, found = true, elapsedMs = 0,
            solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 0, amount = 1)),
        )
        repeat(5) { solving.shiftUp(2) }
        solving.shiftLeft(1)
        solving.giveUp()

        assertEquals(AutosaveResult.SKIPPED, solving.autosave("page_left"))
        assertTrue(saves.saveCalls.isEmpty())
    }

    @Test
    fun `a failed write is reported and the progress stays unsaved`() {
        viewModel.shiftLeft(0)
        saves.saveException = IllegalStateException("disk full")

        assertEquals(AutosaveResult.FAILED, viewModel.autosave("page_left"))

        assertTrue(viewModel.hasUnsavedProgress())
        assertEquals("error", events().single().properties["result"])
        saves.saveException = null
        assertEquals(AutosaveResult.SAVED, viewModel.autosave("page_left"))
    }

    @Test
    fun `it keeps the current save name and shows no message`() = runTest {
        val ui = FakeViewModelUi()
        ui.drive(viewModel) { viewModel.saveGame("mine") }
        val before = ui.shownMessages.size
        viewModel.shiftLeft(0)

        viewModel.autosave("page_left")

        assertEquals("mine", viewModel.currentSaveName())
        assertEquals(before, ui.shownMessages.size)
    }

    @Test
    fun `the event carries its properties`() {
        viewModel.shiftLeft(0)
        viewModel.autosave("update")

        assertEquals(
            mapOf("result" to "success", "board_size" to 3, "tile_labels" to "numbers", "moves" to 1, "trigger" to "update"),
            events().single().properties,
        )
    }

    @Test
    fun `loading an autosave is marked in load_command_used, loading a named save is not`() = runTest {
        saves.save("Autosave_numbers_3x3", intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8), 3, 1)
        saves.save("mine", intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8), 3, 1)

        FakeViewModelUi().drive(viewModel) {
            viewModel.loadGame("Autosave_numbers_3x3")
            viewModel.loadGame("mine")
        }

        assertEquals(
            listOf<Any?>(true, false),
            analytics.events.filter { it.name == "load_command_used" }.map { it.properties["autosave"] },
        )
    }

    @Test
    fun `restoring an autosave at startup is marked in startup_restore_decision`() = runTest {
        saves.save("Autosave_numbers_3x3", intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8), 3, 1)
        val ui = FakeViewModelUi(chooseSaveToRestoreResponses = mutableListOf("Autosave_numbers_3x3"))

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(
            listOf<Any?>(true),
            analytics.events.filter { it.name == "startup_restore_decision" }.map { it.properties["restored_autosave"] },
        )
    }
}

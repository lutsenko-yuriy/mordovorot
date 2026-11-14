package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.FakePreferenceRepository
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-267: a share link's labels through the viewmodel - shown for the game only, tracking nothing. */
class ViewModelSharedLabelsTest {

    private val oneShiftAway = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)

    private val store = FakePreferenceRepository()
    private val analytics = RecordingAnalyticsService()
    private val saves = FakeSaveRepository()
    private val board = BoardImpl(3).apply { restoreState(oneShiftAway) }

    private fun viewModel(preference: String? = null): GameViewModelImpl {
        preference?.let { store.values["tile_labels"] = it }
        return testViewModel(
            board, saves, analytics, mordie = Mordie(FakeMordieSearchFactory()),
            tileLabels = SessionTileLabels(TileLabelsPreference(store)),
        )
    }

    @Test
    fun `useSharedLabels shows them without touching the preference, and tracks nothing`() {
        val viewModel = viewModel("letters")

        viewModel.useSharedLabels(TileLabels.PAIRS)

        assertEquals(TileLabels.PAIRS, viewModel.tileLabels())
        assertEquals("letters", store.values["tile_labels"])
        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `the next new game goes back to the preference`() {
        val viewModel = viewModel("letters")
        viewModel.useSharedLabels(TileLabels.PAIRS)

        viewModel.newGame(3, trigger = "toolbar")

        assertEquals(TileLabels.LETTERS, viewModel.tileLabels())
    }

    @Test
    fun `a save made afterwards carries them, and loading it shows them as a save's`() = runTest {
        val viewModel = viewModel()
        viewModel.useSharedLabels(TileLabels.PAIRS)

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("a") }
        viewModel.newGame(3, trigger = "toolbar")
        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("a") }

        assertEquals(listOf<String?>("pairs"), saves.savedLabels)
        assertEquals(TileLabels.PAIRS, viewModel.tileLabels())
    }

    @Test
    fun `a later Labels OK reports the link as where the old labels came from`() {
        val viewModel = viewModel()
        viewModel.useSharedLabels(TileLabels.PAIRS)

        viewModel.changeTileLabels(TileLabels.NUMBERS)

        val changed = analytics.events.single { it.name == "tile_labels_changed" }.properties
        assertEquals("shared_link", changed["from_source"])
        assertEquals("pairs", changed["from"])
        assertEquals("numbers", changed["to"])
    }
}

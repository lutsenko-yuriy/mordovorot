package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.FakePreferenceRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-243: `newGameFrom`, a fresh game on a given board (a share link's). */
class ViewModelNewGameFromTest {

    private val shared = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)
    private val searches = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()
    private val preference = FakePreferenceRepository().apply { values["tile_labels"] = "numbers" }
    private val labels = SessionTileLabels(TileLabelsPreference(preference))
    private val currentSaveName = CurrentSaveName()
    private val viewModel = testViewModel(
        BoardImpl(3), analytics = analytics, mordie = Mordie(searches), currentSaveName = currentSaveName, tileLabels = labels,
    )

    @Test
    fun `the board is that arrangement at zero moves, and Mordie races it`() {
        viewModel.shiftLeft(0)

        viewModel.newGameFrom(shared)

        assertContentEquals(shared, viewModel.boardState())
        assertEquals(0, viewModel.moveCount())
        assertContentEquals(shared, searches.latest.original)
    }

    @Test
    fun `it is a fresh game - no save name, no unsaved progress, labels back to the preference`() {
        currentSaveName.name = "old"
        labels.choose(TileLabels.LETTERS)
        labels.applySave(TileLabels.PAIRS, 3)
        viewModel.shiftLeft(0)

        viewModel.newGameFrom(shared)

        assertNull(viewModel.currentSaveName())
        assertFalse(viewModel.hasUnsavedProgress())
        assertEquals(TileLabels.LETTERS, viewModel.tileLabels())
    }

    @Test
    fun `it tracks nothing itself`() {
        viewModel.newGameFrom(shared)

        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `an unsolvable board throws and leaves the game alone`() {
        val before = viewModel.boardState()

        assertFailsWith<IllegalArgumentException> { viewModel.newGameFrom(intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)) }

        assertContentEquals(before, viewModel.boardState())
    }
}

package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.FakePreferenceRepository
import testing.FakeSaveRepository
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-332: the `screen_alphabet_hint_dialog` screen view. */
class ViewModelAlphabetHintTest {

    private val store = FakePreferenceRepository()
    private val analytics = RecordingAnalyticsService()
    private val saves = FakeSaveRepository()

    private fun viewModel(side: Int, preference: String?, solved: Boolean = false): GameViewModelImpl {
        preference?.let { store.values["tile_labels"] = it }
        val board = BoardImpl(side).apply {
            restoreState(if (solved) IntArray(side * side) { it } else IntArray(side * side) { (it + 1) % (side * side) })
        }
        val labels = SessionTileLabels(TileLabelsPreference(store)).apply { resetToPreference(side) }
        return testViewModel(board, saves, analytics, mordie = Mordie(FakeMordieSearchFactory()), tileLabels = labels)
    }

    private fun event() = analytics.events.single { it.name == "screen_alphabet_hint_dialog" }.properties

    @Test
    fun `opening from the button reports the scheme, the board and the screen`() {
        viewModel(4, "letters").alphabetHintOpened("button")

        assertEquals(
            mapOf("opened_from" to "button", "screen" to "play", "labels" to "letters", "board_size" to 4),
            event(),
        )
    }

    @Test
    fun `opening from the key reports shortcut`() {
        viewModel(5, "greek_letter_pairs").alphabetHintOpened("shortcut")

        assertEquals(
            mapOf("opened_from" to "shortcut", "screen" to "play", "labels" to "greek_letter_pairs", "board_size" to 5),
            event(),
        )
    }

    @Test
    fun `a won board reports solved`() {
        viewModel(3, "cyrillic_letters", solved = true).alphabetHintOpened("button")

        assertEquals("solved", event()["screen"])
    }

    @Test
    fun `Letters on a 6x6 board report the numbers shown`() {
        viewModel(6, "letters").alphabetHintOpened("button")

        assertEquals("numbers", event()["labels"])
    }
}

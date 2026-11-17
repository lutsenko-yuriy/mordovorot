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
import texts.Language
import texts.texts

/** GH-313: the Cyrillic schemes through the viewmodel: the 6x6 stand-in, the fitting question on a load, and the save's own labels winning. */
class ViewModelCyrillicLabelsTest {

    private val store = FakePreferenceRepository()
    private val analytics = RecordingAnalyticsService()
    private val saves = FakeSaveRepository()
    private val board = BoardImpl(3).apply { restoreState(intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)) }

    private fun viewModel(preference: String? = null): GameViewModelImpl {
        preference?.let { store.values["tile_labels"] = it }
        return testViewModel(
            board, saves, analytics, mordie = Mordie(FakeMordieSearchFactory()),
            tileLabels = SessionTileLabels(TileLabelsPreference(store)),
        )
    }

    private fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }

    @Test
    fun `the Cyrillic preference loads, and 6x6 shows numbers until a 5x5 game`() {
        val viewModel = viewModel("cyrillic_letters")
        assertEquals(TileLabels.CYRILLIC_LETTERS, viewModel.tileLabels())

        viewModel.newGame(6, trigger = "toolbar")
        assertEquals(TileLabels.NUMBERS, viewModel.tileLabels())
        assertEquals("cyrillic_letters", store.values["tile_labels"])

        viewModel.newGame(5, trigger = "toolbar")
        assertEquals(TileLabels.CYRILLIC_LETTERS, viewModel.tileLabels())
    }

    @Test
    fun `Cyrillic letter pairs stay on 6x6, where Cyrillic letters give way`() {
        val viewModel = viewModel("cyrillic_letter_pairs")

        viewModel.newGame(6, trigger = "toolbar")

        assertEquals(TileLabels.CYRILLIC_LETTER_PAIRS, viewModel.tileLabels())
    }

    @Test
    fun `a 6x6 save with Cyrillic letters asks for labels with the Cyrillic message`() = runTest {
        saves.save("six", IntArray(36) { 35 - it }, 6, labels = "cyrillic_letters")
        val viewModel = viewModel()
        val ui = FakeViewModelUi(chooseFittingLabelsResponses = mutableListOf(TileLabels.PAIRS))

        ui.drive(viewModel) { viewModel.loadGame("six") }

        assertEquals(listOf(texts().status.cyrillicLettersDontFit(6) to emptyList<String>()), ui.chooseFittingLabelsCalls)
        assertEquals(TileLabels.PAIRS, viewModel.tileLabels())
        assertEquals(listOf("letters_save"), events("screen_labels_dialog").map { it["opened_from"] })
    }

    @Test
    fun `a save's Cyrillic labels beat the preference on a board they fit`() = runTest {
        saves.save("four", IntArray(16) { 15 - it }, 4, labels = "cyrillic_letter_pairs")
        val viewModel = viewModel("pairs")

        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("four") }

        assertEquals(TileLabels.CYRILLIC_LETTER_PAIRS, viewModel.tileLabels())
        assertEquals("pairs", store.values["tile_labels"])
    }

    @Test
    fun `a saved game keeps its Cyrillic labels`() = runTest {
        val viewModel = viewModel("cyrillic_letter_pairs")

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("mine") }

        assertEquals("cyrillic_letter_pairs", saves.savedLabels.last())
    }

    @Test
    fun `every language says the 28 letters with a word each, starting with the letter`() {
        for (language in Language.entries) {
            val words = language.texts.reader.cyrillicPhonetic
            assertEquals(28, words.size, language.name)
            assertEquals(TileLabels.CYRILLIC_ALPHABET.toList(), words.map { it.first() }, language.name)
        }
    }

    @Test
    fun `every scheme survives a save through the codec`() {
        for (scheme in TileLabels.entries) {
            val text = storage.SaveFileCodec.encode(3, IntArray(9) { it }, 0, null, scheme.wireName)
            assertEquals(scheme.wireName, storage.SaveFileCodec.decode("t", text).labels, scheme.wireName)
        }
    }
}

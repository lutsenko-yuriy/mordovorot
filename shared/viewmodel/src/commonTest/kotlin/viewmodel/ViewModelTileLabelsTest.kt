package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import storage.SavedMordie
import storage.SavedMove
import testing.FakeMordieSearchFactory
import testing.FakePreferenceRepository
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-139: the tile labels through the viewmodel - the dialog's analytics, the change, and the
 *  save > preference > default priority across New, Save, Load, startup restore and Repair. */
class ViewModelTileLabelsTest {

    private val solved = IntArray(9) { it }

    /** Row 0 right once solves it. */
    private val oneShiftAway = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)

    /** Two tiles swapped: no sequence of shifts solves it. */
    private val unsolvable = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)

    private val screenSave = SavedMordie(
        intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8), 2, found = true,
        solution = listOf(SavedMove(isRow = true, index = 0, amount = 1), SavedMove(isRow = false, index = 0, amount = 1)),
        solutionScreen = true,
    )

    private val store = FakePreferenceRepository()
    private val analytics = RecordingAnalyticsService()
    private val saves = FakeSaveRepository()
    private val board = BoardImpl(3).apply { restoreState(oneShiftAway) }

    private fun viewModel(preference: String? = null, startupRestoreDone: Boolean = true): GameViewModelImpl {
        preference?.let { store.values["tile_labels"] = it }
        return testViewModel(
            board, saves, analytics, startupRestoreDone, mordie = Mordie(FakeMordieSearchFactory()),
            tileLabels = SessionTileLabels(TileLabelsPreference(store)),
        )
    }

    private fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
    private fun event(name: String) = events(name).single()

    private suspend fun GameViewModelImpl.load(name: String, ui: FakeViewModelUi = FakeViewModelUi()) =
        ui.drive(this) { loadGame(name) }

    @Test
    fun `the labels start as numbers, or the preference when one is stored`() {
        assertEquals(TileLabels.NUMBERS, viewModel().tileLabels())
        assertEquals(TileLabels.LETTERS, viewModel("letters").tileLabels())
    }

    @Test
    fun `changing the labels relabels, remembers the choice and tracks every property`() {
        val viewModel = viewModel()

        viewModel.changeTileLabels(TileLabels.PAIRS)

        assertEquals(TileLabels.PAIRS, viewModel.tileLabels())
        assertEquals("pairs", store.values["tile_labels"])
        assertEquals(
            mapOf("from" to "numbers", "to" to "pairs", "from_source" to "default", "screen" to "play", "board_size" to 3),
            event("tile_labels_changed"),
        )
    }

    @Test
    fun `OK on the scheme already shown tracks nothing`() {
        val viewModel = viewModel("pairs")

        viewModel.changeTileLabels(TileLabels.PAIRS)

        assertEquals(0, events("tile_labels_changed").size)
    }

    @Test
    fun `the change's screen is solved after a win and solution on the solution screen`() = runTest {
        board.restoreState(solved)
        val viewModel = viewModel("pairs")
        viewModel.changeTileLabels(TileLabels.LETTERS)

        saves.save("s", screenSave.original, 3, 0, screenSave, labels = "letter_pairs")
        viewModel.load("s")
        viewModel.changeTileLabels(TileLabels.NUMBERS)

        assertEquals(
            listOf("preference" to "solved", "save" to "solution"),
            events("tile_labels_changed").map { it["from_source"] to it["screen"] },
        )
    }

    @Test
    fun `opening the dialog tracks where from, the screen and the labels shown`() {
        val viewModel = viewModel("letter_pairs")

        viewModel.tileLabelsDialogOpened("shortcut")

        assertEquals(
            mapOf("opened_from" to "shortcut", "screen" to "play", "current_labels" to "letter_pairs"),
            event("screen_labels_dialog"),
        )
    }

    @Test
    fun `a save stores the labels shown, numbers included`() = runTest {
        val viewModel = viewModel()
        FakeViewModelUi().drive(viewModel) {
            viewModel.saveGame("a")
            viewModel.changeTileLabels(TileLabels.LETTERS)
            viewModel.saveGame("b")
        }

        assertEquals(listOf<String?>("numbers", "letters"), saves.savedLabels)
    }

    @Test
    fun `a loaded save's labels win until the next new game, which goes back to the preference`() = runTest {
        saves.save("l", oneShiftAway, 3, labels = "letters")
        val viewModel = viewModel("pairs")

        viewModel.load("l")
        assertEquals(TileLabels.LETTERS, viewModel.tileLabels())

        viewModel.newGame(3, trigger = "toolbar")
        assertEquals(TileLabels.PAIRS, viewModel.tileLabels())
    }

    @Test
    fun `a save without labels, or with an unknown name, loads with the preference`() = runTest {
        saves.save("old", oneShiftAway, 3)
        saves.save("future", oneShiftAway, 3, labels = "emoji")
        val viewModel = viewModel("pairs")
        viewModel.changeTileLabels(TileLabels.LETTERS)

        viewModel.load("old")
        assertEquals(TileLabels.LETTERS, viewModel.tileLabels())
        viewModel.load("future")
        assertEquals(TileLabels.LETTERS, viewModel.tileLabels())
    }

    @Test
    fun `load_command_used says whether the save's labels differ from the preference`() = runTest {
        saves.save("same", oneShiftAway, 3, labels = "pairs")
        saves.save("other", oneShiftAway, 3, labels = "letters")
        saves.save("none", oneShiftAway, 3)
        val viewModel = viewModel("pairs")

        for (name in listOf("same", "other", "none")) viewModel.load(name)
        viewModel.load("missing")

        assertEquals(
            listOf(false, true, false, null),
            events("load_command_used").map { it["labels_differ_from_preference"] },
        )
        assertEquals(
            mapOf("trigger" to "command", "result" to "success", "labels_differ_from_preference" to true),
            events("load_command_used")[1],
        )
    }

    @Test
    fun `with no preference a save's labels are compared with numbers`() = runTest {
        saves.save("n", oneShiftAway, 3, labels = "numbers")
        saves.save("p", oneShiftAway, 3, labels = "pairs")
        val viewModel = viewModel()

        viewModel.load("n")
        viewModel.load("p")

        assertEquals(listOf(false, true), events("load_command_used").map { it["labels_differ_from_preference"] })
    }

    @Test
    fun `the startup restore keeps the save's labels`() = runTest {
        saves.save("l", oneShiftAway, 3, labels = "letter_pairs")
        val viewModel = viewModel("pairs", startupRestoreDone = false)

        FakeViewModelUi(confirmRestoreResponses = mutableListOf(true)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(TileLabels.LETTER_PAIRS, viewModel.tileLabels())
        assertEquals(true, event("load_command_used")["labels_differ_from_preference"])
    }

    @Test
    fun `Repair and load keeps the save's labels, in the board and the rewritten save`() = runTest {
        saves.save("odd", unsolvable, 3, labels = "letters")
        val viewModel = viewModel("pairs")

        viewModel.load("odd", FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR)))

        assertEquals(TileLabels.LETTERS, viewModel.tileLabels())
        assertEquals("letters", saves.load("odd")?.labels)
    }

    @Test
    fun `board_solved carries the labels shown`() {
        val viewModel = viewModel()
        viewModel.changeTileLabels(TileLabels.PAIRS_FROM_0)

        viewModel.shiftRight(0)

        assertEquals("pairs_from_0", event("board_solved")["tile_labels"])
    }

    @Test
    fun `a viewModel rebuilt around the session's labels keeps them`() {
        val labels = SessionTileLabels(TileLabelsPreference(store))
        testViewModel(board, saves, analytics, tileLabels = labels).changeTileLabels(TileLabels.LETTERS)

        assertEquals(TileLabels.LETTERS, testViewModel(board, saves, analytics, tileLabels = labels).tileLabels())
    }
}

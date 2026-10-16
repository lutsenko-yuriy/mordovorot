package viewmodel

import board_model.BoardImpl
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import mordie.SearchProgress
import storage.SavedBoard
import storage.SavedMordie
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** GH-83: Mordie's wiring through the viewModel - binding on new game/load/repair/startup,
 *  the save snapshot, its two analytics changes and the line/ending queries. */
class ViewModelMordieTest {

    /** 3x3, one `shiftLeft(0)` from solved. */
    private val nearlySolved = intArrayOf(2, 0, 1, 3, 4, 5, 6, 7, 8)
    private val solved = IntArray(9) { it }

    /** Tiles 0/1 swapped - odd, so unsolvable on a 3x3. */
    private val unsolvable = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)

    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()

    private fun viewModel(
        saves: FakeSaveRepository = FakeSaveRepository(),
        board: BoardImpl = BoardImpl(3).apply { restoreState(nearlySolved) },
        sizeChosenAtLaunch: Boolean = false,
        startupRestoreDone: Boolean = false,
        mordie: Mordie = Mordie(factory),
    ) = ViewModelImpl(board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie)

    private fun savesWith(state: IntArray, moveCount: Int = 0, mordie: SavedMordie? = null) =
        FakeSaveRepository(mutableMapOf("m" to SavedBoard(3, state.copyOf(), moveCount, mordie)))

    private suspend fun ViewModelImpl.load(ui: FakeViewModelUi = FakeViewModelUi()) = ui.drive(this) { loadGame("m") }

    private fun event(name: String) = analytics.events.single { it.name == name }.properties

    private fun found(moves: Int, elapsedMs: Long = 0) = SearchProgress(moves, found = true, elapsedMs)

    // --- binding ---

    @Test
    fun `newGame starts a fresh search of the new arrangement`() {
        val viewModel = viewModel()

        viewModel.newGame(4, trigger = "toolbar")

        assertEquals(1, factory.started.size)
        assertEquals(-1, factory.latest.ruledOut)
        assertContentEquals(viewModel.boardState(), factory.latest.original)
        assertEquals("Alright! Let's sort this one out!", viewModel.mordieLine())
    }

    @Test
    fun `newGame cancels the previous search`() {
        val viewModel = viewModel()
        viewModel.newGame(3, trigger = "toolbar")

        viewModel.newGame(3, trigger = "toolbar")

        assertTrue(factory.started.first().cancelled)
        assertEquals(2, factory.started.size)
    }

    @Test
    fun `an unplayed save with no Mordie data starts fresh from the loaded board`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(nearlySolved))

        viewModel.load()

        assertContentEquals(nearlySolved, factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        assertEquals("Alright! Let's sort this one out!", viewModel.mordieLine())
    }

    @Test
    fun `Mordie sits out a save with no Mordie data that already has moves`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(nearlySolved, moveCount = 4))
        viewModel.newGame(3, trigger = "toolbar")

        viewModel.load()
        viewModel.shiftLeft(0)

        assertTrue(factory.latest.cancelled)
        assertEquals(1, factory.started.size)
        assertEquals(5, viewModel.moveCount())
        assertNull(viewModel.mordieLine())
        assertNull(viewModel.mordieEnding())
        assertEquals(mapOf("move_count" to 5, "board_size" to 3), event("board_solved"))
    }

    @Test
    fun `a searching save resumes above its N from the saved original`(): Unit = runBlocking {
        val original = intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0)
        val viewModel = viewModel(savesWith(nearlySolved, mordie = SavedMordie(original, 8, found = false)))

        viewModel.load()

        assertContentEquals(original, factory.latest.original)
        assertEquals(8, factory.latest.ruledOut)
        assertEquals("Okay, so we stopped when I was at 8 moves! What was I doing?..", viewModel.mordieLine())
    }

    @Test
    fun `a searching save with an unsolvable original starts fresh from the loaded board`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(nearlySolved, mordie = SavedMordie(unsolvable, 8, found = false)))

        viewModel.load()

        assertContentEquals(nearlySolved, factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
    }

    @Test
    fun `a found save shows Found at once, starts no search and never fires mordie_solution_found`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), mordie = SavedMordie(nearlySolved, 11, found = true)))

        viewModel.load()
        repeat(5) { viewModel.shiftUp(0) }

        assertEquals(0, factory.started.size)
        assertEquals("Found it! I had to make 11 moves to find the solution! Can you beat it?", viewModel.mordieLine())
        assertTrue(analytics.events.none { it.name == "mordie_solution_found" })
    }

    @Test
    fun `a solved save with Mordie data shows its ending with no line or search`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(solved, moveCount = 3, mordie = SavedMordie(nearlySolved, 3, found = true)))

        viewModel.load()

        assertEquals(0, factory.started.size)
        assertNull(viewModel.mordieLine())
        assertEquals("Cool, you matched me! 3 moves each!", viewModel.mordieEnding())
    }

    @Test
    fun `a solved save without Mordie data has no ending, so the title falls back`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(solved, moveCount = 3))

        viewModel.load()

        assertEquals(0, factory.started.size)
        assertNull(viewModel.mordieLine())
        assertNull(viewModel.mordieEnding())
    }

    @Test
    fun `repair of an unplayed save starts fresh from the repaired board and saves no Mordie data`(): Unit = runBlocking {
        val saves = savesWith(unsolvable, mordie = SavedMordie(unsolvable, 2, found = false))
        val viewModel = viewModel(saves)

        viewModel.load(FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR)))

        assertContentEquals(viewModel.boardState(), factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        assertEquals(listOf<SavedMordie?>(null), saves.savedMordies)
    }

    @Test
    fun `Mordie sits out a repaired save that already has moves`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(unsolvable, moveCount = 5))

        viewModel.load(FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR)))

        assertEquals(0, factory.started.size)
        assertEquals(5, viewModel.moveCount())
        assertNull(viewModel.mordieLine())
    }

    // --- save ---

    @Test
    fun `saveGame stores the original arrangement and the live progress`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val viewModel = viewModel(saves)
        viewModel.newGame(3, trigger = "toolbar")
        val original = viewModel.boardState()
        viewModel.shiftUp(0)
        factory.latest.progress = SearchProgress(6, found = false, elapsedMs = 0)

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("m") }

        assertEquals(listOf<SavedMordie?>(SavedMordie(original, 6, found = false)), saves.savedMordies)
    }

    @Test
    fun `saveGame stores no Mordie data when Mordie is unbound`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val viewModel = viewModel(saves)

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("m") }

        assertEquals(listOf<SavedMordie?>(null), saves.savedMordies)
    }

    // --- startup ---

    @Test
    fun `restoreOnStartup with --size starts Mordie on the board as constructed`(): Unit = runBlocking {
        val viewModel = viewModel(sizeChosenAtLaunch = true)

        FakeViewModelUi().drive(viewModel) { viewModel.restoreOnStartup() }

        assertContentEquals(nearlySolved, factory.latest.original)
    }

    @Test
    fun `restoreOnStartup with a cancelled size prompt starts Mordie on the current board`(): Unit = runBlocking {
        val viewModel = viewModel()

        FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(null)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(1, factory.started.size)
        assertContentEquals(nearlySolved, factory.latest.original)
    }

    @Test
    fun `restoreOnStartup with a chosen size starts exactly one search`(): Unit = runBlocking {
        val viewModel = viewModel()

        FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(4)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(1, factory.started.size)
        assertEquals(16, factory.latest.original.size)
    }

    @Test
    fun `restoreOnStartup that restores a save binds Mordie from it`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(nearlySolved, mordie = SavedMordie(nearlySolved, 8, found = false)))

        FakeViewModelUi(confirmRestoreResponses = mutableListOf(true)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(1, factory.started.size)
        assertEquals(8, factory.latest.ruledOut)
    }

    @Test
    fun `a rebuild after a mode switch keeps the already bound Mordie`(): Unit = runBlocking {
        val mordie = Mordie(factory)
        val first = viewModel(sizeChosenAtLaunch = true, mordie = mordie)
        FakeViewModelUi().drive(first) { first.restoreOnStartup() }
        first.shiftUp(0)

        val rebuilt = viewModel(startupRestoreDone = true, mordie = mordie)
        FakeViewModelUi().drive(rebuilt) { rebuilt.restoreOnStartup() }

        assertEquals(1, factory.started.size)
        assertEquals(1, mordie.shifts)
    }

    // --- analytics ---

    @Test
    fun `mordie_solution_found fires at the check that reveals the solution`() {
        val viewModel = viewModel()
        viewModel.newGame(4, trigger = "toolbar")
        repeat(3) { viewModel.shiftUp(0) }
        factory.latest.progress = found(11, elapsedMs = 1234)

        repeat(7) { viewModel.shiftUp(1) }

        assertEquals(
            mapOf(
                "board_size" to 4,
                "solution_length" to 11,
                "player_shift_count" to 5,
                "search_duration_ms" to 1234L,
                "resumed_from_save" to false,
            ),
            event("mordie_solution_found"),
        )
    }

    @Test
    fun `mordie_solution_found reports a resumed search`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), mordie = SavedMordie(nearlySolved, 8, found = false)))
        viewModel.load()
        factory.latest.progress = found(9)

        repeat(5) { viewModel.shiftUp(0) }

        assertEquals(true, event("mordie_solution_found")["resumed_from_save"])
    }

    @Test
    fun `board_solved reports player_won when Mordie hadn't found it`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(nearlySolved))
        viewModel.load()
        factory.latest.progress = SearchProgress(0, found = false, elapsedMs = 0)

        viewModel.shiftLeft(0)

        assertEquals("player_won", event("board_solved")["mordie_result"])
        assertEquals(0, event("board_solved")["mordie_moves"])
        assertEquals("You beat me! I was still at 0 moves.", viewModel.mordieEnding())
        assertTrue(factory.latest.cancelled)
    }

    @Test
    fun `board_solved reports matched`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(nearlySolved))
        viewModel.load()
        factory.latest.progress = found(1)

        viewModel.shiftLeft(0)

        assertEquals("matched", event("board_solved")["mordie_result"])
        assertEquals(1, event("board_solved")["mordie_moves"])
        assertEquals("Cool, you matched me! 1 move each!", viewModel.mordieEnding())
    }

    @Test
    fun `board_solved reports mordie_won`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(nearlySolved))
        viewModel.load()
        factory.latest.progress = found(1)

        viewModel.shiftUp(2)
        viewModel.shiftDown(2)
        viewModel.shiftLeft(0)

        assertEquals("mordie_won", event("board_solved")["mordie_result"])
        assertEquals("Sorry, but I win: 1 move vs your 2.", viewModel.mordieEnding())
    }

    @Test
    fun `board_solved reports machine for fewer moves than a saved N`(): Unit = runBlocking {
        val viewModel = viewModel(savesWith(nearlySolved, mordie = SavedMordie(nearlySolved, 5, found = true)))
        viewModel.load()

        viewModel.shiftLeft(0)

        assertEquals("machine", event("board_solved")["mordie_result"])
        assertEquals(5, event("board_solved")["mordie_moves"])
        assertEquals("I made 5 moves and you made 1 move! Are you a machine?", viewModel.mordieEnding())
    }
}

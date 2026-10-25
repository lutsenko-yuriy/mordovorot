package viewmodel

import kotlinx.coroutines.test.runTest
import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.IdaStarSolver
import mordie.Mordie
import mordie.MordieSearch
import mordie.MordieSearchFactory
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SaveFileCodec
import storage.SavedBoard
import storage.SavedMordie
import storage.SavedMove
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

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
    ) = testViewModel(board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie)

    private fun savesWith(state: IntArray, moveCount: Int = 0, mordie: SavedMordie? = null) =
        FakeSaveRepository(mutableMapOf("m" to SavedBoard(3, state.copyOf(), moveCount, mordie)))

    private suspend fun GameViewModelImpl.load(ui: FakeViewModelUi = FakeViewModelUi()) = ui.drive(this) { loadGame("m") }

    private fun event(name: String) = analytics.events.single { it.name == name }.properties

    private fun found(moves: Int, elapsedMs: Long = 0) = SearchProgress(moves, found = true, elapsedMs)

    /** Solves [nearlySolved]: row 0 rotated right twice. */
    private val path = listOf(SavedMove(isRow = true, index = 0, amount = 2))

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
    fun `an unplayed save with no Mordie data starts fresh from the loaded board`() = runTest {
        val viewModel = viewModel(savesWith(nearlySolved))

        viewModel.load()

        assertContentEquals(nearlySolved, factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        assertEquals("Alright! Let's sort this one out!", viewModel.mordieLine())
    }

    @Test
    fun `Mordie sits out a save with no Mordie data that already has moves`() = runTest {
        val viewModel = viewModel(savesWith(nearlySolved, moveCount = 4))
        viewModel.newGame(3, trigger = "toolbar")

        viewModel.load()
        viewModel.shiftLeft(0)

        assertTrue(factory.latest.cancelled)
        assertEquals(1, factory.started.size)
        assertEquals(5, viewModel.moveCount())
        assertNull(viewModel.mordieLine())
        assertNull(viewModel.mordieEnding())
        assertEquals(mapOf("move_count" to 5, "board_size" to 3, "tile_labels" to "numbers"), event("board_solved"))
    }

    @Test
    fun `a searching save resumes above its N from the saved original`() = runTest {
        val original = intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0)
        val viewModel = viewModel(savesWith(nearlySolved, mordie = SavedMordie(original, 8, found = false)))

        viewModel.load()

        assertContentEquals(original, factory.latest.original)
        assertEquals(8, factory.latest.ruledOut)
        assertEquals("Okay, so we stopped when I was at 8 moves! What was I doing?..", viewModel.mordieLine())
    }

    @Test
    fun `a searching save with an unsolvable original starts fresh from the loaded board`() = runTest {
        val viewModel = viewModel(savesWith(nearlySolved, mordie = SavedMordie(unsolvable, 8, found = false)))

        viewModel.load()

        assertContentEquals(nearlySolved, factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
    }

    @Test
    fun `a found save shows Found at once, starts no search and never fires mordie_solution_found`() = runTest {
        val viewModel = viewModel(savesWith(intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), mordie = SavedMordie(nearlySolved, 11, found = true)))

        viewModel.load()
        repeat(5) { viewModel.shiftUp(0) }

        assertEquals(0, factory.started.size)
        assertEquals("Found it! I had to make 11 moves to find the solution! Can you beat it?", viewModel.mordieLine())
        assertTrue(analytics.events.none { it.name == "mordie_solution_found" })
    }

    @Test
    fun `a solved save with Mordie data shows its ending with no line or search`() = runTest {
        val viewModel = viewModel(savesWith(solved, moveCount = 3, mordie = SavedMordie(nearlySolved, 3, found = true)))

        viewModel.load()

        assertEquals(0, factory.started.size)
        assertNull(viewModel.mordieLine())
        assertEquals("Cool, you matched me! 3 moves each!", viewModel.mordieEnding())
    }

    @Test
    fun `a solved save without Mordie data has no ending, so the title falls back`() = runTest {
        val viewModel = viewModel(savesWith(solved, moveCount = 3))

        viewModel.load()

        assertEquals(0, factory.started.size)
        assertNull(viewModel.mordieLine())
        assertNull(viewModel.mordieEnding())
    }

    @Test
    fun `repair of an unplayed save starts fresh from the repaired board and saves no Mordie data`() = runTest {
        val saves = savesWith(unsolvable, mordie = SavedMordie(unsolvable, 2, found = false))
        val viewModel = viewModel(saves)

        viewModel.load(FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR)))

        assertContentEquals(viewModel.boardState(), factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        assertEquals(listOf<SavedMordie?>(null), saves.savedMordies)
    }

    @Test
    fun `Mordie sits out a repaired save that already has moves`() = runTest {
        val viewModel = viewModel(savesWith(unsolvable, moveCount = 5))

        viewModel.load(FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR)))

        assertEquals(0, factory.started.size)
        assertEquals(5, viewModel.moveCount())
        assertNull(viewModel.mordieLine())
    }

    // --- 6x6, GH-160 ---

    private val sitsOut = "6x6 is too much for me, you're on your own!"

    /** 6x6, one `shiftRight(0)` from solved. */
    private val nearlySolved6 = BoardImpl(6).apply { restoreState(IntArray(36) { it }); shiftLeft(0) }.boardArray

    private fun saves6(state: IntArray, moveCount: Int = 0, mordie: SavedMordie? = null) =
        FakeSaveRepository(mutableMapOf("m" to SavedBoard(6, state.copyOf(), moveCount, mordie)))

    @Test
    fun `Mordie sits a new 6x6 game out - the line for shifts 0-4, no search, Give Up hidden`() {
        val viewModel = viewModel()

        viewModel.newGame(6, trigger = "toolbar")

        assertEquals(0, factory.started.size)
        assertEquals(sitsOut, viewModel.mordieLine())
        assertEquals(GiveUpButton.HIDDEN, viewModel.giveUpButton())
        repeat(4) { viewModel.shiftUp(0) }
        assertEquals(sitsOut, viewModel.mordieLine())
        viewModel.shiftUp(0)
        assertNull(viewModel.mordieLine())
        assertEquals(GiveUpButton.HIDDEN, viewModel.giveUpButton())

        viewModel.newGame(4, trigger = "toolbar")
        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())
    }

    @Test
    fun `a 6x6 save with moves and Mordie data loads sitting out, and saves no Mordie data`() = runTest {
        val saves = saves6(nearlySolved6, moveCount = 3, mordie = SavedMordie(nearlySolved6, 8, found = false))
        val viewModel = viewModel(saves)

        viewModel.load()
        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("n") }

        assertEquals(0, factory.started.size)
        assertEquals(sitsOut, viewModel.mordieLine())
        assertEquals(listOf<SavedMordie?>(null), saves.savedMordies)
    }

    @Test
    fun `a solved 6x6 save loads with no line`() = runTest {
        val viewModel = viewModel(saves6(IntArray(36) { it }, moveCount = 3))

        viewModel.load()

        assertNull(viewModel.mordieLine())
        assertNull(viewModel.mordieEnding())
    }

    @Test
    fun `restoreOnStartup with --size=6 sits Mordie out`() = runTest {
        val viewModel = viewModel(board = BoardImpl(6), sizeChosenAtLaunch = true)

        FakeViewModelUi().drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(0, factory.started.size)
        assertEquals(sitsOut, viewModel.mordieLine())
    }

    @Test
    fun `restoreOnStartup with 6 chosen at the prompt sits Mordie out`() = runTest {
        val viewModel = viewModel()

        FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(6)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(0, factory.started.size)
        assertEquals(sitsOut, viewModel.mordieLine())
    }

    @Test
    fun `a rebuild after a mode switch keeps a 6x6 game's shift count`() = runTest {
        val mordie = Mordie(factory)
        val board = BoardImpl(6)
        val first = viewModel(board = board, sizeChosenAtLaunch = true, mordie = mordie)
        FakeViewModelUi().drive(first) { first.restoreOnStartup() }
        repeat(5) { first.shiftUp(0) }

        val rebuilt = viewModel(board = board, startupRestoreDone = true, mordie = mordie)
        FakeViewModelUi().drive(rebuilt) { rebuilt.restoreOnStartup() }

        assertEquals(5, mordie.shifts)
        assertNull(rebuilt.mordieLine())
    }

    @Test
    fun `solving a 6x6 - no Mordie props on board_solved, no ending or line, Give Up hidden`() = runTest {
        val viewModel = viewModel(saves6(nearlySolved6))
        viewModel.load()

        viewModel.shiftRight(0)

        assertTrue(viewModel.isSolved())
        assertEquals(mapOf("move_count" to 1, "board_size" to 6, "tile_labels" to "numbers"), event("board_solved"))
        assertNull(viewModel.mordieEnding())
        assertNull(viewModel.mordieLine())
        assertEquals(GiveUpButton.HIDDEN, viewModel.giveUpButton())
    }

    // --- save ---

    @Test
    fun `saveGame stores the original arrangement and the live progress`() = runTest {
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
    fun `saveGame stores Mordie's solution once found, GH-126`() = runTest {
        val saves = FakeSaveRepository()
        val viewModel = viewModel(saves)
        viewModel.newGame(3, trigger = "toolbar")
        val original = viewModel.boardState()
        factory.latest.progress = SearchProgress(1, found = true, elapsedMs = 0, solution = listOf(SolutionMove(isRow = true, index = 0, amount = 2)))

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("m") }

        assertEquals(listOf<SavedMordie?>(SavedMordie(original, 1, found = true, solution = path)), saves.savedMordies)
    }

    @Test
    fun `a real solver's solution passes the save codec's check, GH-126`() = runTest {
        val solving = MordieSearchFactory { original, _ ->
            val solver = IdaStarSolver(original).also { while (!it.step(10_000)) { } }
            val progress = SearchProgress(solver.solutionLength!!, found = true, elapsedMs = 0, solver.solution())
            object : MordieSearch {
                override fun progress() = progress
                override fun cancel() = Unit
            }
        }
        val saves = FakeSaveRepository()
        val viewModel = viewModel(saves, mordie = Mordie(solving))
        viewModel.newGame(3, trigger = "toolbar")

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("m") }

        val mordie = saves.savedMordies.single()!!
        val text = SaveFileCodec.encode(3, viewModel.boardState(), 0, mordie)
        assertEquals(mordie, SaveFileCodec.decode("m", text).mordie)
        assertTrue(mordie.solution!!.isNotEmpty())
    }

    @Test
    fun `a loaded solution is saved again, found or solved, GH-126`() = runTest {
        for ((state, moveCount) in listOf(intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0) to 0, solved to 1)) {
            val mordie = SavedMordie(nearlySolved, 1, found = true, solution = path)
            val saves = savesWith(state, moveCount, mordie)
            val viewModel = viewModel(saves)
            viewModel.load()

            FakeViewModelUi().drive(viewModel) { viewModel.saveGame("again") }

            assertEquals(listOf<SavedMordie?>(mordie), saves.savedMordies)
        }
    }

    @Test
    fun `saveGame stores no Mordie data when Mordie is unbound`() = runTest {
        val saves = FakeSaveRepository()
        val viewModel = viewModel(saves)

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("m") }

        assertEquals(listOf<SavedMordie?>(null), saves.savedMordies)
    }

    // --- startup ---

    @Test
    fun `restoreOnStartup with --size starts Mordie on the board as constructed`() = runTest {
        val viewModel = viewModel(sizeChosenAtLaunch = true)

        FakeViewModelUi().drive(viewModel) { viewModel.restoreOnStartup() }

        assertContentEquals(nearlySolved, factory.latest.original)
    }

    @Test
    fun `restoreOnStartup with a cancelled size prompt starts Mordie on the current board`() = runTest {
        val viewModel = viewModel()

        FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(null)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(1, factory.started.size)
        assertContentEquals(nearlySolved, factory.latest.original)
    }

    @Test
    fun `restoreOnStartup with a chosen size starts exactly one search`() = runTest {
        val viewModel = viewModel()

        FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(4)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(1, factory.started.size)
        assertEquals(16, factory.latest.original.size)
    }

    @Test
    fun `restoreOnStartup that restores a save binds Mordie from it`() = runTest {
        val viewModel = viewModel(savesWith(nearlySolved, mordie = SavedMordie(nearlySolved, 8, found = false)))

        FakeViewModelUi(confirmRestoreResponses = mutableListOf(true)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(1, factory.started.size)
        assertEquals(8, factory.latest.ruledOut)
    }

    @Test
    fun `a rebuild after a mode switch keeps the already bound Mordie`() = runTest {
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
    fun `mordie_solution_found reports a resumed search`() = runTest {
        val viewModel = viewModel(savesWith(intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), mordie = SavedMordie(nearlySolved, 8, found = false)))
        viewModel.load()
        factory.latest.progress = found(9)

        repeat(5) { viewModel.shiftUp(0) }

        assertEquals(true, event("mordie_solution_found")["resumed_from_save"])
    }

    @Test
    fun `board_solved reports player_won when Mordie hadn't found it`() = runTest {
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
    fun `board_solved reports matched`() = runTest {
        val viewModel = viewModel(savesWith(nearlySolved))
        viewModel.load()
        factory.latest.progress = found(1)

        viewModel.shiftLeft(0)

        assertEquals("matched", event("board_solved")["mordie_result"])
        assertEquals(1, event("board_solved")["mordie_moves"])
        assertEquals("Cool, you matched me! 1 move each!", viewModel.mordieEnding())
    }

    @Test
    fun `board_solved reports mordie_won`() = runTest {
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
    fun `board_solved reports machine for fewer moves than a saved N`() = runTest {
        val viewModel = viewModel(savesWith(nearlySolved, mordie = SavedMordie(nearlySolved, 5, found = true)))
        viewModel.load()

        viewModel.shiftLeft(0)

        assertEquals("machine", event("board_solved")["mordie_result"])
        assertEquals(5, event("board_solved")["mordie_moves"])
        assertEquals("I made 5 moves and you made 1 move! Are you a machine?", viewModel.mordieEnding())
    }
}

package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import mordie.MordieLine
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedBoard
import storage.SavedMordie
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-225: Mordie plays every board size, 6x6 to 9x9 included, as on 3x3 to 5x5. */
class ViewModelBigBoardMordieTest {

    private val factory = FakeMordieSearchFactory()
    private val analytics = RecordingAnalyticsService()

    private fun viewModel(
        saves: FakeSaveRepository = FakeSaveRepository(),
        board: BoardImpl = BoardImpl(4),
    ) = testViewModel(board, saves, analytics, Mordie(factory))

    private fun solved(side: Int) = IntArray(side * side) { it }

    /** [side]x[side], one left shift of row 0 from solved. */
    private fun nearlySolved(side: Int) = BoardImpl(side).apply { restoreState(solved(side)); shiftLeft(0) }.boardArray

    private fun savesWith(side: Int, state: IntArray, moveCount: Int = 0, mordie: SavedMordie? = null) =
        FakeSaveRepository(mutableMapOf("m" to SavedBoard(side, state.copyOf(), moveCount, mordie)))

    private suspend fun GameViewModelImpl.load() = FakeViewModelUi().drive(this) { loadGame("m") }

    private fun event(name: String) = analytics.events.single { it.name == name }.properties

    private val answer = List(5) { SolutionMove(isRow = true, index = 0, amount = 1) }

    @Test
    fun `a new 6x6 game starts a search, with Give Up dimmed until the first check`() {
        val viewModel = viewModel()

        viewModel.newGame(6, trigger = "toolbar")

        assertEquals(36, factory.latest.original.size)
        assertEquals(MordieText.line(MordieLine.Opening), viewModel.mordieLine())
        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())
    }

    @Test
    fun `Give Up lights at the first check once Mordie found an answer on 6x6`() {
        val viewModel = viewModel()
        viewModel.newGame(6, trigger = "toolbar")
        factory.latest.progress = SearchProgress(5, found = true, elapsedMs = 0, solution = answer)

        repeat(4) { viewModel.shiftUp(0) }
        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())
        viewModel.shiftUp(0)

        assertEquals(GiveUpButton.GIVE_UP, viewModel.giveUpButton())
        assertEquals(6, event("mordie_solution_found")["board_size"])
    }

    @Test
    fun `a 6x6 save with Mordie data resumes its search`() = runTest {
        val saves = savesWith(6, nearlySolved(6), moveCount = 3, mordie = SavedMordie(solved(6), 8, found = false))
        val viewModel = viewModel(saves)

        viewModel.load()

        assertEquals(listOf(8), factory.started.map { it.ruledOut })
        assertEquals(36, factory.latest.original.size)
    }

    @Test
    fun `a 6x6 save with no Mordie data and no moves starts Mordie fresh`() = runTest {
        val viewModel = viewModel(savesWith(6, nearlySolved(6)))

        viewModel.load()

        assertEquals(1, factory.started.size)
        assertEquals(GiveUpButton.DIMMED, viewModel.giveUpButton())
    }

    @Test
    fun `a 6x6 save with no Mordie data and moves made sits Mordie out`() = runTest {
        val saves = savesWith(6, nearlySolved(6), moveCount = 3)
        val viewModel = viewModel(saves)

        viewModel.load()
        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("n") }

        assertEquals(0, factory.started.size)
        assertNull(viewModel.mordieLine())
        assertEquals(listOf<SavedMordie?>(null), saves.savedMordies)
    }

    @Test
    fun `a solved 6x6 save with no Mordie data loads with no line`() = runTest {
        val viewModel = viewModel(savesWith(6, solved(6), moveCount = 3))

        viewModel.load()

        assertNull(viewModel.mordieLine())
        assertNull(viewModel.mordieEnding())
        assertEquals(GiveUpButton.HIDDEN, viewModel.giveUpButton())
    }

    @Test
    fun `restoreOnStartup with 6 chosen at the prompt starts a search`() = runTest {
        val viewModel = viewModel()

        FakeViewModelUi(chooseBoardSizeResponses = mutableListOf(6)).drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(36, factory.latest.original.size)
    }

    @Test
    fun `solving a 6x6 reports Mordie's props and ending, and offers Mordie's solution`() = runTest {
        val viewModel = viewModel(savesWith(6, nearlySolved(6)))
        viewModel.load()
        factory.latest.progress = SearchProgress(5, found = true, elapsedMs = 0, solution = answer, provenShortest = false)

        viewModel.shiftRight(0)

        assertTrue(viewModel.isSolved())
        val solved = event("board_solved")
        assertEquals(6, solved["board_size"])
        assertEquals(5, solved["mordie_moves"])
        assertEquals("beat_mordie", solved["mordie_result"])
        assertTrue(viewModel.mordieEnding() != null)
        assertEquals(GiveUpButton.MORDIES_SOLUTION, viewModel.giveUpButton())
    }
}

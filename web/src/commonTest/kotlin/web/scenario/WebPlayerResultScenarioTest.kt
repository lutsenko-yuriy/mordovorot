package web.scenario

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import storage.SavedMordie
import storage.SavedMove
import web.Arrow

/** GH-273 scenarios: Mordie's line on the solution screen opens with how the player's game ended.
 *  Same setup as [WebGiveUpScenarioTest]: a 4x4 board two moves from solved. */
class WebPlayerResultScenarioTest {
    private val mordieTexts = texts().mordie


    private val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
    private val savedPath = path.map { SavedMove(it.isRow, it.index, it.amount) }
    private val gaveUpLine = "${mordieTexts.solutionStopped(3)} ${mordieTexts.solutionHere(2)}"
    private val solvedLine = "${mordieTexts.solutionSolved(2)} ${mordieTexts.solutionHere(2)}"

    private fun WebHarness.searchFinds() {
        mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
    }

    /** Mordie finds its solution, then five shifts of column 3 (one move) reveal it. */
    private fun WebHarness.reveal() {
        searchFinds()
        repeat(5) { game.onArrow(Arrow.UP, 3) }
    }

    /** The reveal's move plus [more] on other lines. */
    private fun WebHarness.movesMade(more: Int) {
        reveal()
        repeat(more) { game.onArrow(Arrow.LEFT, it + 1) }
    }

    private fun WebHarness.giveUp() {
        game.solution.onGiveUp()
        game.solution.onGiveUpAnswered(true)
    }

    /** Right on row 0, then down twice on column 1: Mordie's own solution, in 2 moves. */
    private fun WebHarness.solve() {
        game.onArrow(Arrow.RIGHT, 0)
        repeat(2) { game.onArrow(Arrow.DOWN, 1) }
    }

    private fun WebHarness.givenUpAfterThree(): WebHarness {
        movesMade(more = 2)
        giveUp()
        return this
    }

    @Test
    fun `giving up after 3 moves puts the result in front of Mordie's line`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.movesMade(more = 2)
        assertEquals(3, page.state.moves)

        page.giveUp()

        assertEquals(gaveUpLine, page.state.mordie)
        assertFalse(page.state.movesShown)
        assertEquals("gave_up", page.props("screen_solution").single()["player_result"])
    }

    @Test
    fun `Mordie's solution after a win puts the solved result in front of Mordie's line`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.searchFinds()
        page.solve()

        page.game.solution.onMordiesSolution()

        assertEquals(solvedLine, page.state.mordie)
        assertEquals("solved", page.props("screen_solution").single()["player_result"])
    }

    @Test
    fun `a count of 1 reads 1 move`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.movesMade(more = 0)

        page.giveUp()

        assertEquals("${mordieTexts.solutionStopped(1)} ${mordieTexts.solutionHere(2)}", page.state.mordie)
    }

    @Test
    fun `giving up before any move says the player did not even start`() = webTest {
        val page = WebHarness(backgroundScope).seed("m", original, mordie = SavedMordie(original, 2, found = true, solution = savedPath)).start()
        page.game.onLoadChosen("m")
        assertEquals(0, page.state.moves)

        page.giveUp()

        assertEquals("${mordieTexts.solutionNotStarted} ${mordieTexts.solutionHere(2)}", page.state.mordie)
        assertEquals("gave_up", page.props("screen_solution").single()["player_result"])
    }

    @Test
    fun `stepping the replay never counts moves, feeds Mordie or changes the result`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original).givenUpAfterThree()

        repeat(2) { page.game.solution.onReplayNext() }
        page.game.solution.onReplayRestart()

        assertFalse(page.state.movesShown)
        assertEquals(gaveUpLine, page.state.mordie)
        assertTrue(page.props("board_solved").isEmpty())
        assertFalse(page.game.hasUnsavedProgress())
    }

    @Test
    fun `a solution-screen save keeps the result, and Load shows the same line`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original).givenUpAfterThree()
        page.game.onSave()
        page.game.onSaveAnswered("s")
        page.game.onNew()
        page.game.onSizeChosen(4)

        page.game.onLoad()
        page.game.onLoadChosen("s")

        assertEquals(gaveUpLine, page.state.mordie)
        val last = page.props("screen_solution").last()
        assertEquals("load", last["opened_from"])
        assertEquals("gave_up", last["player_result"])
    }

    @Test
    fun `a solved result survives a reload and the startup restore shows the same line`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original)
        page.searchFinds()
        page.solve()
        page.game.solution.onMordiesSolution()
        page.game.onSave()
        page.game.onSaveAnswered("s")

        val reloaded = WebHarness(backgroundScope, store = page.store).start()
        reloaded.game.onLoadChosen("s")

        assertEquals(solvedLine, reloaded.state.mordie)
        val event = reloaded.props("screen_solution").single()
        assertEquals("startup_prompt", event["opened_from"])
        assertEquals("solved", event["player_result"])
    }

    @Test
    fun `a solution-screen save from before this ticket keeps today's line`() = webTest {
        val page = WebHarness(backgroundScope).seed("old", original, mordie = SavedMordie(original, 2, found = true, solution = savedPath, solutionScreen = true)).start()

        page.game.onLoadChosen("old")

        assertEquals(mordieTexts.solutionHere(2), page.state.mordie)
        assertEquals("unknown", page.props("screen_solution").single()["player_result"])
    }

    @Test
    fun `leaving the solution screen forgets the result`() = webTest {
        val page = WebHarness(backgroundScope).startOn(original).givenUpAfterThree()

        page.game.onNew()
        page.game.onSizeChosen(4)

        assertNull(page.state.solution)
        assertTrue(page.state.movesShown)
        assertFalse(page.state.mordie.orEmpty().contains("gave up"))
    }

    @Test
    fun `giving up before the first shift is put softly`() = webTest {
        // TODO: 1. Give Up before the first shift
        // TODO: 2. The solution screen opens
        // TODO: 3. Its line starts with `mordie.solutionNotStarted`
    }

    @Test
    fun `giving up after shifts is put softly`() = webTest {
        // TODO: 1. Make two shifts
        // TODO: 2. Give Up
        // TODO: 3. Its line starts with `mordie.solutionStopped(2)`
    }

    @Test
    fun `losing to Mordie keeps units on both counts`() = webTest {
        // TODO: 1. Solve in more moves than Mordie's solution
        // TODO: 2. The ending shows `mordie.endingMordieWon(mine, yours)`
        // TODO: 3. Both counts carry their unit
    }

    @Test
    fun `beating an unproven answer has its own ending`() = webTest {
        // TODO: 1. Seed Mordie's answer as not proven shortest
        // TODO: 2. Solve in fewer moves
        // TODO: 3. The ending shows `mordie.endingBeatUnproven(mine, yours)`
        // TODO: 4. It differs from `mordie.endingPlayerWon(mine)`
    }
}
